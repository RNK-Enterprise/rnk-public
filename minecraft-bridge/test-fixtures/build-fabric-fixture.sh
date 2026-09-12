#!/bin/bash
# Builds the demo Fabric fixture mods (heartsync) into real Fabric mod JARs.
#
#   bash build-fabric-fixture.sh [output-dir]
#
# Builds both entrypoint variants:
#   - fabric-heartsync:        DedicatedServerModInitializer (server path)
#   - fabric-heartsync-client: ModInitializer (client/main path)
#
# Set BUILD_ONLY=1 to stop after the JARs are produced (the Bridge E2E uses
# this); without it, the script also runs the direct adapt + verify demo on
# the server fixture.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TYNC="$(cd "$SCRIPT_DIR/../../the-tync" && pwd)"
OUT="${1:-/tmp/crossloader-demo}"
FIXTURE="$SCRIPT_DIR/fabric-heartsync"
FIXTURE_CLIENT="$SCRIPT_DIR/fabric-heartsync-client"
mkdir -p "$OUT"

echo "── 1. Fabric API stub (faithful method shapes, from test-fixtures/fabric-stub)"
STUB=/tmp/fabric-api-stub
rm -rf "$STUB" && mkdir -p "$STUB"
cp -r "$SCRIPT_DIR/fabric-stub/net" "$STUB/net"
javac -d "$STUB" "$STUB/net/fabricmc/api/"*.java

echo "── 2a. Compile the SERVER fixture (DedicatedServerModInitializer) and package it"
mkdir -p /tmp/fabric-fixture-classes
javac -cp "$STUB" -d /tmp/fabric-fixture-classes "$FIXTURE/HeartSyncMod.java"
jar --create --file "$OUT/heartsync-fabric.jar" \
  --manifest="$FIXTURE/MANIFEST.MF" \
  -C /tmp/fabric-fixture-classes . \
  -C "$FIXTURE" fabric.mod.json
echo "   source JAR: $(stat -c%s "$OUT/heartsync-fabric.jar") bytes -> $OUT/heartsync-fabric.jar"

echo "── 2b. Compile the CLIENT fixture (ModInitializer) and package it"
mkdir -p /tmp/fabric-fixture-client-classes
javac -cp "$STUB" -d /tmp/fabric-fixture-client-classes "$FIXTURE_CLIENT/HeartSyncClientMod.java"
jar --create --file "$OUT/heartsync-client-fabric.jar" \
  --manifest="$FIXTURE_CLIENT/MANIFEST.MF" \
  -C /tmp/fabric-fixture-client-classes . \
  -C "$FIXTURE_CLIENT" fabric.mod.json
echo "   source JAR: $(stat -c%s "$OUT/heartsync-client-fabric.jar") bytes -> $OUT/heartsync-client-fabric.jar"

if [ "${BUILD_ONLY:-0}" = "1" ]; then
  exit 0
fi

echo "── 3. Compile the Tync adapter + verifier"
cd "$TYNC/main-app"
mkdir -p /tmp/tync-transform-classes
javac -cp "target/dependency/*" -d /tmp/tync-transform-classes \
  src/main/java/com/rnk/thetync/transform/CrossLoaderAdapter.java \
  src/main/java/com/rnk/thetync/transform/AdaptedArtifactVerifier.java

echo "── 4. ADAPT: Fabric → Paper bytecode rewriting (server fixture)"
java -cp "/tmp/tync-transform-classes:target/dependency/*" \
  com.rnk.thetync.transform.CrossLoaderAdapter \
  "$OUT/heartsync-fabric.jar" "$OUT/heartsync-adapted.jar"

echo "── 5. VERIFY: execute the adapted artifact"
java -cp "/tmp/tync-transform-classes:target/dependency/*" \
  com.rnk.thetync.transform.AdaptedArtifactVerifier \
  "$OUT/heartsync-adapted.jar"
echo "── verifier exit: $? (0 = adaptation proven executable)"
