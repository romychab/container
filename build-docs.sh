#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Activate virtual environment with mkdocs if available
if [ -f "$SCRIPT_DIR/.venv/bin/activate" ]; then
    source "$SCRIPT_DIR/.venv/bin/activate"
fi

MKDOCS_DOCS_DIR="build/mkdocs-docs"

# Clean previous generated docs
rm -rf "$MKDOCS_DOCS_DIR"
mkdir -p "$MKDOCS_DOCS_DIR"

# Copy doc pages, fixing TOC anchors for MkDocs compatibility:
# MkDocs uses _1 (not -1) for duplicate heading anchors,
# and drops slashes differently than GitHub markdown.
cp docs/container-type.md "$MKDOCS_DOCS_DIR/container-type.md"

sed 's|(#from-a-plain-flow-1)|(#from-a-plain-flow_1)|g
s|(#from-a-flow-of-containers-1)|(#from-a-flow-of-containers_1)|g
s|(#public-interface--private-implementation-pattern)|(#public-interface-private-implementation-pattern)|g' \
    docs/reducer-pattern.md > "$MKDOCS_DOCS_DIR/reducer-pattern.md"

sed 's|(#basic-usage-1)|(#basic-usage_1)|g' \
    docs/subjects.md > "$MKDOCS_DOCS_DIR/subjects.md"

# Copy README as index page:
# - fix links from docs/X.md to X.md
# - redirect LICENSE link to GitHub
# - remove "Documentation" section (redundant on the site itself)
sed 's|(docs/container-type.md)|(container-type.md)|g
s|(docs/reducer-pattern.md)|(reducer-pattern.md)|g
s|(docs/subjects.md)|(subjects.md)|g
s|(LICENSE)|(https://github.com/romychab/container/blob/main/LICENSE)|g
/^## Documentation$/,/^## /{/^## Documentation$/d;/^## /!d;}' README.MD > "$MKDOCS_DOCS_DIR/index.md"

# Build the site
mkdocs build

echo ""
echo "Documentation site built successfully in build/mkdocs/"
echo "To preview locally: mkdocs serve"
