const fs = require('node:fs');
const path = require('node:path');
const admin = require('firebase-admin');

function getCredentialPath() {
  const envPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH || process.env.GOOGLE_APPLICATION_CREDENTIALS;
  if (!envPath) {
    throw new Error(
      'Missing FIREBASE_SERVICE_ACCOUNT_PATH (or GOOGLE_APPLICATION_CREDENTIALS). ' +
        'Set it to your Firebase service account JSON file path.'
    );
  }

  const resolved = path.resolve(envPath);
  if (!fs.existsSync(resolved)) {
    throw new Error(`Service account file not found: ${resolved}`);
  }

  return resolved;
}

function initFirebase(credentialPath) {
  const raw = fs.readFileSync(credentialPath, 'utf8');
  const serviceAccount = JSON.parse(raw);

  if (!admin.apps.length) {
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
  }

  return admin.app();
}

function checkMcpPackage() {
  try {
    const pkgPath = require.resolve('@modelcontextprotocol/server/package.json');
    const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf8'));
    return pkg.version || 'unknown';
  } catch {
    return null;
  }
}

function main() {
  const checkOnly = process.argv.includes('--check-only');
  const credentialPath = getCredentialPath();
  const app = initFirebase(credentialPath);
  const mcpVersion = checkMcpPackage();

  console.log('Firebase initialized successfully.');
  console.log(`Project: ${app.options.credential.projectId || 'unknown'}`);
  console.log(`Credential file: ${credentialPath}`);

  if (mcpVersion) {
    console.log(`@modelcontextprotocol/server detected: v${mcpVersion}`);
  } else {
    console.log('@modelcontextprotocol/server package not detected yet. Run npm install.');
  }

  if (checkOnly) {
    process.exit(0);
  }

  console.log('Server scaffold is ready. Integrate MCP tool handlers in this file as needed.');
}

main();
