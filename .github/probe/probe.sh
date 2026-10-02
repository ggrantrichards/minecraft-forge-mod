#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
J=$(find .gradle/mavenizer/repo -name 'client-extra*.jar' | head -1)
unzip -p "$J" assets/minecraft/textures/trims/entity/humanoid/eye.png.mcmeta; echo
unzip -p "$J" assets/minecraft/textures/trims/entity/humanoid_leggings/eye.png.mcmeta; echo
unzip -l "$J" | grep -E "trims/entity/" | awk '{print $4}' | grep -v mcmeta | sed 's|.*/entity/||' | tr '\n' ' '; echo
unzip -l "$J" | grep -E "trims/items/|trims/" | awk '{print $4}' | grep -v "entity" | head -20
for f in assets/minecraft/textures/trims/entity/humanoid/eye.png assets/minecraft/textures/trims/entity/humanoid_leggings/eye.png assets/minecraft/textures/palettes/trim_base.png assets/minecraft/textures/entity/banner/flow.png assets/minecraft/textures/item/flow_banner_pattern.png assets/minecraft/textures/item/eye_armor_trim_smithing_template.png; do
  echo "B64 $f"; unzip -p "$J" "$f" | base64 -w0; echo
done
unzip -l "$J" | grep -E "textures/entity/(banner|shield)/" | head -5
exit 0
