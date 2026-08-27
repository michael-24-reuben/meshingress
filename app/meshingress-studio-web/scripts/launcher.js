#!/usr/bin/env node

/**
 * Meshingress Studio Web - Launch & Build Orchestrator
 * 
 * Provides:
 *  - Color-styled, timestamped event logging
 *  - Project and environment validation
 *  - Build artifact cleaning
 *  - Fresh compilation (TypeScript type-check + Vite build)
 *  - Build validation & asset integrity inspection
 *  - Interactive development server launch (npm run dev)
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawn, execSync } from 'node:child_process';
import os from 'node:os';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const ROOT_DIR = path.resolve(__dirname, '..');

// ANSI Color Palette
const colors = {
  reset: '\x1b[0m',
  bold: '\x1b[1m',
  dim: '\x1b[2m',
  italic: '\x1b[3m',
  underline: '\x1b[4m',

  // Foreground
  black: '\x1b[30m',
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  magenta: '\x1b[35m',
  cyan: '\x1b[36m',
  white: '\x1b[37m',
  gray: '\x1b[90m',

  // Bright Foreground
  brightRed: '\x1b[91m',
  brightGreen: '\x1b[92m',
  brightYellow: '\x1b[93m',
  brightBlue: '\x1b[94m',
  brightMagenta: '\x1b[95m',
  brightCyan: '\x1b[96m',
  brightWhite: '\x1b[97m',

  // Background
  bgBlue: '\x1b[44m',
  bgMagenta: '\x1b[45m',
  bgCyan: '\x1b[46m',
  bgGreen: '\x1b[42m',
  bgYellow: '\x1b[43m',
  bgRed: '\x1b[41m',
};

// Logger with timestamps, categories, and colors
class Logger {
  constructor(verbose = true) {
    this.verbose = verbose;
  }

  timestamp() {
    const now = new Date();
    return now.toTimeString().split(' ')[0];
  }

  prefix(category, color, bgColor = '') {
    const ts = `${colors.gray}[${this.timestamp()}]${colors.reset}`;
    const tag = bgColor 
      ? `${bgColor}${colors.bold}${colors.white} ${category} ${colors.reset}` 
      : `${color}${colors.bold}[${category}]${colors.reset}`;
    return `${ts} ${tag}`;
  }

  info(msg) {
    console.log(`${this.prefix('INFO', colors.brightBlue)} ${msg}`);
  }

  debug(msg) {
    if (this.verbose) {
      console.log(`${this.prefix('DEBUG', colors.magenta)} ${colors.dim}${msg}${colors.reset}`);
    }
  }

  validate(msg) {
    console.log(`${this.prefix('VALIDATE', colors.cyan)} ${msg}`);
  }

  clean(msg) {
    console.log(`${this.prefix('CLEAN', colors.yellow)} ${msg}`);
  }

  compile(msg) {
    console.log(`${this.prefix('COMPILE', colors.brightMagenta)} ${msg}`);
  }

  verify(msg) {
    console.log(`${this.prefix('VERIFY', colors.brightCyan)} ${msg}`);
  }

  server(msg) {
    console.log(`${this.prefix('SERVER', colors.brightGreen)} ${msg}`);
  }

  success(msg) {
    console.log(`${this.prefix('SUCCESS', colors.brightGreen, colors.bgGreen)} ${colors.bold}${msg}${colors.reset}`);
  }

  warn(msg) {
    console.log(`${this.prefix('WARN', colors.brightYellow, colors.bgYellow)} ${colors.yellow}${msg}${colors.reset}`);
  }

  error(msg) {
    console.error(`${this.prefix('ERROR', colors.brightRed, colors.bgRed)} ${colors.brightRed}${msg}${colors.reset}`);
  }

  banner() {
    console.log('\n' + colors.brightCyan + colors.bold +
      '  ╔══════════════════════════════════════════════════════════════╗\n' +
      '  ║              MESHINGRESS STUDIO WEB LAUNCHER                 ║\n' +
      '  ║             Validate ➔ Clean ➔ Compile ➔ Run                 ║\n' +
      '  ╚══════════════════════════════════════════════════════════════╝' + colors.reset + '\n');
  }
}

const logger = new Logger(true);

// CLI Argument Parsing
function parseArgs() {
  const args = process.argv.slice(2);
  const options = {
    skipBuild: false,
    buildOnly: false,
    cleanOnly: false,
    validateOnly: false,
    port: null,
    host: false,
    help: false,
    debug: false,
    rawArgs: args,
  };

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    if (arg === '--skip-build') options.skipBuild = true;
    else if (arg === '--build-only') options.buildOnly = true;
    else if (arg === '--clean-only') options.cleanOnly = true;
    else if (arg === '--validate-only') options.validateOnly = true;
    else if (arg === '--host') options.host = true;
    else if (arg === '--debug' || arg === '-d') options.debug = true;
    else if (arg === '--port' || arg === '-p') {
      if (i + 1 < args.length && !args[i + 1].startsWith('-')) {
        options.port = args[++i];
      }
    } else if (arg === '--help' || arg === '-h') {
      options.help = true;
    }
  }

  return options;
}

function printHelp() {
  console.log(`
${colors.bold}Usage:${colors.reset} node scripts/launcher.js [options]
       .\\launch.ps1 [options]
       launch.cmd [options]

${colors.bold}Options:${colors.reset}
  --help, -h          Show this help message
  --build-only        Run validation, clean, compile & verify, then exit
  --clean-only        Clean build artifacts (dist, .vite) and exit
  --validate-only     Run project and environment validation only and exit
  --skip-build        Skip clean & compilation, launch dev server directly
  --port, -p <port>   Set custom Vite development server port (default: 5173)
  --host              Expose dev server on local network
  --debug, -d         Enable verbose debug logging
`);
}

// Helper to run child command synchronously and return output/status
function runSync(command, args, cwd = ROOT_DIR, silent = false) {
  const startTime = Date.now();
  logger.debug(`Executing: ${command} ${args.join(' ')} in ${cwd}`);
  
  const isWindows = process.platform === 'win32';
  const cmd = isWindows && (command === 'npm' || command === 'npx' || command === 'tsc' || command === 'vite' || command === 'oxlint')
    ? `${command}.cmd`
    : command;

  try {
    const result = execSync(`${cmd} ${args.join(' ')}`, {
      cwd,
      stdio: silent ? 'pipe' : 'inherit',
      encoding: 'utf-8',
    });
    const elapsed = Date.now() - startTime;
    logger.debug(`Command finished successfully in ${elapsed}ms: ${command} ${args.join(' ')}`);
    return { success: true, output: result, elapsed };
  } catch (err) {
    const elapsed = Date.now() - startTime;
    logger.debug(`Command failed in ${elapsed}ms with exit code ${err.status}`);
    return { success: false, error: err, status: err.status, elapsed };
  }
}

// Helper to format bytes
function formatBytes(bytes, decimals = 2) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

// 1. Validation Function
async function validateProject() {
  const startTime = Date.now();
  logger.validate('Starting pre-flight project & environment validation...');

  // System & Runtime Info
  logger.debug(`OS: ${os.type()} ${os.release()} (${os.arch()})`);
  logger.debug(`Node.js version: ${process.version}`);
  logger.debug(`Working directory: ${ROOT_DIR}`);

  const nodeMajor = parseInt(process.version.slice(1).split('.')[0], 10);
  if (nodeMajor < 18) {
    logger.warn(`Node.js version ${process.version} detected. Recommended version is >= 18.0.0.`);
  } else {
    logger.validate(`Node.js runtime: ${colors.green}${process.version}${colors.reset} (Valid)`);
  }

  // Critical File Verification
  const criticalFiles = [
    { file: 'package.json', desc: 'Package Manifest' },
    { file: 'tsconfig.json', desc: 'TypeScript Solution Config' },
    { file: 'tsconfig.app.json', desc: 'TypeScript App Config' },
    { file: 'tsconfig.node.json', desc: 'TypeScript Node Config' },
    { file: 'vite.config.ts', desc: 'Vite Configuration' },
    { file: 'index.html', desc: 'HTML Entry Point' },
    { file: 'src/main.tsx', desc: 'React Main Entry' },
    { file: 'src/App.tsx', desc: 'Root Application Component' },
  ];

  let missingFiles = 0;
  for (const item of criticalFiles) {
    const fullPath = path.join(ROOT_DIR, item.file);
    if (fs.existsSync(fullPath)) {
      const stats = fs.statSync(fullPath);
      logger.debug(`Checked ${item.file} [${formatBytes(stats.size)}] - Found`);
    } else {
      logger.error(`Missing critical file: ${item.file} (${item.desc})`);
      missingFiles++;
    }
  }

  if (missingFiles > 0) {
    throw new Error(`Validation failed: ${missingFiles} critical file(s) missing.`);
  }

  // Dependency Verification
  const nodeModulesPath = path.join(ROOT_DIR, 'node_modules');
  if (!fs.existsSync(nodeModulesPath)) {
    logger.warn('Dependencies (node_modules) not found. Triggering automated installation...');
    const installRes = runSync('npm', ['install']);
    if (!installRes.success) {
      throw new Error('Automated npm install failed. Please inspect npm logs.');
    }
    logger.success('Dependencies installed successfully.');
  } else {
    logger.validate(`Dependencies: ${colors.green}node_modules present${colors.reset}`);
  }

  // Linting / Syntax Validation (Fast check)
  logger.validate('Running linter pre-check...');
  const lintRes = runSync('npx', ['oxlint'], ROOT_DIR, true);
  if (lintRes.success) {
    logger.validate(`Linter check: ${colors.green}Passed cleanly${colors.reset}`);
  } else {
    logger.warn('Linter reported warnings or notices (non-blocking). Proceeding with build pipeline.');
  }

  const elapsed = Date.now() - startTime;
  logger.validate(`Validation completed successfully in ${elapsed}ms.`);
  return true;
}

// 2. Clean Function
async function cleanBuildArtifacts() {
  const startTime = Date.now();
  logger.clean('Cleaning build artifacts and compiler caches...');

  const targets = [
    { path: path.join(ROOT_DIR, 'dist'), desc: 'Production Build Output (dist)' },
    { path: path.join(ROOT_DIR, 'node_modules', '.vite'), desc: 'Vite Cache (node_modules/.vite)' },
    { path: path.join(ROOT_DIR, 'tsconfig.tsbuildinfo'), desc: 'TypeScript Build Info Root' },
    { path: path.join(ROOT_DIR, 'tsconfig.app.tsbuildinfo'), desc: 'TypeScript App Build Info' },
    { path: path.join(ROOT_DIR, 'tsconfig.node.tsbuildinfo'), desc: 'TypeScript Node Build Info' },
  ];

  let cleanedCount = 0;
  for (const target of targets) {
    if (fs.existsSync(target.path)) {
      try {
        fs.rmSync(target.path, { recursive: true, force: true });
        logger.clean(`Removed ${colors.yellow}${target.desc}${colors.reset}`);
        cleanedCount++;
      } catch (err) {
        logger.warn(`Could not remove ${target.path}: ${err.message}`);
      }
    } else {
      logger.debug(`Clean skipped (does not exist): ${target.desc}`);
    }
  }

  const elapsed = Date.now() - startTime;
  logger.clean(`Clean complete. ${cleanedCount} target(s) removed in ${elapsed}ms.`);
}

// 3. Compile Function
async function compileProject() {
  const startTime = Date.now();
  logger.compile('Starting fresh compilation pipeline...');

  // Step 3a: TypeScript compilation / type check
  logger.compile(`[1/2] TypeScript Project Reference Compilation (${colors.bold}tsc -b${colors.reset})...`);
  const tscRes = runSync('npx', ['tsc', '-b']);
  if (!tscRes.success) {
    throw new Error('TypeScript compilation (tsc -b) failed with errors.');
  }
  logger.debug(`TypeScript compilation succeeded in ${tscRes.elapsed}ms.`);

  // Step 3b: Vite Build
  logger.compile(`[2/2] Vite Production Bundler (${colors.bold}vite build${colors.reset})...`);
  const viteRes = runSync('npx', ['vite', 'build']);
  if (!viteRes.success) {
    throw new Error('Vite production build failed.');
  }
  logger.debug(`Vite build succeeded in ${viteRes.elapsed}ms.`);

  const elapsed = Date.now() - startTime;
  logger.compile(`Compilation finished successfully in ${elapsed}ms.`);
}

// 4. Validate Compile Function
async function validateCompile() {
  const startTime = Date.now();
  logger.verify('Validating compiled build artifacts...');

  const distPath = path.join(ROOT_DIR, 'dist');
  if (!fs.existsSync(distPath)) {
    throw new Error('Build verification failed: dist/ directory was not generated.');
  }

  const htmlPath = path.join(distPath, 'index.html');
  if (!fs.existsSync(htmlPath)) {
    throw new Error('Build verification failed: dist/index.html is missing.');
  }
  const htmlStats = fs.statSync(htmlPath);
  if (htmlStats.size === 0) {
    throw new Error('Build verification failed: dist/index.html is empty.');
  }
  logger.verify(`Verified ${colors.brightCyan}dist/index.html${colors.reset} [${formatBytes(htmlStats.size)}]`);

  // Inspect Assets Directory
  const assetsDir = path.join(distPath, 'assets');
  if (!fs.existsSync(assetsDir)) {
    throw new Error('Build verification failed: dist/assets directory is missing.');
  }

  const files = fs.readdirSync(assetsDir);
  const jsFiles = files.filter(f => f.endsWith('.js'));
  const cssFiles = files.filter(f => f.endsWith('.css'));

  if (jsFiles.length === 0) {
    throw new Error('Build verification failed: No JavaScript bundle generated in dist/assets.');
  }
  if (cssFiles.length === 0) {
    logger.warn('No CSS bundle found in dist/assets.');
  }

  logger.verify(`Generated bundles in ${colors.brightCyan}dist/assets/${colors.reset}:`);
  for (const file of files) {
    const filePath = path.join(assetsDir, file);
    const stats = fs.statSync(filePath);
    const isJS = file.endsWith('.js');
    const isCSS = file.endsWith('.css');
    const typeLabel = isJS ? `${colors.yellow}[JS]` : isCSS ? `${colors.cyan}[CSS]` : `${colors.gray}[ASSET]`;
    logger.verify(`  ${typeLabel}${colors.reset} ${file} (${formatBytes(stats.size)})`);
  }

  const elapsed = Date.now() - startTime;
  logger.verify(`Artifact validation passed (${files.length + 1} artifacts verified in ${elapsed}ms).`);
  return true;
}

// 5. Dev Server Runner
function runDevServer(options) {
  logger.server('Launching Vite Development Server (npm run dev)...');

  const isWindows = process.platform === 'win32';
  const npmCmd = isWindows ? 'npm.cmd' : 'npm';

  const devArgs = ['run', 'dev'];
  const extraArgs = [];

  if (options.port) {
    extraArgs.push('--port', options.port);
  }
  if (options.host) {
    extraArgs.push('--host');
  }

  if (extraArgs.length > 0) {
    devArgs.push('--', ...extraArgs);
  }

  logger.debug(`Spawning: ${npmCmd} ${devArgs.join(' ')}`);

  const child = spawn(npmCmd, devArgs, {
    cwd: ROOT_DIR,
    stdio: 'inherit',
    shell: isWindows,
  });

  const cleanup = () => {
    logger.server('Shutting down development server gracefully...');
    if (child && !child.killed) {
      if (isWindows && child.pid) {
        try {
          execSync(`taskkill /pid ${child.pid} /T /F`, { stdio: 'ignore' });
        } catch {
          // Ignore process cleanup errors if already terminated
        }
      } else {
        child.kill('SIGINT');
      }
    }
  };

  process.on('SIGINT', () => {
    cleanup();
    process.exit(0);
  });

  process.on('SIGTERM', () => {
    cleanup();
    process.exit(0);
  });

  child.on('error', (err) => {
    logger.error(`Failed to start development server: ${err.message}`);
    process.exit(1);
  });

  child.on('close', (code) => {
    if (code !== 0 && code !== null) {
      logger.error(`Development server exited with code ${code}`);
      process.exit(code);
    } else {
      logger.server('Development server stopped.');
      process.exit(0);
    }
  });
}

// Main Orchestrator Flow
async function main() {
  const options = parseArgs();

  if (options.help) {
    printHelp();
    process.exit(0);
  }

  logger.verbose = options.debug || true;
  logger.banner();

  try {
    // 1. Validation
    await validateProject();
    if (options.validateOnly) {
      logger.success('Validation completed. (Exiting as --validate-only was requested)');
      process.exit(0);
    }

    // 2. Clean
    if (!options.skipBuild || options.cleanOnly) {
      await cleanBuildArtifacts();
      if (options.cleanOnly) {
        logger.success('Clean operation completed. (Exiting as --clean-only was requested)');
        process.exit(0);
      }
    }

    // 3. Compile afresh & 4. Validate compile
    if (!options.skipBuild) {
      await compileProject();
      await validateCompile();
      logger.success('Build & verification pipeline completed with zero errors!');
    } else {
      logger.info('Skipping fresh compilation step (--skip-build specified).');
    }

    if (options.buildOnly) {
      logger.success('Build-only target complete. (Exiting as --build-only was requested)');
      process.exit(0);
    }

    // 5. Run Dev Service
    runDevServer(options);

  } catch (err) {
    logger.error(`Pipeline halted with error: ${err.message}`);
    if (options.debug && err.stack) {
      console.error(colors.dim + err.stack + colors.reset);
    }
    process.exit(1);
  }
}

main();
