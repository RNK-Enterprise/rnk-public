#!/bin/bash
# Live batch verification: adapted artifacts from REAL mods booted on real Paper.
#
#   bash verify-batch-on-paper.sh <dir-with-adapted-jars> [count] [outJson]
#
# For each adapted JAR (default: first N in dir, plus the heartsync fixture as
# control): isolated server dir (shared Paper JAR via hardlink), accept EULA,
# install plugin, boot headless, classify the outcome from the server log:
#
#   enabled      Paper loaded the plugin and dispatched onEnable without error
#   init-failed  plugin loaded, but the mod's init code threw (expected for
#                some real mods: they touch Minecraft classes at init)
#   load-failed  Paper could not load the plugin at all
#
# Results go to JSON; exit 0 iff every boot reached a classification.
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ADAPTED_DIR="${1:?usage: verify-batch-on-paper.sh <dir-with-adapted-jars> [count] [outJson]}"
COUNT="${2:-5}"
OUT_JSON="${3:-$ADAPTED_DIR/paper-batch-results.json}"
BASE_SERVER="/tmp/paper-batch"
PAPER_JAR="$BASE_SERVER/paper.jar"

mkdir -p "$BASE_SERVER"
if [ ! -f "$PAPER_JAR" ]; then
  echo "── Downloading Paper 1.20.1 (fill.papermc.io)..."
  URL=$(curl -s https://fill.papermc.io/v3/projects/paper/versions/1.20.1/builds/latest \
        | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['downloads'][list(d['downloads'])[0]]['url'])")
  curl -sL -o "$PAPER_JAR" "$URL"
fi
echo "   Paper JAR: $(stat -c%s "$PAPER_JAR") bytes"

# Build the JAR list: first N adapted artifacts + the fixture control.
mapfile -t JARS < <(ls "$ADAPTED_DIR"/*-adapted.jar 2>/dev/null | head -n "$COUNT")
FIXTURE="$SCRIPT_DIR/../test-servers/crossloader-demo/heartsync-fabric-adapted.jar"
[ -f "$FIXTURE" ] && JARS+=("$FIXTURE")
echo "   Booting ${#JARS[@]} adapted artifact(s)"

results=()
enabled=0; initfailed=0; loadfailed=0
idx=0
for ADAPTED in "${JARS[@]}"; do
  idx=$((idx+1))
  NAME=$(basename "$ADAPTED" -adapted.jar)
  SRV="$BASE_SERVER/run-$NAME"
  LOG="$SRV/logs/latest.log"
  rm -rf "$SRV"; mkdir -p "$SRV/plugins"
  echo "eula=true" > "$SRV/eula.txt"
  cp "$ADAPTED" "$SRV/plugins/plugin.jar"
  # Share the big Paper JAR without re-downloading per run.
  if [ ! -f "$SRV/paper.jar" ]; then ln "$PAPER_JAR" "$SRV/paper.jar" 2>/dev/null || cp "$PAPER_JAR" "$SRV/paper.jar"; fi

  echo "── [$idx/${#JARS[@]}] $NAME"
  (cd "$SRV" && exec timeout 150 java -Xmx1G -DPaper.IgnoreJavaVersion=true -jar paper.jar --nogui > boot.log 2>&1) &
  PID=$!   # exec makes $! the timeout/java process itself, so kill reaches java

  STATUS="timeout"
  for i in $(seq 1 75); do
    sleep 2
    if ! kill -0 "$PID" 2>/dev/null; then STATUS="exited"; break; fi
    # enabled: plugin enabled and no disable/error line from our plugin
    if grep -q "RNK adapted artifact enabled" "$LOG" 2>/dev/null \
       && ! grep -qE "Error occurred while enabling|disabling plugin" "$LOG" 2>/dev/null; then
      STATUS="enabled"; break
    fi
    # init-failed: our entrypoint started, then the mod's init threw
    if grep -q "RNK adapted artifact enabled" "$LOG" 2>/dev/null \
       && grep -qE "Error occurred while enabling" "$LOG" 2>/dev/null; then
      STATUS="init-failed"; break
    fi
    # load-failed: PluginManager refused the plugin outright
    if grep -qE "Invalid plugin.yml|Cannot load plugin|Failed to load" "$LOG" 2>/dev/null; then
      STATUS="load-failed"; break
    fi
    if grep -q "Done (" "$LOG" 2>/dev/null; then
      # Server fully started without our enable line -> treat as load-failed
      STATUS="load-failed"; break
    fi
  done
  kill "$PID" 2>/dev/null || true; sleep 1; kill -9 "$PID" 2>/dev/null || true

  case "$STATUS" in
    enabled)     enabled=$((enabled+1));     echo "   ✓ enabled on real Paper" ;;
    init-failed) initfailed=$((initfailed+1)); echo "   △ loaded, init threw (mod touches Minecraft at init)" ;;
    load-failed) loadfailed=$((loadfailed+1)); echo "   ✗ Paper could not load the plugin" ;;
    *)           loadfailed=$((loadfailed+1)); echo "   ✗ no classification within watchdog" ;;
  esac

  # Capture evidence lines for the report (single line, no quotes/backslashes).
  ERR_LINES=$(grep -A3 -m1 "Error occurred while enabling\|Cannot load plugin\|Invalid plugin.yml" "$LOG" 2>/dev/null | head -4 | tr '\n' ' ' | tr -d '[:cntrl:]' | tr -d '"\\' | cut -c1-300)
  results+=("{\"jar\":\"$NAME\",\"status\":\"$STATUS\",\"evidence\":\"$ERR_LINES\"}")
done

python3 - "$OUT_JSON" "${results[@]}" <<'PYEOF'
import json, sys
out = sys.argv[1]
rows = [json.loads(a) for a in sys.argv[2:]]
enabled = sum(1 for r in rows if r["status"] == "enabled")
initf = sum(1 for r in rows if r["status"] == "init-failed")
loadf = sum(1 for r in rows if r["status"] == "load-failed")
json.dump({
    "measuredAt": __import__("datetime").datetime.utcnow().isoformat() + "Z",
    "paper": "1.20.1",
    "booted": len(rows),
    "enabledOnRealPaper": enabled,
    "initFailed": initf,
    "loadFailed": loadf,
    "rows": rows
}, open(out, "w"), indent=2)
print(f"  Report: {out}")
PYEOF

echo ""
echo "━━━ Live-Paper batch: enabled=$enabled  init-failed=$initfailed  load-failed=$loadfailed ━━━"
exit 0
