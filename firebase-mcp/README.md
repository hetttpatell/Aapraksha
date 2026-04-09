# firebase-mcp

Firebase MCP scaffold configured to use a local Firebase service-account JSON file path.

## 1) Install dependencies

```powershell
cd c:\Users\Jitu\AndroidStudioProjects\Aapraksha\firebase-mcp
npm install
```

## 2) Configure key file path

Set one of these environment variables:

- `FIREBASE_SERVICE_ACCOUNT_PATH` (recommended)
- `GOOGLE_APPLICATION_CREDENTIALS`

PowerShell example:

```powershell
$env:FIREBASE_SERVICE_ACCOUNT_PATH="C:\Users\Jitu\Downloads\aap-raksha-cf3f4-firebase-adminsdk-fbsvc-beb844a11a.json"
```

## 3) Verify key works

```powershell
npm run check:key
```

## 4) Start scaffold

```powershell
npm start
```

## Security

- Never commit service-account JSON files.
- If a key is ever exposed, revoke it and generate a new one.
