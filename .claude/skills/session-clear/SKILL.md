---
name: session-clear
description: Wrap up an Adwam session when the owner says they are clearing it, or when the conversation is getting long. Updates and merges docs/handover.md, then replies "Safe to clear" with the next model and prompt.
---

# Clearing a session

The handover is the only backup: anything not merged to main is lost when the owner clears.

1. Update `docs/handover.md`: open PRs, what waits on the owner, decisions, next work with its
   model. Keep it under ~6 KB: move finished items, release notes and hard-won facts to
   `docs/history.md` (right section, newest first). Update "Last updated" and the session id
   under "Sessions" (`get_session` gives it).
2. Commit, push, open a PR (or add it to the open one), wait for CI, merge (standing approval
   unless a risk in CLAUDE.md, "Workflow", applies).
3. Reply with exactly:
   - "Safe to clear"
   - the next session's model, with a one-line reason (CLAUDE.md, "Sessions": Opus for religious
     content, audio, user data, cross-module or unknown-cause work; Sonnet for the rest)
   - its prompt, starting "Continue from docs/handover.md." plus the task.

Don't start or archive sessions yourself.
