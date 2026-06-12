#!/usr/bin/env bash
set -euo pipefail

# Usage: bash build-scripts/build-docs.sh [container|store]
# Builds the MkDocs site for the given project (default: container).

PROJECT="${1:-container}"

case "$PROJECT" in
    container)
        README="README.md"
        DOCS_DIR="docs"
        STAGING_DIR="build/mkdocs-src"
        CONFIG="mkdocs.yml"
        SITE_DIR="build/mkdocs-container"
        ;;
    store)
        README="store/README.md"
        DOCS_DIR="store/docs"
        STAGING_DIR="build/mkdocs-store-src"
        CONFIG="mkdocs-store.yml"
        SITE_DIR="build/mkdocs-store"
        ;;
    *)
        echo "Unknown project '$PROJECT'. Expected 'container' or 'store'." >&2
        exit 1
        ;;
esac

# resolve mkdocs: prefer local venv, fall back to PATH
if [ -x ".venv/bin/mkdocs" ]; then
    MKDOCS=".venv/bin/mkdocs"
elif command -v mkdocs &>/dev/null; then
    MKDOCS="mkdocs"
else
    echo "MkDocs not found. Install it with:"
    echo "  python3 -m venv .venv && .venv/bin/pip install mkdocs"
    exit 1
fi

# prepare staging directory
rm -rf "$STAGING_DIR"
mkdir -p "$STAGING_DIR"

# Copy README.md as index.md.
# README links to docs files using the 'docs/' prefix (e.g., docs/installation.md).
# In the staging flat structure all files are at the same level, so strip that prefix.
sed '/<!-- docs-exclude-start -->/,/<!-- docs-exclude-end -->/d' "$README" \
    | sed 's|(\(docs/\)|(|g' > "$STAGING_DIR/index.md"

# Copy all docs pages into the staging directory (flat, no subdirectory).
# llm.md is a stub pointing to the agent skill in the repo - not a site page.
find "$DOCS_DIR" -maxdepth 1 -name '*.md' ! -name 'llm.md' \
    -exec cp {} "$STAGING_DIR/" \;

# The store docs link to files of the parent Container project. Those relative
# links cannot work on the standalone store site - rewrite them to absolute URLs.
if [ "$PROJECT" = "store" ]; then
    sed -i.bak \
        -e 's|(\.\./README\.md)|(https://docs.uandcode.com/container/)|g' \
        -e 's|(\.\./LICENSE)|(https://github.com/romychab/container/blob/main/LICENSE)|g' \
        -e 's|(\.\./skills/container-store/*)|(https://github.com/romychab/container/tree/main/skills/container-store)|g' \
        -e 's|(\.\./\.\./docs/reducer-pattern\.md)|(https://docs.uandcode.com/container/reducer-pattern/)|g' \
        "$STAGING_DIR"/*.md
    rm -f "$STAGING_DIR"/*.bak
fi

# Convert <!-- block-start: {type} --> ... <!-- block-end --> to MkDocs admonitions
python3 - "$STAGING_DIR" <<'EOF'
import re, sys
from pathlib import Path

def transform(m):
    block_type = m.group(1)
    body = m.group(2).strip('\n')
    indented = '\n'.join('    ' + line for line in body.split('\n'))
    return f'!!! {block_type}\n{indented}'

for path in Path(sys.argv[1]).glob('*.md'):
    content = path.read_text()
    updated = re.sub(
        r'<!-- block-start: (\S+) -->\n(.*?)<!-- block-end -->',
        transform,
        content,
        flags=re.DOTALL,
    )
    if updated != content:
        path.write_text(updated)
EOF

# build the site
$MKDOCS build --config-file "$CONFIG"

echo ""
echo "Documentation built successfully -> $SITE_DIR/index.html"
echo "To preview locally: mkdocs serve --config-file $CONFIG"
