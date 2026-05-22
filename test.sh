#!/bin/bash
# Run all tests for the shareholder-list-team2 project
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== Running backend tests ==="
cd "$SCRIPT_DIR/backend"
mvn test

echo ""
echo "=== All tests passed ==="

