#!/usr/bin/env bash
set -euo pipefail

REPOSITORY="Jahrome907/minecraft-agent-skills"
BRANCH="main"
SKILLS=(
  minecraft-modding
  minecraft-testing
  minecraft-resource-pack
  minecraft-ci-release
  minecraft-multiloader
)

usage() {
  cat <<'EOF'
Usage: ./scripts/update-agent-skills.sh --check|--update

  --check   Report whether the locked upstream commit is current. Writes nothing.
  --update  Review and install the five pinned Minecraft skills, then update the lock.
EOF
}

case "${1:-}" in
  --check|--update) MODE="$1" ;;
  --help|-h|"") usage; exit 0 ;;
  *) echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
esac
[[ $# -eq 1 ]] || { usage >&2; exit 2; }

for command in git curl tar python3; do
  command -v "$command" >/dev/null 2>&1 || {
    echo "Required command not found: $command" >&2
    exit 1
  }
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOCK_FILE="$PROJECT_ROOT/.agents/skills.lock.json"
INSTALL_DIR="$PROJECT_ROOT/.agents/skills"

current_commit="UNLOCKED"
if [[ -f "$LOCK_FILE" ]]; then
  current_commit="$(python3 - "$LOCK_FILE" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as handle:
    print(json.load(handle).get("source", {}).get("commit", "UNLOCKED"))
PY
)"
fi

latest_commit="$(git ls-remote "https://github.com/$REPOSITORY.git" "refs/heads/$BRANCH" | awk '{print $1}')"
[[ "$latest_commit" =~ ^[0-9a-f]{40}$ ]] || {
  echo "Could not resolve a full upstream commit SHA." >&2
  exit 1
}

echo "Current locked commit: $current_commit"
echo "Upstream main commit:  $latest_commit"
echo "Managed skill directories:"
printf '  - %s\n' "${SKILLS[@]}"

if [[ "$current_commit" == "$latest_commit" ]]; then
  echo "Skills are already current."
  exit 0
fi

if [[ "$MODE" == "--check" ]]; then
  echo "An update is available. No files were changed."
  exit 0
fi

tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/minecraft-agent-skills.XXXXXX")"
trap 'rm -rf -- "$tmp_dir"' EXIT INT TERM
archive="$tmp_dir/source.tar.gz"
source_root="$tmp_dir/minecraft-agent-skills-$latest_commit"

curl --retry 5 --retry-all-errors --retry-delay 2 -fsSL \
  "https://codeload.github.com/$REPOSITORY/tar.gz/$latest_commit" \
  -o "$archive"
tar -xzf "$archive" -C "$tmp_dir"

python3 - "$source_root/.agents/skills" "${SKILLS[@]}" <<'PY'
from pathlib import Path
import re
import sys

root = Path(sys.argv[1]).resolve()
skills = sys.argv[2:]
dangerous = {
    "SSH key access": ("/.ssh", "~/.ssh", "id_rsa", "id_ed25519"),
    "file upload or publishing": ("git push", "npm publish", "gh release", "scp ", "rsync "),
    "secret enumeration": ("printenv", "os.environ", "process.env"),
    "privileged or broad deletion": ("sudo ", "chown ", "rm -rf /", "rm -rf ~"),
    "unexplained network execution": ("curl ", "wget ", "fetch(", "requests."),
}

for expected in skills:
    directory = root / expected
    skill_file = directory / "SKILL.md"
    if not skill_file.is_file():
        raise SystemExit(f"Missing SKILL.md: {expected}")
    if any(path.is_symlink() for path in directory.rglob("*")):
        raise SystemExit(f"Symlinks are not allowed in managed skill: {expected}")
    text = skill_file.read_text(encoding="utf-8")
    match = re.match(r"^---\n(.*?)\n---\n", text, re.S)
    if not match:
        raise SystemExit(f"Invalid YAML front matter: {expected}")
    front = match.group(1)
    name = re.search(r"^name:\s*[\"']?([^\"'\n]+)", front, re.M)
    description = re.search(r"^description:\s*(.+)$", front, re.M)
    if not name or name.group(1).strip() != expected or not description or not description.group(1).strip():
        raise SystemExit(f"Invalid name or description: {expected}")
    for script in (directory / "scripts").glob("**/*") if (directory / "scripts").is_dir() else ():
        if not script.is_file() or "vendor" in script.parts:
            continue
        script_text = script.read_text(encoding="utf-8", errors="replace").lower()
        for label, signals in dangerous.items():
            if any(signal.lower() in script_text for signal in signals):
                raise SystemExit(f"Security review required ({label}): {script}")
print("Static safety and front-matter checks passed.")
PY

mkdir -p "$INSTALL_DIR"
for skill in "${SKILLS[@]}"; do
  source_dir="$source_root/.agents/skills/$skill"
  target_dir="$INSTALL_DIR/$skill"
  rm -rf -- "$target_dir"
  cp -R "$source_dir" "$target_dir"
done

retrieved_at="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
python3 - "$LOCK_FILE" "$latest_commit" "$retrieved_at" <<'PY'
import json, sys
from pathlib import Path

path = Path(sys.argv[1])
payload = {
    "schema_version": 1,
    "source": {
        "repository": "Jahrome907/minecraft-agent-skills",
        "branch": "main",
        "commit": sys.argv[2],
        "retrieved_at": sys.argv[3],
    },
    "install_scope": "project",
    "install_path": ".agents/skills",
    "skills": [
        "minecraft-modding",
        "minecraft-testing",
        "minecraft-resource-pack",
        "minecraft-ci-release",
        "minecraft-multiloader",
    ],
}
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8", newline="\n")
PY

echo "Updated five project skills to $latest_commit."
echo "No commit, push, Gradle task, or publishing action was performed."
