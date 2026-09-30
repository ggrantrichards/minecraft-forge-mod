#!/usr/bin/env bash
set +e
ASSETS=$(find .gradle/mavenizer/repo/net/minecraft/client-extra -name '*.jar' | head -1)
mkdir -p /tmp/assets && unzip -qo "$ASSETS" -d /tmp/assets
python3 - <<'PY'
import json
T='/tmp/assets/data/minecraft/tags/block/'
def expand(name, seen=None):
    seen = seen or set()
    out=set()
    for v in json.load(open(T+name+'.json'))['values']:
        v = v['id'] if isinstance(v, dict) else v
        if v.startswith('#'):
            n=v[1:].split(':')[1]
            if n not in seen:
                seen.add(n); out |= expand(n, seen)
        else:
            out.add(v.split(':')[1])
    return out
for tag in ('blocks_motion_no_leaves', 'causes_suffocation'):
    s=expand(tag)
    print('===', tag, len(s))
    for b in ['end_stone','stone','glass','tinted_glass','stone_stairs','stone_slab','cobblestone_wall','blue_ice','bone_block','moss_block','moss_carpet','short_grass','quartz_pillar','chest','spawner','sculk_catalyst','end_stone_brick_wall','deepslate_tile_slab']:
        print(f'  {b}: {b in s}')
PY
