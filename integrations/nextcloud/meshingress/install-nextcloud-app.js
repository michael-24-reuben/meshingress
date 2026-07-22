#!/usr/bin/env node
'use strict';

const { existsSync } = require('node:fs');
const { spawnSync } = require('node:child_process');
const { dirname } = require('node:path');

const APP_ID = 'meshingress';
const APP_DIRECTORY = dirname(__filename);
const CUSTOM_APPS_DIRECTORY = '/var/www/html/custom_apps';
const SOURCE_IMPORT_WORKER_MARKER = 'meshingress-nextcloud-source-imports';
const SOURCE_IMPORT_WORKER_PID_FILE = 'meshingress-source-import-worker.pid';
const SOURCE_IMPORT_WORKER_INTERVAL_SECONDS = 1;

function usage() {
  console.log(`Usage:
  MESHINGRESS_NEXTCLOUD_DEPLOY_HOST=<host> \\
  MESHINGRESS_NEXTCLOUD_DEPLOY_USER=<linux-user> \\
  node integrations/nextcloud/meshingress/install-nextcloud-app.js

Optional environment variable:
  MESHINGRESS_NEXTCLOUD_DEPLOY_CONTAINER  Nextcloud container name (default: nextcloud-app.v1)

Option:
  --replace-existing  Replace the installed app only after the staged PHP files
                      have passed syntax validation.

The script copies this app to a fresh custom_apps/meshingress directory, checks PHP
syntax, enables the app with occ, installs the persistent source-import worker,
and prints its registered OCS routes. It uses the current SSH agent and never
accepts or stores passwords. By default it refuses to overwrite an existing app
directory.`);
}

function fail(message) {
  console.error(`Meshingress deploy: ${message}`);
  process.exitCode = 1;
}

function requireSafe(value, name, expression) {
  if (!value || !expression.test(value)) {
    throw new Error(`${name} is missing or contains unsupported characters.`);
  }
  return value;
}

function shellQuote(value) {
  return `'${value.replaceAll("'", "'\"'\"'")}'`;
}

function run(command, argumentsList) {
  const result = spawnSync(command, argumentsList, {
    cwd: process.cwd(),
    shell: false,
    stdio: 'inherit',
  });
  if (result.error) {
    throw new Error(`Could not start ${command}: ${result.error.message}`);
  }
  if (result.status !== 0) {
    throw new Error(`${command} exited with status ${result.status}.`);
  }
}

function cleanup(target, stage) {
  spawnSync('ssh', [target, `rm -rf -- ${shellQuote(stage)}`], {
    cwd: process.cwd(),
    shell: false,
    stdio: 'ignore',
  });
}

function sourceImportWorkerScript(container) {
  return [
    'while true; do',
    `  /usr/bin/docker exec -u www-data ${container} php occ meshingress:source-import:work --watch --interval=${SOURCE_IMPORT_WORKER_INTERVAL_SECONDS} --no-interaction --quiet`,
    '  sleep 3',
    'done',
  ].join('\n');
}

function sourceImportWorkerCron(container) {
  return `@reboot /bin/sh -c 'sleep 30; while true; do /usr/bin/docker exec -u www-data ${container} php occ meshingress:source-import:work --watch --interval=${SOURCE_IMPORT_WORKER_INTERVAL_SECONDS} --no-interaction --quiet; sleep 3; done' >/dev/null 2>&1 # ${SOURCE_IMPORT_WORKER_MARKER}\n`;
}

function configureSourceImportWorker(container) {
  const workerScriptBase64 = Buffer.from(sourceImportWorkerScript(container), 'utf8').toString('base64');
  const workerCronBase64 = Buffer.from(sourceImportWorkerCron(container), 'utf8').toString('base64');
  const workerCommandMarker = 'meshingress:source-import:work --watch';

  return [
    'worker_home=$(getent passwd "$(id -un)" | cut -d: -f6)',
    'test -n "$worker_home"',
    'state_dir="$worker_home/.local/state"',
    `pid_file="$state_dir/${SOURCE_IMPORT_WORKER_PID_FILE}"`,
    'install -d -m 700 "$state_dir"',
    'cron_tmp=$(mktemp)',
    'trap \'rm -f "$cron_tmp"\' EXIT',
    '(crontab -l 2>/dev/null || true) > "$cron_tmp"',
    `{ grep -Fv ${shellQuote(SOURCE_IMPORT_WORKER_MARKER)} "$cron_tmp" || true; printf '%s' ${shellQuote(workerCronBase64)} | base64 -d; } | crontab -`,
    'rm -f "$cron_tmp"',
    'trap - EXIT',
    'if test -s "$pid_file"; then',
    '  old_pid=$(cat "$pid_file")',
    `  if kill -0 "$old_pid" 2>/dev/null && ps -p "$old_pid" -o args= | grep -F -- ${shellQuote(workerCommandMarker)} >/dev/null; then`,
    '    pkill -TERM -P "$old_pid" 2>/dev/null || true',
    '    kill -TERM "$old_pid" 2>/dev/null || true',
    '    for attempt in 1 2 3 4 5; do',
    '      kill -0 "$old_pid" 2>/dev/null || break',
    '      sleep 1',
    '    done',
    '  fi',
    'fi',
    'rm -f "$pid_file"',
    `nohup /bin/sh -c "$(printf '%s' ${shellQuote(workerScriptBase64)} | base64 -d)" >/dev/null 2>&1 &`,
    'worker_pid=$!',
    'printf "%s\\n" "$worker_pid" > "$pid_file"',
    'sleep 1',
    'kill -0 "$worker_pid"',
    `ps -p "$worker_pid" -o args= | grep -F -- ${shellQuote(workerCommandMarker)} >/dev/null`,
    `crontab -l | grep -F -- ${shellQuote(SOURCE_IMPORT_WORKER_MARKER)} >/dev/null`,
  ].join('\n');
}

