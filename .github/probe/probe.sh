#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
J=$(find .gradle/mavenizer/repo -name 'client-extra*.jar' | head -1)
for f in textures/trims/color_palettes/trim_palette.png textures/trims/entity/humanoid/eye.png textures/trims/entity/humanoid_leggings/eye.png \
         textures/entity/banner/flow.png textures/entity/shield/flow.png textures/item/flow_banner_pattern.png textures/item/eye_armor_trim_smithing_template.png; do
  echo "B64 $f"; unzip -p "$J" "assets/minecraft/$f" | base64 -w0; echo
done
unzip -p "$J" assets/minecraft/atlases/armor_trims.json | head -c 3000; echo
unzip -l "$J" | grep -E "atlases/" | head -20
unzip -l "$J" | grep -E "trims/color_palettes" | head -20
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src && cd /tmp/src
grep -n "delayedComponent" net/minecraft/world/item/Item.java | head -3
grep -rn "smithing_trim\|SmithingTrimRecipe(" net/minecraft/world/item/crafting/SmithingTrimRecipe.java | head -5
grep -n "Codec\|fieldOf" net/minecraft/world/item/crafting/SmithingTrimRecipe.java | head -12
exit 0
