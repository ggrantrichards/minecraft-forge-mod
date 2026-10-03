#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
FJ=$(find .gradle/mavenizer/repo -name 'forge-26.3-*.jar' ! -name '*sources*' | head -1)
echo "jar: $FJ"
echo "== fake players"; unzip -l "$FJ" | grep -i "fakeplayer\|FakePlayer" | head
for c in net.minecraftforge.common.util.FakePlayer net.minecraftforge.common.util.FakePlayerFactory; do echo "== $c"; javap -cp "$FJ" $c 2>&1 | head -30; done
echo "== EntityRenderer culling"; javap -cp "$FJ" net.minecraft.client.renderer.entity.EntityRenderer | grep -i "render\|cull" | head -30
echo "== LivingEntityRenderer"; javap -p -cp "$FJ" net.minecraft.client.renderer.entity.LivingEntityRenderer | grep -i "upside\|shaking\|setupRotations\|isBodyVisible" | head
echo "== EntityType.Builder passengers"; javap -cp "$FJ" 'net.minecraft.world.entity.EntityType$Builder' | grep -i "passenger\|attach\|ridingOffset" | head
echo "== KeyMapping"; javap -cp "$FJ" net.minecraft.client.KeyMapping | grep -i "setDown\|isDown\|click" | head
echo "== ServerLevel noSave"; javap -cp "$FJ" net.minecraft.server.level.ServerLevel | grep -i "nosave" | head
echo "== attribute classes"; unzip -l "$FJ" | grep -E "net/minecraft/world/attribute/[A-Za-z]+\.class" | awk '{print $4}' | head -60
echo "== client attribute/fog"; unzip -l "$FJ" | grep -E "net/minecraft/client/renderer/fog/|CubicSampler|EnvironmentAttribute" | awk '{print $4}' | head -40
echo "== Camera"; javap -cp "$FJ" net.minecraft.client.Camera | grep -i "attribute\|fog\|biome" | head
for c in net.minecraft.world.attribute.EnvironmentAttributeSystem net.minecraft.world.attribute.EnvironmentAttributeProbe net.minecraft.world.attribute.EnvironmentAttributes; do echo "== $c"; javap -cp "$FJ" $c 2>&1 | head -40; done
echo "== Forge fog/viewport events"; unzip -l "$FJ" | grep -E "net/minecraftforge/client/event/(ViewportEvent|.*Fog)" | awk '{print $4}'
javap -cp "$FJ" 'net.minecraftforge.client.event.ViewportEvent$ComputeFogColor' 2>&1 | head -20
echo "== Player upside down / Dinnerbone"; javap -p -cp "$FJ" net.minecraft.client.renderer.entity.LivingEntityRenderer | grep -i "boolean is" | head
exit 0
