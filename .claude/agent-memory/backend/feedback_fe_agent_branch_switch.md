---
name: FE agent switches branches mid-work
description: FE agent switches HEAD while BE agent is working — git commits land on wrong branch; stash+checkout pattern to recover
type: feedback
---

When the FE agent is active in the same worktree, it switches branches without warning. This causes:
- Files created as untracked end up on the FE branch
- `git add` stages them to the FE branch index
- A `git commit` would put BE work into the FE branch

**Why:** Both agents share the single implementor worktree `C:\Users\satya\src\financas_bot_telegram`. The worktree has one `.git/HEAD` — whoever runs `git checkout` last wins.

**How to apply:**
1. Always run `git branch --show-current` before committing.
2. If on wrong branch: `git reset HEAD <files>` to unstage, then `git stash --include-untracked`, `git checkout feature/be-NNN-slug`, `git stash pop`.
3. This has happened at least twice (BE-026 commit, BE-029 staging). Expect it to recur.
4. Naming pattern matters: integration test classes must end in `IntegrationTest` (not `IT`) so Maven Surefire picks them up — Surefire doesn't run `*IT.java`.
