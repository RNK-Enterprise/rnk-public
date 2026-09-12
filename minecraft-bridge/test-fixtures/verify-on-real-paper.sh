#!/bin/bash
# Live verification: adapted artifact runs on a REAL Paper server.
#
#   bash verify-on-real-paper.sh [adapted.jar] [server-dir]
#
# Steps: fetch Paper 1.20.1 (PaperMC fill API) if absent, accept EULA,
# install the adapted plugin, boot headless, watch the log for the mod's
# real output line, then stop the server. Exit 0 = proven on real Paper.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ADAPTED="${1:-/tmp/crossloader-demo/heartsync-fabric-adapted.jar}"
SERVER_DIR="${2:-/tmp/paper-live}"
PAPER_JAR="$SERVER_DIR/paper.jar"
LOG="$SERVER_DIR/logs/latest.log"

[ -f "$ADAPTED" ] || { echo "adapted JAR not found: $ADAPTED (run e2e-crossloader first)"; exit 1; }
mkdir -p "$SERVER_DIR/plugins"

if [ ! -f "$PAPER_JAR" ]; then
  echo "── Downloading Paper 1.20.1 (fill.papermc.io)..."
  URL=$(curl -s https://fill.papermc.io/v3/projects/paper/versions/1.20.1/builds/latest \
        | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['downloads'][list(d['downloads'])[0]]['url'])")
  curl -sL -o "$PAPER_JAR" "$URL"
fi
echo "   Paper JAR: $(stat -c%s "$PAPER_JAR") bytes"

echo "── Accept EULA + install adapted plugin"
echo "eula=true" > "$SERVER_DIR/eula.txt"
cp "$ADAPTED" "$SERVER_DIR/plugins/heartsync-adapted.jar"

echo "── Booting real Paper server (headless, watchdog 180s)"
rm -f "$LOG"
cd "$SERVER_DIR"
timeout 180 java -Xmx1G -DPaper.IgnoreJavaVersion=true -jar paper.jar --nogui > /tmp/paper-boot.log 2>&1 &
SERVER_PID=$!

echo "── Watching for the mod's REAL output line..."
FOUND=0
for i in $(seq 1 90); do
  sleep 2
  if grep -q "\[HeartSync\] onInitializeServer: Fabric DedicatedServerModInitializer contract reached" "$LOG" 2>/dev/null; then
    FOUND=1
    break
  fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    echo "   server process exited before the mod line appeared"
    break
  fi
  if grep -q "Done (" "$LOG" 2>/dev/null && [ "$i" -gt 30 ]; then
    # Server fully started but no mod line after grace period
    break
  fi
done

if [ "$FOUND" = "1" ]; then
  echo ""
  echo "━━━ PROOF: mod's real code executed on REAL Paper ━━━"
  grep -E "RNK adapted artifact enabled|\[HeartSync\]" "$LOG" | head -5
  echo ""
else
  echo "── mod line NOT found; last server output:"
  tail -25 /tmp/paper-boot.log 2>/dev/null || tail -25 "$LOG" 2>/dev/null
fi

kill "$SERVER_PID" 2>/dev/null || true
sleep 2
kill -9 "$SERVER_PID" 2>/dev/null || true

[ "$FOUND" = "1" ] && exit 0 || exit 1
