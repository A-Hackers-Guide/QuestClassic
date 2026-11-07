# GitHub achievements

GitHub awards badges for certain actions. This repo is structured so you can earn them naturally while building. All of them are legit (not "sock-puppet" farms — those get deleted).

| Badge | What it needs | How we earn it on this repo |
|---|---|---|
| **Quickdraw** | Close an issue or PR within 5 minutes of opening it | Open a trivial typo-fix issue, land the fix PR, close the issue in the PR body with `Closes #N`. |
| **YOLO** | Merge a PR without review | Any of your own PRs, merge directly on the main branch. Fine on a solo repo. |
| **Pull Shark** | Merge 2 pull requests | Land the "widget stubs → widget impls" PRs listed in `widget_sdk.md`. Each one is a natural small PR. |
| **Pair Extraordinaire** | Co-author at least 1 commit | Any commit with a `Co-Authored-By: <email>` trailer. We've added one at the bottom of every CI release commit. Your own co-author commits also count. |
| **Galaxy Brain** | 2 accepted answers on Discussions | Enable Discussions on the repo. Answer any user question with "Answer" marked. Takes real traffic; land it naturally. |
| **Starstruck** | 16 / 128 / 512 / 4096 stars | Depends on reach. The 16-star tier is very achievable once the repo is up. |
| **Heart on Your Sleeve** | 10 reactions on things you've created | React with 👍 / ❤️ to your own comments/issues. |

## Suggested first-day sequence to earn 4 badges

1. Push the initial repo → auto release of `v0.1.0` tag → **Pull Shark #1 (via main-branch commits counted as implicit PRs)**. (Note: Pull Shark counts _merged PRs_, so…)
2. Open PR #1: "fix typo in README". Merge without review → **YOLO**.
3. Open an issue "stub Free memory widget", link PR #1's trivial follow-up, close within 5 min → **Quickdraw**.
4. Any CI commit already carries `Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>` if generated via this collaboration → **Pair Extraordinaire**.
5. Merge the Free-memory widget PR → **Pull Shark** (2 PRs).

## What we don't do

- Create fake accounts or coordinate star-trading rings. GitHub detects and penalizes both.
- Spam issues/PRs for the sake of counting. Each PR in this repo does real work (a widget, a buzz pattern, a backend).
- Automate reactions — `Heart on Your Sleeve` is nice but it's not something the repo structure should chase at the expense of readability.
