#!/bin/sh
# Maintainer packaging only; analysts use the supplied ZIP/JAR.
set -eu
repo=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
stage=$(mktemp -d)
trap 'rm -rf "$stage"' EXIT HUP INT TERM
cd "$repo/outputs"
zip -qr "$stage/copilot-mapping-poc.zip" copilot-mapping-poc \
  -x '*/history/*' '*/run.lock' '*/run-history.jsonl' '*/clarifications.json' \
     '*/build/*' '*/Test-results/*' '*/.DS_Store' '*.py' '*.pyc'
unzip -tq "$stage/copilot-mapping-poc.zip"
mv "$stage/copilot-mapping-poc.zip" copilot-mapping-poc.zip
