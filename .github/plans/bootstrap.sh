#!/usr/bin/env bash
# QuestClassic — repo bootstrap.
#
# Usage:
#   cd QuestClassic
#   bash .github/plans/bootstrap.sh
#
# What this does (interactively — reads from .github/plans/{prs,issues}/):
#   1. Creates the public GitHub repo A-Hackers-Guide/QuestClassic.
#   2. Pushes main.
#   3. Pushes every feature branch and opens its PR with the pre-written body.
#   4. Merges each PR into main via --squash (YOLO + Pull Shark).
#   5. Opens the planned issues.
#   6. Tags v0.1.0 to trigger the release workflow.
#
# Pre-flight assumed:
#   - gh is installed and `gh auth status` shows you logged in as A-Hackers-Guide.
#   - You're inside the QuestClassic repo (this script's cwd needs to be the repo root).
#   - You've committed and are on main.
set -euo pipefail

OWNER="A-Hackers-Guide"
REPO="QuestClassic"

step() { printf "\n\033[36m›› %s\033[0m\n" "$*"; }

[[ "$(basename "$PWD")" == "$REPO" ]] || { echo "run from the $REPO repo root"; exit 2; }
command -v gh >/dev/null || { echo "install gh first"; exit 2; }
gh auth status >/dev/null || { echo "gh auth login first"; exit 2; }

step "1/6  Creating public repo $OWNER/$REPO and pushing main"
gh repo create "$OWNER/$REPO" --public \
  --description "Sideloadable Quest 2+ companion: nostalgic launcher, buzz-pattern diagnostics, programmable AI, build-your-own HUD, AR semicircle menu, self-update channel." \
  --source . --remote origin --push

# PR template: branch name → pre-written body file
declare -a PRS=(
  "feat/signature-verifier|01-signature-verifier.md|feat: SignatureVerifier panel — verify a Meta OTA .zip on-device"
  "feat/system-internals|02-system-internals.md|feat: SystemInternals panel — surface RE findings live on-device"
  "feat/widget-implementations|03-widget-implementations.md|feat: implement the four stub widgets"
  "feat/meta-hosts-audit|04-meta-hosts-audit.md|feat: MetaHostsAudit panel — resolve + ping every Meta host, categorized"
)

step "2/6  Pushing feature branches"
for row in "${PRS[@]}"; do
  IFS='|' read -r branch _body _title <<< "$row"
  git push -u origin "$branch"
done

step "3/6  Opening PRs"
declare -a PR_NUMS=()
for row in "${PRS[@]}"; do
  IFS='|' read -r branch body title <<< "$row"
  url=$(gh pr create --base main --head "$branch" \
    --title "$title" \
    --body-file ".github/plans/prs/$body")
  num="${url##*/}"
  PR_NUMS+=("$num")
  echo "  opened PR #$num ($branch) → $url"
done

step "4/6  Merging PRs (squash, one at a time; CI will rebuild main each time)"
for n in "${PR_NUMS[@]}"; do
  gh pr merge "$n" --squash --delete-branch --admin
  echo "  merged PR #$n"
done
git fetch origin
git checkout main
git pull --ff-only

step "5/6  Opening issues"
for f in .github/plans/issues/*.md; do
  title=$(awk -F': "?' '/^title:/{sub(/"? *$/,"",$2); print $2}' "$f")
  labels=$(awk -F': *' '/^labels:/{gsub(/[\[\]]/,"",$2); print $2}' "$f")
  body=$(awk '/^---$/{c++; next} c>=2' "$f")
  if [[ -n "$labels" ]]; then
    echo "$body" | gh issue create --title "$title" --label "$(echo "$labels" | sed 's/, */,/g')" --body-file -
  else
    echo "$body" | gh issue create --title "$title" --body-file -
  fi
done

step "6/6  Tagging v0.1.0 to trigger the release workflow"
git tag v0.1.0 -m "QuestClassic v0.1.0 — initial public release"
git push origin v0.1.0

echo
echo "Done. View the repo: gh repo view $OWNER/$REPO --web"
