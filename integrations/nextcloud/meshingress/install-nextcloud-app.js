#!/usr/bin/env node
'use strict';

const { existsSync } = require('node:fs');
const { spawnSync } = require('node:child_process');
const { dirname } = require('node:path');

const APP_ID = 'meshingress';
const APP_DIRECTORY = dirname(__filename);
const CUSTOM_APPS_DIRECTORY = '/var/www/html/custom_apps';

function fail(message) {
  console.error(`Meshingress deploy: ${message}`);
  process.exitCode = 1;
}

function requireSafe(value, name, expression) {
  if (!value || !expression.test(value)) throw new Error(`${name} is missing or contains unsupported characters.`);
  return value;
}

function quote(value) {
  return `'${value.replaceAll("'", "'\"'\"'")}'`;
}

function run(command, argumentsList) {
  const result = spawnSync(command, argumentsList, { cwd: process.cwd(), shell: false, stdio: 'inherit' });
  if (result.error) throw new Error(`Could not start ${command}: ${result.error.message}`);
  if (result.status !== 0) throw new Error(`${command} exited with status ${result.status}.`);
}

function usage() {
  console.log(`Usage:
  MESHINGRESS_NEXTCLOUD_DEPLOY_HOST=<host> \\
  MESHINGRESS_NEXTCLOUD_DEPLOY_USER=<linux-user> \\
  node integrations/nextcloud/meshingress/install-nextcloud-app.js [--replace-existing]

The helper stages the plain meshingress app, PHP-lints it in the target container,
and enables it. It does not restart Nextcloud or configure a persistent worker.`);
}

if (process.argv.includes('--help') || process.argv.includes('-h')) {
  usage();
  process.exit(0);
}

try {
  const replaceExisting = process.argv.includes('--replace-existing');
  const unsupported = process.argv.slice(2).filter(value => value !== '--replace-existing');
  if (unsupported.length) throw new Error(`Unsupported argument(s): ${unsupported.join(', ')}`);
  const host = requireSafe(process.env.MESHINGRESS_NEXTCLOUD_DEPLOY_HOST, 'MESHINGRESS_NEXTCLOUD_DEPLOY_HOST', /^[A-Za-z0-9.-]+$/);
  const user = requireSafe(process.env.MESHINGRESS_NEXTCLOUD_DEPLOY_USER, 'MESHINGRESS_NEXTCLOUD_DEPLOY_USER', /^[A-Za-z0-9._-]+$/);
  const container = requireSafe(process.env.MESHINGRESS_NEXTCLOUD_DEPLOY_CONTAINER || 'nextcloud-app.v1', 'MESHINGRESS_NEXTCLOUD_DEPLOY_CONTAINER', /^[A-Za-z0-9_.-]+$/);
  if (!existsSync(`${APP_DIRECTORY}/appinfo/info.xml`)) throw new Error('appinfo/info.xml is missing.');

  process.chdir(dirname(APP_DIRECTORY));
  const target = `${user}@${host}`;
  const nonce = `${process.pid}-${Date.now()}`;
  const stage = `/tmp/${APP_ID}-install-${nonce}`;
  const appPath = `${CUSTOM_APPS_DIRECTORY}/${APP_ID}`;
  const replacement = `${CUSTOM_APPS_DIRECTORY}/.${APP_ID}-replacement-${nonce}`;
  const backup = `${CUSTOM_APPS_DIRECTORY}/.${APP_ID}-backup-${nonce}`;
  try {
    run('ssh', [target, `set -eu; mkdir -m 700 ${quote(stage)}`]);
    run('scp', ['-r', '-p', APP_ID, `${target}:${stage}/`]);
    const deploy = replaceExisting
      ? [
          `docker exec ${quote(container)} sh -lc ${quote(`test -d ${quote(appPath)} && test ! -e ${quote(replacement)}`)}`,
          `docker cp ${quote(`${stage}/${APP_ID}`)} ${quote(`${container}:${replacement}`)}`,
          `docker exec ${quote(container)} chown -R www-data:www-data ${quote(replacement)}`,
          `docker exec ${quote(container)} sh -lc ${quote(`find ${quote(replacement)} -type f -name '*.php' -exec php -l {} \\;`)}`,
          `docker exec ${quote(container)} sh -lc ${quote(`mv ${quote(appPath)} ${quote(backup)} && mv ${quote(replacement)} ${quote(appPath)}`)}`,
          `docker exec -u www-data ${quote(container)} php occ app:enable ${APP_ID}`,
          `docker exec ${quote(container)} rm -rf -- ${quote(backup)}`,
        ]
      : [
          `docker exec ${quote(container)} sh -lc ${quote(`test ! -e ${quote(appPath)}`)}`,
          `docker cp ${quote(`${stage}/${APP_ID}`)} ${quote(`${container}:${appPath}`)}`,
          `docker exec ${quote(container)} chown -R www-data:www-data ${quote(appPath)}`,
          `docker exec ${quote(container)} sh -lc ${quote(`find ${quote(appPath)} -type f -name '*.php' -exec php -l {} \\;`)}`,
          `docker exec -u www-data ${quote(container)} php occ app:enable ${APP_ID}`,
        ];
    run('ssh', [target, ['set -eu', ...deploy, `docker exec -u www-data ${quote(container)} php occ router:list ${APP_ID} --ocs`].join('; ')]);
  } finally {
    spawnSync('ssh', [target, `rm -rf -- ${quote(stage)}`], { cwd: process.cwd(), shell: false, stdio: 'ignore' });
  }
  console.log(`Meshingress deploy: ${APP_ID} installed and enabled on ${host}. Configure the durable workspace worker separately.`);
} catch (error) {
  fail(error instanceof Error ? error.message : String(error));
}
