#!/usr/bin/env bash

project_dir=$PWD
set -Eeuxo pipefail
cd e2e/monorepo-support


if [[ ! -e $project_dir/.git ]]; then
  echo "Expected formatting-stack to be a Git repo"
  exit 1
fi

if [[ -e .git ]]; then
  echo "monorepo-support is meant to emulate monorepos. It should not have its own Git repository; that would alter formatting-stack output"
  exit 1
fi

mkdir checkouts
cd checkouts
ln -s $project_dir formatting-stack
cd ..
echo "(ns foo)" > src/foo.clj
echo "(ns bar)" > src/bar.clj
git add src/foo.clj
lein with-profile -dev do clean, test
git reset src/foo.clj
rm src/foo.clj
rm src/bar.clj
