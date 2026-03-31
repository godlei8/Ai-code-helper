# Aliyun Release Bundle

This directory contains the Aliyun release builder and the server-side templates used by the bundle.

## Build on Windows

```powershell
.\deploy\aliyun\build-release.ps1
```

The command creates a versioned bundle under `release/aliyun/`:

```text
release/aliyun/
└─ ai-code-helper-aliyun-<timestamp>-<commit>/
   ├─ VERSION
   ├─ build-info.json
   ├─ SHA256SUMS
   ├─ app/
   │  ├─ backend/ai-code-helper.jar
   │  └─ frontend/...
   ├─ config/application-prod.yml.example
   └─ ops/
      ├─ scripts/install.sh
      ├─ scripts/rollback.sh
      ├─ scripts/status.sh
      ├─ nginx/ai-code-helper.conf.template
      └─ systemd/ai-code-helper.service.template
```

`build-info.json` records the branch, commit, build time, backend hash, and frontend asset hashes.

## Deploy on an Aliyun Linux server

1. Upload the generated zip to the server and unzip it.
2. Enter the extracted bundle directory.
3. Run the installer as root:

```bash
sudo APP_ROOT=/opt/ai-code-helper \
  SERVER_NAME=your.domain.com \
  APP_USER=root \
  APP_GROUP=root \
  bash ops/scripts/install.sh
```

4. Verify the running version:

```bash
sudo APP_ROOT=/opt/ai-code-helper bash ops/scripts/status.sh
```

## Server layout after install

```text
/opt/ai-code-helper/
├─ current -> /opt/ai-code-helper/releases/<version>
├─ releases/
│  └─ <version>/
├─ shared/
│  ├─ config/application-prod.yml
│  ├─ logs/
│  └─ minimax-mcp/
```

The installer also renders and installs:

- `/etc/systemd/system/ai-code-helper.service`
- `/etc/nginx/conf.d/ai-code-helper.conf`

`index.html` is configured as no-cache, while hashed assets are configured for long cache lifetimes.

## Roll back

```bash
sudo APP_ROOT=/opt/ai-code-helper bash ops/scripts/rollback.sh <version>
```

## Notes

- The installer is idempotent for config creation: it only seeds `application-prod.yml` if it does not already exist.
- Each release keeps its own `VERSION`, `build-info.json`, and `SHA256SUMS`, so you can confirm exactly what is running.
- The server must have `java`, `nginx`, and `systemd` available. If you use MCP, make sure `uvx` is also installed.
