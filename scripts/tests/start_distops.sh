#!/usr/bin/env bash
set -Eeo pipefail

# Start distops container for testing
# Usage: bash scripts/tests/start_distops.sh

HOST="${HOST:-localhost}"
IMAGE="${IMAGE:-dyrnq/distops:latest}"
CONTAINER_NAME="${CONTAINER_NAME:-distops-test}"
API_PORT="${API_PORT:-12680}"
REGISTRY_PORT="${REGISTRY_PORT:-5000}"

echo "[INFO] Starting distops container..."
echo "  Image: $IMAGE"
echo "  Name:  $CONTAINER_NAME"
echo "  Host:  $HOST"

# Clean up existing
docker rm -f "$CONTAINER_NAME" 2>/dev/null || true

docker run -d --name "$CONTAINER_NAME" --network host \
  -e TZ=Asia/Shanghai \
  -e JAVA_OPTS="-server -Xms256m -Xms256m -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -Djava.net.preferIPv4Stack=true -Dspring.flyway.enabled=true" \
  -e OTEL_TRACES_EXPORTER=none \
  "$IMAGE"

echo "[INFO] Waiting for distops to be ready..."
for i in $(seq 1 30); do
  if curl -sf "http://${HOST}:${API_PORT}/" >/dev/null 2>&1; then
    echo "[INFO] distops ready on ${HOST}:${API_PORT}"
    exit 0
  fi
  sleep 2
done

echo "[ERROR] distops failed to start within 60s"
docker logs "$CONTAINER_NAME" --tail 20
exit 1
