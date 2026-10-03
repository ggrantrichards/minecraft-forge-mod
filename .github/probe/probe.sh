#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
JARS=$(find .gradle ~/.gradle -name '*.jar' 2>/dev/null | grep -iE '26.3|forge|eventbus' | grep -v sources)
NAMES="DensityFunction KeyDispatchDataCodec EnderMan Endermite NoteBlockEvent PlayerEvent LivingDeathEvent DustParticleOptions TemptGoal RandomSwimmingGoal AbstractTickableSoundInstance WeepingVinesBlock WeepingVinesPlantBlock HurtByTargetGoal NearestAttackableTargetGoal MeleeAttackGoal WaterAvoidingRandomStrollGoal ServerLevelAccessor PathType SinglePointContext ItemCombinerMenu LodestoneTracker DensityFunctions"
for j in $JARS; do
  L=$(unzip -l "$j" 2>/dev/null | awk '{print $4}' | grep '\.class$')
  for n in $NAMES; do echo "$L" | grep -E "/$n(\\\$[A-Za-z]+)?\.class$" | sed "s|^|$n: |"; done
done | sort -u
