#!/usr/bin/env bash
set -euo pipefail

data_root=/home/alphasunny/Public/Nextcloud/data/remote.dev/me
published_root=/home/alphasunny/Public/Nextcloud/published/_mnt/remote.dev/me
snapshot_git="$data_root/git/meshingress-snapshot.git"
snapshot="$published_root/meshingress"
archive=/tmp/meshingress-xps-snapshot.zip
incoming="$(mktemp -d "$data_root/.meshingress-incoming.XXXXXX")"

cleanup() {
  rm -rf "$incoming" "$archive"
}
trap cleanup EXIT

mkdir -p "$snapshot" "$data_root/git"
unzip -qq "$archive" -d "$incoming"
if [ ! -d "$snapshot_git" ]; then
  git init --bare --initial-branch=xps-snapshot "$snapshot_git"
  git --git-dir="$snapshot_git" config core.bare false
  git --git-dir="$snapshot_git" config core.worktree "$snapshot"
  git --git-dir="$snapshot_git" config user.name 'XPS Snapshot'
  git --git-dir="$snapshot_git" config user.email 'xps-snapshot@localhost'
  git --git-dir="$snapshot_git" config gc.pruneExpire never
fi
rsync -a --delete "$incoming/" "$snapshot/"
find "$snapshot" -type d -exec chmod a+rx {} +
find "$snapshot" -type f -exec chmod a+r {} +
git --git-dir="$snapshot_git" --work-tree="$snapshot" add -A
if ! git --git-dir="$snapshot_git" --work-tree="$snapshot" diff --cached --quiet; then
  git --git-dir="$snapshot_git" --work-tree="$snapshot" commit -m "XPS snapshot $(date -Is)"
fi
git --git-dir="$snapshot_git" rev-parse HEAD
