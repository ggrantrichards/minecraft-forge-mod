#!/usr/bin/env bash
# Temporary: vanilla mob models and textures, as a reference for the mod's own designs.
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
ASSETS=$(find .gradle/mavenizer/repo/net/minecraft/client-extra -name '*.jar' | head -1)
mkdir -p /tmp/src /tmp/assets && unzip -qo "$SRC" -d /tmp/src && unzip -qo "$ASSETS" -d /tmp/assets
cd /tmp/src
for m in WardenModel EndermanModel CreakingModel IronGolemModel IllagerModel WitchModel VillagerModel BreezeModel; do
  f=$(find . -name "$m.java" | head -1); echo "=== MODEL $m $f"
  awk '/createBodyLayer|createMesh|LayerDefinition create/{p=1} p{print} /return LayerDefinition/{if(p){exit}}' "$f" | head -120
done
T=/tmp/assets/assets/minecraft/textures/entity
for t in warden/warden.png warden/warden_bioluminescent_layer.png creaking/creaking.png creaking/creaking_eyes.png enderman/enderman.png iron_golem/iron_golem.png illager/evoker.png illager/illusioner.png witch.png witch/witch.png wandering_trader.png villager/wandering_trader.png; do
  if [ -f "$T/$t" ]; then echo "=== PNG $t"; base64 -w 0 "$T/$t"; echo; fi
done
ls $T | tr '\n' ' '
exit 1
