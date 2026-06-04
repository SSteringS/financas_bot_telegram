---
name: Worktree branch collision — backend switches branches mid-implementation
description: Backend agent switches git branches while FE agent is implementing, causing commits to land on wrong branch
type: feedback
---

The backend agent and frontend agent share the same git worktree (`C:\Users\satya\src\financas_bot_telegram`). The backend agent switches branches to implement its tasks, which changes `HEAD` while the FE agent is working.

**Why:** Single worktree shared between BE and FE instances. When BE checks out a different branch, the next `git add && git commit` from FE lands on BE's branch.

**How to apply:**
1. Always run `git branch --show-current` before staging/committing to verify you're on the correct FE branch.
2. If commits landed on the wrong branch, recover with:
   ```bash
   git cherry-pick <commit-hash>   # on the correct FE branch
   git reset --hard <prev-hash>    # on the wrong BE branch
   ```
3. When staging files with `git add`, immediately check `git status` to confirm the branch.
4. If staging and the branch changed mid-work, use `git stash` to save changes, switch branches, then `git stash pop`.
5. When `gh pr merge --squash` fails due to BE agent's dirty working tree, use the GitHub API directly:
   ```bash
   gh api repos/SSteringS/financas_bot_telegram/pulls/{PR}/merge --method PUT --field merge_method=squash --field commit_title="..."
   ```
