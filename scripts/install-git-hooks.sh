#!/usr/bin/env bash
# Install Git hooks: auto-export DB snapshot before each commit
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

git config core.hooksPath .githooks
echo "[install-hooks] core.hooksPath = .githooks"

if [ -f ".githooks/pre-commit" ]; then
  chmod +x .githooks/pre-commit .githooks/pre-push 2>/dev/null || true
fi

echo "[install-hooks] done. Next commit will auto-export student_pro_snapshot.sql"
