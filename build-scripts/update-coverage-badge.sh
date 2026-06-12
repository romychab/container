#!/usr/bin/env bash
set -euo pipefail

# Usage: bash build-scripts/update-coverage-badge.sh [module]
#   module: Gradle module whose Kover report is published (default: container)
#
# Reads the Kover XML report for the module and updates the coverage badge
# by writing a Shields.io endpoint JSON ("<module>-coverage.json") to a
# GitHub Gist.
#
# Required environment variables:
#   GIST_TOKEN  - GitHub personal access token with the 'gist' scope
#   GIST_ID     - ID of the Gist containing the coverage JSON files

MODULE="${1:-container}"

export KOVER_MODULE="$MODULE"
# shellcheck source=kover-coverage.sh
source "$(dirname "$0")/kover-coverage.sh"

if [ "$PCT_INT" -ge 80 ]; then COLOR="brightgreen"
elif [ "$PCT_INT" -ge 60 ]; then COLOR="yellow"
elif [ "$PCT_INT" -ge 40 ]; then COLOR="orange"
else COLOR="red"
fi

echo "Updating coverage badge for '$MODULE': ${PCT_INT}% (${COLOR})"

COVERAGE_JSON=$(printf '{"schemaVersion":1,"label":"Coverage","message":"%s%%","color":"%s"}' "$PCT_INT" "$COLOR")
PAYLOAD=$(jq -n --arg content "$COVERAGE_JSON" --arg file "${MODULE}-coverage.json" \
    '{"files":{($file):{"content":$content}}}')

curl -s -X PATCH \
    -H "Authorization: token $GIST_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD" \
    "https://api.github.com/gists/$GIST_ID"
