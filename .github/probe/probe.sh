#!/usr/bin/env bash
# Temporary: prints Minecraft/Forge 26.3 source and vanilla assets that Phase 4 (mobs) depends on.
set +e
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
ASSETS=$(find .gradle/mavenizer/repo/net/minecraft/client-extra -name '*.jar' | head -1)
echo "SRC=$SRC"
mkdir -p /tmp/src /tmp/assets && unzip -qo "$SRC" -d /tmp/src && unzip -qo "$ASSETS" -d /tmp/assets
cd /tmp/src
show() { f=$(find . -path "*$1" | head -1); echo "=== FILE $f"; grep -v '^import' "$f" | grep -v '^\s*$'; echo "=== END $1"; }
sig() { f=$(find . -path "*$1" | head -1); echo "=== SIG $f"; grep -n -E '^\s*(public|protected|static|abstract|default|private static final)[^=]*\(|class |interface |record ' "$f" | head -${2:-80}; }
# ---- entity basics
sig /world/entity/EntityType.java 60
grep -n -E ' (BEE|ENDERMITE|VEX|ALLAY|PHANTOM|GHAST|HAPPY_GHAST|SHULKER|ARMADILLO) = register' -A6 net/minecraft/world/entity/EntityType.java
grep -n -E 'class Builder' -A80 net/minecraft/world/entity/EntityType.java | grep -E 'public' | head -40
sig /world/entity/Mob.java 140
sig /world/entity/PathfinderMob.java
sig /world/entity/animal/Animal.java 60
sig /world/entity/AgeableMob.java 40
sig /world/entity/LivingEntity.java 400 | grep -E 'hurtServer|createLivingAttributes|aiStep|tick\(|die\(|dropCustomDeathLoot|isInvulnerableTo|getAmbientSound|getHurtSound|getDeathSound|playStepSound|causeFallDamage|checkFallDamage|isPushable|doPush|travel|actuallyHurt|canBeSeenAsEnemy|isSensitiveToWater|fireImmune|addAdditionalSaveData|readAdditionalSaveData|knockback|defineSynchedData|spawnAtLocation|hasLineOfSight|setNoGravity|isNoGravity|getGravity|onSyncedDataUpdated|shouldDropLoot|removeWhenFarAway|canBeLeashed'
sig /world/entity/Entity.java 500 | grep -E 'addAdditionalSaveData|readAdditionalSaveData|defineSynchedData|spawnAtLocation|setNoGravity|isNoGravity|fireImmune|isInvulnerableTo|hurtServer|getBoundingBox|isInWaterOrRain|isInWater\(|playSound|level\(\)|teleportTo|randomTeleport|makeSound|dampensVibrations|isCurrentlyGlowing|isInvisible|discard|getTeamColor|isSilent'
show /world/entity/monster/Endermite.java
show /world/entity/monster/Vex.java
show /world/entity/animal/allay/Allay.java | head -200
sig /world/entity/animal/Bee.java 200 | head -120
grep -n -B2 -A30 'class BeeWanderGoal' net/minecraft/world/entity/animal/Bee.java | head -60
grep -n -A12 'public static AttributeSupplier.Builder createAttributes' net/minecraft/world/entity/animal/Bee.java net/minecraft/world/entity/animal/Chicken.java net/minecraft/world/entity/monster/Phantom.java net/minecraft/world/entity/monster/Ghast.java
grep -n -B3 -A20 'eggTime' net/minecraft/world/entity/animal/Chicken.java | head -60
show /world/entity/animal/sheep/Sheep.java | grep -n -E 'shear|Shear|readyForShearing|mobInteract' | head -30
show /world/entity/Shearable.java
grep -rn -E 'interface IForgeShearable' -A30 net/minecraftforge | head -40
sig /world/entity/ai/goal/Goal.java
sig /world/entity/ai/goal/WaterAvoidingRandomFlyingGoal.java
sig /world/entity/ai/goal/MeleeAttackGoal.java
sig /world/entity/ai/goal/target/NearestAttackableTargetGoal.java
sig /world/entity/ai/goal/target/HurtByTargetGoal.java
sig /world/entity/ai/goal/TemptGoal.java
sig /world/entity/ai/goal/BreedGoal.java
sig /world/entity/ai/goal/LookAtPlayerGoal.java
sig /world/entity/ai/goal/RandomLookAroundGoal.java
sig /world/entity/ai/goal/WaterAvoidingRandomStrollGoal.java
sig /world/entity/ai/goal/AvoidEntityGoal.java
sig /world/entity/ai/control/FlyingMoveControl.java
sig /world/entity/ai/navigation/FlyingPathNavigation.java
sig /world/entity/ai/control/MoveControl.java
sig /world/entity/SpawnPlacements.java
sig /world/entity/SpawnPlacementTypes.java
grep -n -E 'ENDERMAN|ENDERMITE|BEE|PHANTOM|SHULKER' net/minecraft/world/entity/SpawnPlacements.java
sig /world/entity/monster/Monster.java
sig /world/entity/FlyingMob.java
sig /world/entity/ambient/AmbientCreature.java
sig /world/entity/ai/attributes/Attributes.java 80 | head -5; grep -n 'public static final Holder<Attribute>' net/minecraft/world/entity/ai/attributes/Attributes.java | head -50
# ---- items
grep -n -E 'SPAWN_EGG = register' -A2 net/minecraft/world/item/Items.java | head -12
sig /world/item/SpawnEggItem.java
grep -n -E 'spawnEgg|SpawnEgg' net/minecraft/world/item/Item.java | head
# ---- saving
sig /world/level/storage/ValueOutput.java
sig /world/level/storage/ValueInput.java
sig /network/syncher/SynchedEntityData.java
sig /network/syncher/EntityDataSerializers.java 10; grep -n 'public static final EntityDataSerializer<' net/minecraft/network/syncher/EntityDataSerializers.java | head -12
# ---- client rendering
show /client/model/EndermiteModel.java
show /client/renderer/entity/EndermiteRenderer.java
show /client/model/VexModel.java
show /client/renderer/entity/VexRenderer.java
show /client/renderer/entity/state/VexRenderState.java
show /client/renderer/entity/state/LivingEntityRenderState.java | head -60
sig /client/renderer/entity/state/EntityRenderState.java
sig /client/renderer/entity/MobRenderer.java
sig /client/renderer/entity/LivingEntityRenderer.java 80
sig /client/renderer/entity/EntityRenderer.java 80
sig /client/model/EntityModel.java
sig /client/model/Model.java
sig /client/model/geom/ModelPart.java 60
sig /client/model/geom/builders/CubeListBuilder.java
sig /client/model/geom/builders/PartDefinition.java
sig /client/model/geom/PartPose.java
sig /client/model/geom/builders/LayerDefinition.java
sig /client/model/geom/builders/CubeDeformation.java
sig /client/model/geom/ModelLayerLocation.java
grep -rn -E 'class (RegisterRenderers|RegisterLayerDefinitions|AddLayers)' -A20 net/minecraftforge/client/event/EntityRenderersEvent.java | grep -E 'class|public|BUS' | head -30
f=$(find net/minecraftforge -name 'EntityAttributeCreationEvent.java'); grep -v '^import' $f | grep -E 'public|BUS|class'
f=$(find net/minecraftforge -name 'SpawnPlacementRegisterEvent.java'); grep -v '^import' $f | grep -E 'public|BUS|class|enum'
grep -rn -E 'static .* (entityTranslucent|entityCutoutNoCull|eyes|entityCutout)\(' net/minecraft/client/renderer/ | head
show /client/renderer/entity/layers/EyesLayer.java
show /client/renderer/entity/layers/PhantomEyesLayer.java
show /client/renderer/entity/AllayRenderer.java
grep -n -E 'getBlockLightLevel|getSkyLightLevel|shouldShowName|getShadowRadius|scale\(' net/minecraft/client/renderer/entity/*Renderer.java | head -20
show /client/renderer/entity/GhastRenderer.java
show /client/model/GhastModel.java | head -60
grep -rn 'KeyframeAnimation\|AnimationState' net/minecraft/client/model/BreezeModel.java | head -5
sig /world/entity/AnimationState.java
# ---- assets
A=/tmp/assets/assets/minecraft; D=/tmp/assets/data/minecraft
c() { echo "=== DATA $1"; tr -s ' \n' ' ' < "$1" 2>/dev/null | head -c ${2:-2500} || echo MISSING; echo; }
c $A/items/bee_spawn_egg.json; c $A/models/item/bee_spawn_egg.json; ls $A/textures/item | grep spawn_egg | head -5
ls $A/textures/entity | head -80 | tr '\n' ' '; echo
c $D/worldgen/biome/end_highlands.json 3000
c $D/loot_table/entities/endermite.json; c $D/loot_table/entities/phantom.json 1500
ls $D/tags/entity_type | tr '\n' ' '; echo
c $D/tags/entity_type/fall_damage_immune.json
grep -rn 'class SoundEvents' net/minecraft/sounds/SoundEvents.java | head -1; grep -n -E ' (AMETHYST_BLOCK_CHIME|ALLAY_AMBIENT_WITHOUT_ITEM|BEACON_AMBIENT|WARDEN_HEARTBEAT|ENDERMITE_AMBIENT|ENDERMITE_HURT|ENDERMITE_DEATH|PHANTOM_FLAP|PHANTOM_AMBIENT|ELDER_GUARDIAN_AMBIENT|SHULKER_AMBIENT|BEACON_POWER_SELECT|WARDEN_NEARBY_CLOSER|ALLAY_HURT|ALLAY_DEATH|SHEEP_SHEAR|BLAZE_SHOOT|GHAST_SHOOT|RESPAWN_ANCHOR_CHARGE|CONDUIT_AMBIENT) ' net/minecraft/sounds/SoundEvents.java
