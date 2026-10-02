#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
J=$(find .gradle/mavenizer/repo -name 'client-extra*.jar' | head -1)
unzip -l "$J" | grep -i -E "trim" | grep -v -E "item/.*_trim\.json|models/item" | head -40
for a in banner_patterns shield_patterns items blocks; do echo "== atlas $a"; unzip -p "$J" assets/minecraft/atlases/$a.json; echo; done
unzip -l "$J" | grep -i -E "entity/banner/(flow|base)|banner_base|shield/flow|textures/entity/shield" | head
for f in $(unzip -l "$J" | grep -o -E "assets/minecraft/textures/trims/[^ ]*(palette|eye)[^ ]*\.png" | head -6) assets/minecraft/textures/entity/banner/flow.png assets/minecraft/textures/entity/shield/flow.png; do
  echo "B64 $f"; unzip -p "$J" "$f" | base64 -w0; echo
done
exit 0
