#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
J=$(find .gradle/mavenizer/repo -name 'client-extra*.jar' | head -1)
mkdir -p /tmp/music/files
for b in end_stone blackstone deepslate moss_block cherry_log cherry_log_top cherry_leaves sandstone terracotta smooth_sandstone amethyst_block bone_block_side deepslate_bricks calcite shroomlight quartz_block_side netherrack basalt_side tuff purpur_block mangrove_log dark_oak_log crimson_stem warped_wart_block; do
  unzip -p "$J" assets/minecraft/textures/block/$b.png > /tmp/music/files/$b.png && echo ok $b
done
exit 0
