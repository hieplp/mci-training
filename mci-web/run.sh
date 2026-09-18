#!/usr/bin/env bash
# Run mci-web on all interfaces (LAN-accessible) at port 8081.
set -euo pipefail
cd "$(dirname "$0")"
exec ./gradlew bootRun --console=plain -q --args='--server.address=0.0.0.0 --server.port=8081'
