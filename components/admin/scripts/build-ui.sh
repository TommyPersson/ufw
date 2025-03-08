#!/usr/bin/env bash

set -euo pipefail

pushd src/ui

npm install
npm run build

popd

TARGET_DIR="src/generated/resources/static-files"

mkdir -p $TARGET_DIR

cp -r src/ui/dist/index.html $TARGET_DIR
cp -r src/ui/dist/admin/ui/assets $TARGET_DIR
