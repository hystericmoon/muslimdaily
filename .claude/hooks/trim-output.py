#!/usr/bin/env python3
"""PostToolUse hook: trim noisy Bash output (installs, builds, test runs).
Keeps every error-looking line from the head, plus the last 30 lines."""
import json, re, sys

TAIL = 30
MAX_ERR = 40
NOISY = re.compile(
    r"\b(npm|pnpm|yarn|bun)\s+(i|install|ci|add|run\s+(build|test)|test|build)\b"
    r"|\bpip3?\s+install\b|\buv\s+(pip|sync|add)\b|\bpoetry\s+install\b"
    r"|\bcargo\s+(build|test|check|install)\b|\bgo\s+(build|test|mod)\b"
    r"|\b(make|cmake|gradle|gradlew|mvn|tsc|webpack|vite\s+build|next\s+build)\b"
    r"|\b(pytest|jest|vitest|mocha|rspec|phpunit|playwright\s+test)\b"
    r"|\bdocker\s+(build|compose\s+build)\b|\bapt(-get)?\s+install\b|\bbrew\s+install\b")
ERR = re.compile(r"error|fail|exception|traceback|panic|fatal|✗|✕|ERR!", re.I)

def trim(text):
    lines = text.splitlines()
    if len(lines) <= TAIL:
        return text, False
    head, tail = lines[:-TAIL], lines[-TAIL:]
    errs = [l for l in head if ERR.search(l)][:MAX_ERR]
    note = f"[trim-output: hid {len(head) - len(errs)} of {len(lines)} lines; kept {len(errs)} error lines + last {TAIL}]"
    return "\n".join([note, *errs, "...", *tail]), True

def main():
    try:
        data = json.load(sys.stdin)
    except Exception:
        return
    if data.get("tool_name") != "Bash":
        return
    cmd = (data.get("tool_input") or {}).get("command", "")
    resp = data.get("tool_response")
    if not NOISY.search(cmd) or not isinstance(resp, dict):
        return
    out = dict(resp)
    changed = False
    for key in ("stdout", "stderr"):
        if isinstance(out.get(key), str):
            out[key], c = trim(out[key])
            changed |= c
    if changed:
        print(json.dumps({"hookSpecificOutput": {
            "hookEventName": "PostToolUse", "updatedToolOutput": out}}))

if __name__ == "__main__":
    main()
