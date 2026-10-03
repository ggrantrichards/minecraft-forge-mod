#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
FJ=$(find .gradle/mavenizer/repo -name 'forge-26.3-*.jar' ! -name '*sources*' | head -1)
for c in net.minecraft.client.renderer.entity.state.AvatarRenderState net.minecraft.client.renderer.entity.state.EntityRenderState net.minecraft.world.item.context.UseOnContext net.minecraft.world.level.ClipContext; do echo "== $c"; javap -p -cp "$FJ" "$c" 2>&1 | grep -v lambda | head -60; done
echo "== ServerPlayer ctors"; javap -p -cp "$FJ" net.minecraft.server.level.ServerPlayer | grep "ServerPlayer(" | head
echo "== ClientInformation"; javap -p -cp "$FJ" net.minecraft.server.level.ClientInformation | grep -i "static\|createDefault" | head
echo "== GameProfile ctor"; find / -name "authlib*.jar" 2>/dev/null | head -2; AJ=$(find / -name "authlib*.jar" 2>/dev/null | head -1); javap -cp "$AJ" com.mojang.authlib.GameProfile | head -12
echo "== SpawnEggItem.useOn"; javap -p -c -cp "$FJ" net.minecraft.world.item.SpawnEggItem | grep -n -A80 "InteractionResult useOn" | grep -i "invoke\|getfield" | head -40
echo "== EnderEyeItem.useOn"; javap -p -c -cp "$FJ" net.minecraft.world.item.EnderEyeItem | grep -n -A60 "InteractionResult useOn" | grep -i "invoke" | head -30
echo "== Minecraft hitResult"; javap -p -cp "$FJ" net.minecraft.client.Minecraft | grep -i "hitResult\|crosshairPickEntity" | head
echo "== GameRenderer.pick"; javap -p -c -cp "$FJ" net.minecraft.client.renderer.GameRenderer | grep -n -A70 "public void pick(float)" | grep -i "invoke\|putfield" | head -30
echo "== Attributes gravity"; javap -p -c -cp "$FJ" net.minecraft.world.entity.ai.attributes.Attributes | grep -n -i -A10 "gravity" | head -30
echo "== RenderTickEvent fire site"; javap -p -c -cp "$FJ" net.minecraft.client.Minecraft | grep -n "RenderTickEvent\|onRenderTick\|handleAccumulatedMovement\|gameRenderer.*render\|GameRenderer.render" | head
exit 0
