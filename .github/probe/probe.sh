#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
J=$(find .gradle/mavenizer/repo -name 'forge-26.3-*.jar' ! -name '*sources*' | head -1)
JAVAP=$(dirname $(readlink -f $(which java)))/javap
echo "== turn/mouse events"; unzip -l "$J" | awk '{print $4}' | grep -iE "client/event/.*(Turn|Mouse|Sensitiv)" | head
for c in net.minecraftforge.client.event.CalculatePlayerTurnEvent net.minecraft.client.MouseHandler; do echo "== $c"; $JAVAP -p -cp "$J" "$c" 2>&1 | grep -vE "^\s*$" | head -40; done
echo "== Entity.turn"; $JAVAP -c -p -cp "$J" net.minecraft.world.entity.Entity | grep -A30 "public void turn(double, double)" | head -40
# Also report through an annotation (readable through the checks API without downloading logs).
out=$(head -c 30000 /tmp/probe.txt | sed 's/%/%25/g; s/\r//g')
out=${out//$'\n'/%0A}
echo "::notice title=probe::${out}"
