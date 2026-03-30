#!/bin/bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="$BASE_DIR/backend"
CONFIG_DIR="$APP_DIR/config"
LOG_DIR="$APP_DIR/logs"
PID_FILE="$APP_DIR/app.pid"
JAR_FILE="$APP_DIR/ai-code-helper.jar"

mkdir -p "$CONFIG_DIR" "$LOG_DIR"

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  echo "Backend is already running with PID $(cat "$PID_FILE")"
  exit 0
fi

nohup java -Xms512m -Xmx1024m \
  -jar "$JAR_FILE" \
  --spring.profiles.active=prod \
  --spring.config.additional-location="$CONFIG_DIR/" \
  > "$LOG_DIR/app.out" 2>&1 &

echo $! > "$PID_FILE"
echo "Backend started with PID $(cat "$PID_FILE")"
