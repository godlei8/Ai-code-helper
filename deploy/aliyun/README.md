# Aliyun Deployment Package

This directory contains the files that are copied into the Aliyun deployment bundle.

## Package layout

```text
ai-code-helper-aliyun/
├─ backend/
│  ├─ ai-code-helper.jar
│  └─ config/
│     └─ application-prod.yml.example
├─ frontend/
│  └─ ...
├─ nginx/
│  └─ ai-code-helper.conf
├─ scripts/
│  ├─ start-backend.sh
│  └─ stop-backend.sh
└─ systemd/
   └─ ai-code-helper.service
```

## Recommended deployment

1. Upload `frontend/` to an Nginx static directory.
2. Upload `backend/` and `scripts/` to `/opt/ai-code-helper`.
3. Copy `backend/config/application-prod.yml.example` to `application-prod.yml` and fill in your real keys.
4. Update `nginx/ai-code-helper.conf` to your actual domain and static root.
5. Start the backend with `scripts/start-backend.sh`.
6. Reload Nginx after enabling the site config.
