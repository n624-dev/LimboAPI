#!/bin/sh
set -eu
if [ "$#" -ne 2 ]; then
  echo 'Usage: validation/build.sh /absolute/path/velocity.jar /absolute/path/limboapi.jar' >&2
  exit 2
fi
task_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
task_output="$task_root/build/validation"
mkdir -p "$task_output"
javac --release 25 -cp "$1:$2" -processorpath "$1" -d "$task_output" \
  "$task_root/validation/src/main/java/jp/n624/limbo/validation/SmokePlugin.java"
jar --create --file "$task_root/build/limboapi-validation.jar" -C "$task_output" .
