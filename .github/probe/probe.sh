#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
FJ=$(find .gradle/mavenizer/repo -name 'forge-26.3-*.jar' ! -name '*sources*' | head -1)
for c in 'net.minecraftforge.client.event.RenderLivingEvent$Pre' 'net.minecraftforge.client.event.RenderLivingEvent' 'net.minecraftforge.client.event.RenderAvatarEvent$Pre' 'net.minecraft.client.player.ClientInput' 'net.minecraft.world.entity.player.Input' 'net.minecraft.client.MouseHandler' 'net.minecraftforge.event.TickEvent$RenderTickEvent$Pre' 'net.minecraftforge.event.TickEvent$RenderTickEvent' 'net.minecraftforge.event.entity.living.LivingEvent$LivingJumpEvent' 'net.minecraftforge.event.TickEvent$PlayerTickEvent$Post'; do echo "== $c"; javap -p -cp "$FJ" "$c" 2>&1 | head -40; done
echo "== TickEvent classes"; unzip -l "$FJ" | grep "net/minecraftforge/event/TickEvent" | awk '{print $4}'
echo "== Attributes GRAVITY syncable"; javap -p -c -cp "$FJ" net.minecraft.world.entity.ai.attributes.Attributes | grep -n -B2 -A12 '"gravity"' | head -30
echo "== isPlayerUpsideDown"; javap -p -c -cp "$FJ" net.minecraft.client.renderer.entity.player.AvatarRenderer | grep -n -A25 "public static boolean isPlayerUpsideDown" | head -40
echo "== Camera.alignWithEntity / update"; javap -p -c -cp "$FJ" net.minecraft.client.Camera | grep -n -A60 "private void alignWithEntity" | grep -i "invoke\|getfield" | head -40
echo "== GameRenderer camera angles event"; javap -p -c -cp "$FJ" net.minecraft.client.renderer.GameRenderer | grep -n -B6 -A6 "ComputeCameraAngles\|fireComputeCameraAngles\|onCameraSetup" | head -40
echo "== ForgeEventFactoryClient camera"; javap -p -c -cp "$FJ" net.minecraftforge.client.event.ForgeEventFactoryClient | grep -n -A20 "CameraAngles" | head -40
echo "== Entity.pick / Player pick"; javap -p -cp "$FJ" net.minecraft.world.entity.Entity | grep -i " pick" ; javap -p -cp "$FJ" net.minecraft.client.renderer.GameRenderer | grep -i "pick"
exit 0
