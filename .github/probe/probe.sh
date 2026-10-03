#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
J=$(find .gradle/mavenizer/repo -name 'forge-26.3-*.jar' ! -name '*sources*' | head -1)
echo "jar: $J"
JAVAP=$(dirname $(readlink -f $(which java)))/javap
for c in net.minecraft.world.level.levelgen.RandomState net.minecraft.server.level.ServerBossEvent net.minecraft.world.entity.InteractionHand net.minecraft.world.InteractionHand 'net.minecraft.world.entity.SwingAnimation' net.minecraft.world.level.biome.Climate\$Sampler; do
  echo "== $c"; $JAVAP -cp "$J" "$c" 2>&1 | head -60
done
echo "== SwingAnimation classes"; unzip -l "$J" | awk '{print $4}' | grep -i swinganim
echo "== SoundEvents CAT"; $JAVAP -cp "$J" net.minecraft.sounds.SoundEvents | grep -iE " CAT|CHEST_OPEN|WOODEN_DOOR|BELL_BLOCK|STONE_STEP|ITEM_PICKUP|SHEEP_AMB|EXPERIENCE_ORB|ZOMBIE_HORSE|NOTE_BLOCK_CHIME"
echo "== Entity hurtMarked/hurt"; $JAVAP -cp "$J" net.minecraft.world.entity.Entity | grep -iE "hurtmark|markHurt|hasImpulse|needsSync|velocity|push\("
echo "== LivingEntity attackAnim/slot/swing"; $JAVAP -p -cp "$J" net.minecraft.world.entity.LivingEntity | grep -iE "attackanim|swing|SlotFor|hurtAndBreak|EquipmentSlot "
echo "== ItemStack hurtAndBreak"; $JAVAP -cp "$J" net.minecraft.world.item.ItemStack | grep -iE "hurtAndBreak"
echo "== EquipmentSlot"; $JAVAP -cp "$J" net.minecraft.world.entity.EquipmentSlot | head -30
echo "== StructurePiece"; $JAVAP -p -cp "$J" net.minecraft.world.level.levelgen.structure.StructurePiece | grep -iE "createChest|postProcess|addAdditional|StructurePiece\("
echo "== EntityType create"; $JAVAP -cp "$J" net.minecraft.world.entity.EntityType | grep -E " create\(|spawn\("
echo "== Mob finalizeSpawn"; $JAVAP -cp "$J" net.minecraft.world.entity.Mob | grep -iE "finalizeSpawn|setPersistence"
echo "== WorldGenLevel"; $JAVAP -cp "$J" net.minecraft.world.level.ServerLevelAccessor | head -20
echo "== HumanoidRenderState / ArmedEntityRenderState attackTime"; for c in net.minecraft.client.renderer.entity.state.HumanoidRenderState net.minecraft.client.renderer.entity.state.LivingEntityRenderState net.minecraft.client.renderer.entity.state.ArmedEntityRenderState; do echo "-- $c"; $JAVAP -cp "$J" $c 2>&1 | grep -iE "attack|swing" ; done
echo "== HumanoidMobRenderer extract"; $JAVAP -cp "$J" net.minecraft.client.renderer.entity.HumanoidMobRenderer | head; $JAVAP -cp "$J" net.minecraft.client.renderer.entity.state.ArmedEntityRenderState | head -40
