#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
FJ=$(find .gradle/mavenizer/repo -name 'forge-26.3-*.jar' ! -name '*sources*' | head -1)
echo "jar: $FJ"
echo "== Forge client events"; unzip -l "$FJ" | grep -E "net/minecraftforge/client/event/[A-Za-z]+(\\\$[A-Za-z]+)?\.class" | awk '{print $4}' | head -150
echo "== Forge entity events"; unzip -l "$FJ" | grep -E "net/minecraftforge/event/entity/[A-Za-z/]+(\\\$[A-Za-z]+)?\.class" | awk '{print $4}' | head -120
for c in 'net.minecraftforge.client.event.ViewportEvent$ComputeCameraAngles' 'net.minecraftforge.event.entity.EntityEvent$Size' 'net.minecraftforge.client.event.CalculateDetachedCameraDistanceEvent' 'net.minecraftforge.client.event.RenderPlayerEvent$Pre' 'net.minecraftforge.client.event.MovementInputUpdateEvent' 'net.minecraftforge.client.event.ComputeFovModifierEvent'; do echo "== $c"; javap -cp "$FJ" "$c" 2>&1 | head -30; done
echo "== Camera"; javap -p -cp "$FJ" net.minecraft.client.Camera | head -80
echo "== Entity eye/dims"; javap -p -cp "$FJ" net.minecraft.world.entity.Entity | grep -i "eye\|dimension\|onGround\|verticalCollision\|setPos\b\|makeBoundingBox\|maxUpStep\|collide" | head -60
echo "== LivingEntity travel/jump"; javap -p -cp "$FJ" net.minecraft.world.entity.LivingEntity | grep -i "travel\|jump\|friction\|getDimensions\|getDefaultDimensions\|gravity\|upside" | head -40
echo "== Player dims"; javap -p -cp "$FJ" net.minecraft.world.entity.player.Player | grep -i "dimension\|eye\|pose" | head -30
echo "== LocalPlayer"; javap -p -cp "$FJ" net.minecraft.client.player.LocalPlayer | grep -i "aiStep\|input\|jump\|onGround\|serverAiStep\|sendPosition" | head -30
echo "== AvatarRenderer / PlayerRenderer"; unzip -l "$FJ" | grep -E "client/renderer/entity/player/" | awk '{print $4}' | head; javap -p -cp "$FJ" net.minecraft.client.renderer.entity.player.AvatarRenderer 2>&1 | head -40
echo "== LivingEntityRenderer upside"; javap -p -c -cp "$FJ" net.minecraft.client.renderer.entity.LivingEntityRenderer | grep -n -A30 "isUpsideDown\|isEntityUpsideDown" | head -80
echo "== LivingEntityRenderState"; javap -p -cp "$FJ" net.minecraft.client.renderer.entity.state.LivingEntityRenderState | head -50
echo "== GameRenderer bob/roll"; javap -p -cp "$FJ" net.minecraft.client.renderer.GameRenderer | grep -i "roll\|bob\|camera\|projection" | head -30
exit 0