if (process.argv.includes('--help') || process.argv.includes('-h')) {
  usage();
  process.exit(0);
}

try {
  const replaceExisting = process.argv.includes('--replace-existing');
  const unsupportedArguments = process.argv.slice(2).filter(argument => argument !== '--replace-existing');
  if (unsupportedArguments.length > 0) {
    throw new Error(`Unsupported argument(s): ${unsupportedArguments.join(', ')}`);
  }
  const host = requireSafe(process.env.MESHINGRESS_NEXTCLOUD_DEPLOY_HOST, 'MESHINGRESS_NEXTCLOUD_DEPLOY_HOST', /^[A-Za-z0-9.-]+$/);
  const user = requireSafe(process.env.MESHINGRESS_NEXTCLOUD_DEPLOY_USER, 'MESHINGRESS_NEXTCLOUD_DEPLOY_USER', /^[A-Za-z0-9._-]+$/);
  const container = requireSafe(process.env.MESHINGRESS_NEXTCLOUD_DEPLOY_CONTAINER || 'nextcloud-app.v1', 'MESHINGRESS_NEXTCLOUD_DEPLOY_CONTAINER', /^[A-Za-z0-9_.-]+$/);
  const infoXml = `${APP_DIRECTORY}/appinfo/info.xml`;
  if (!existsSync(infoXml)) {
    throw new Error(`Expected ${infoXml} was not found.`);
  }

  process.chdir(dirname(APP_DIRECTORY));
  const target = `${user}@${host}`;
  const stage = `/tmp/${APP_ID}-install-${process.pid}-${Date.now()}`;
  const localSource = APP_ID;
  const appPath = `${CUSTOM_APPS_DIRECTORY}/${APP_ID}`;
  const replacementPath = `${CUSTOM_APPS_DIRECTORY}/.${APP_ID}-replacement-${process.pid}-${Date.now()}`;
  const backupPath = `${CUSTOM_APPS_DIRECTORY}/.${APP_ID}-backup-${process.pid}-${Date.now()}`;

  try {
    run('ssh', [target, `set -eu; test ! -e ${shellQuote(stage)}; mkdir -m 700 ${shellQuote(stage)}`]);
    run('scp', ['-r', '-p', localSource, `${target}:${stage}/`]);
    const deployment = replaceExisting
      ? [
          `docker exec ${shellQuote(container)} sh -lc ${shellQuote(`test -d ${shellQuote(appPath)} && test ! -e ${shellQuote(replacementPath)}`)}`,
          `docker cp ${shellQuote(`${stage}/${APP_ID}`)} ${shellQuote(`${container}:${replacementPath}`)}`,
          `docker exec ${shellQuote(container)} chown -R www-data:www-data ${shellQuote(replacementPath)}`,
          `docker exec ${shellQuote(container)} sh -lc ${shellQuote(`find ${shellQuote(replacementPath)} -type f -name '*.php' -exec php -l {} \\;`)}`,
          `docker exec ${shellQuote(container)} sh -lc ${shellQuote(`mv ${shellQuote(appPath)} ${shellQuote(backupPath)} && mv ${shellQuote(replacementPath)} ${shellQuote(appPath)}`)}`,
          `docker exec -u www-data ${shellQuote(container)} php occ app:enable ${APP_ID}`,
          `docker exec ${shellQuote(container)} rm -rf -- ${shellQuote(backupPath)}`,
        ]
      : [
          `docker exec ${shellQuote(container)} sh -lc ${shellQuote(`test ! -e ${shellQuote(appPath)}`)}`,
          `docker cp ${shellQuote(`${stage}/${APP_ID}`)} ${shellQuote(`${container}:${appPath}`)}`,
          `docker exec ${shellQuote(container)} chown -R www-data:www-data ${shellQuote(appPath)}`,
          `docker exec ${shellQuote(container)} sh -lc ${shellQuote(`find ${shellQuote(appPath)} -type f -name '*.php' -exec php -l {} \\;`)}`,
          `docker exec -u www-data ${shellQuote(container)} php occ app:enable ${APP_ID}`,
        ];
    run('ssh', [target, [
      'set -eu',
      ...deployment,
      configureSourceImportWorker(container),
      `docker exec -u www-data ${shellQuote(container)} php occ router:list ${APP_ID} --ocs`,
    ].join('; ')]);
  } finally {
    cleanup(target, stage);
  }

  console.log(`Meshingress deploy: ${APP_ID} installed and enabled on ${host}.`);
} catch (error) {
  fail(error instanceof Error ? error.message : String(error));
}
