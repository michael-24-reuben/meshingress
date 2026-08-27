#!/usr/bin/env bash
set -euo pipefail

repository=/home/alphasunny/Public/Nextcloud/data/remote.dev/me/git/meshingress.git
bundle_directory=/home/alphasunny/Public/Nextcloud/published/_mnt/remote.dev/me/git
bundle_path="$bundle_directory/meshingress.bundle"
temporary_bundle="$(mktemp "$repository/.meshingress.bundle.XXXXXX")"

cleanup() {
  rm -f "$temporary_bundle"
}
trap cleanup EXIT

mkdir -p "$bundle_directory"
git -C "$repository" bundle create "$temporary_bundle" --all
git -C "$repository" bundle verify "$temporary_bundle" >/dev/null
chmod 0644 "$temporary_bundle"
mv -f "$temporary_bundle" "$bundle_path"
trap - EXIT

printf 'Updated Nextcloud-published bundle: %s\n' "$bundle_path"
