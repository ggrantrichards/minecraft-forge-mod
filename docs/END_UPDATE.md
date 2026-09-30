# The Astral Reaches: an End update

A design plan for expanding the End into a sparse, mysterious network of floating
"astral" regions, built as a Forge mod for Minecraft 26.3. Nothing here is final; every
biome, mob and number is a starting point for playtesting.

**Working name:** *The Astral Reaches*. **Suggested mod ID:** `astralreaches`.

---

## 1. Design pillars

Every idea in this document has to pass these five tests.

1. **Space, seen through Minecraft's eyes.** Nebulae, comets, dark matter and black holes
   become blocks, mobs and weather. They use 16×16 textures, a vanilla-style palette and
   chunky shapes. The mod adds no realistic space art, UI overlays or sci-fi HUDs.
2. **The End stays strange.** The End is not a second Overworld. It stays mostly void,
   islands stay far apart, and nothing explains itself. Players can build a home there,
   but it should feel like an outpost at the edge of something bigger.
3. **Wonder, then usefulness.** You notice a thing because it is beautiful or odd, and
   only later learn what it is for.
4. **Sidegrades, not ladders.** New gear doesn't make netherite obsolete. The upgrade
   system is about choosing a playstyle (see §8), so two late-game players can be equally
   strong and kitted out completely differently.
5. **Cheap to run.** No expensive light updates, no constant entity scans, no huge
   custom renderers. Most of the atmosphere lives in the sky, which costs almost nothing.

---

## 2. Lore: what the End actually is

This lore is never stated outright. Players piece it together from ruins, Star Charts
and the behaviour of mobs.

- Long ago, a people we'll call **the Cartographers** mapped the sky and built a road
  between the stars: the **Stellar Gateways**. The End was their crossroads, a flat
  plaza of stone floating between the stars.
- Something **collapsed a star** near the crossroads. The blast shattered the plaza into
  the islands we know, scattered the Gateways, and left scars that are still there:
  - the **Dark Matter Belts**, where space itself is bruised;
  - the **Void Comets**, fragments of the dead star, still orbiting;
  - the **Black Hole Guardians**, small tears that open and close on their own.
- The **Ender Dragon** is not a villain. It is the last warden of the broken crossroads,
  set to stop anyone from walking the star road again. That is why it guards the one
  place where a Gateway still half-works.
- **Endermen** are descendants of the Cartographers' workers. They still carry blocks
  around because they are trying, very slowly, to rebuild something none of them
  remembers. (This is why they take blocks and put them down elsewhere.)
- **Shulkers and End Cities** came later: settlers who moved in after the fall and built
  upward, away from the scars.
- **The Watcher** (§5.6) is the question the lore never answers: was it a Cartographer,
  the thing that collapsed the star, or something looking in from outside?

**How lore is delivered:**
- **Star Charts**: map-like items found in ruins. Each shows a fragment of the old
  Gateway network and marks one real, nearby structure.
- **Echo Logs**: text read by holding a Data Shard in a Portal Lens (§7). Short, vague,
  a few lines each, like the Ancient City's sculk mysteries.
- **Architecture**: ruins in a style unlike End Cities (rounded arches, star inlays,
  obsidian-and-quartz).

---

## 3. The map: how the End is laid out

Vanilla today has a central island with the dragon, then (1,000+ blocks out) outer islands
in four biomes: Highlands, Midlands, Barrens and Small Islands.

**The plan:**
- **The central island is untouched.** The dragon fight stays vanilla.
- The first **~1,000 blocks** of outer End keep the vanilla biomes, so End Cities and
  elytra hunting feel the same as always.
- Beyond that, the outer End becomes a **patchwork of vanilla biomes and five new astral
  biomes**, placed by a noise map. New biomes get rarer the further they are from the
  vanilla feel. Dark Matter Belts and the Shattered Observatory are rare.
- **Void is still the dominant biome.** New biomes never raise island density above
  vanilla Highlands. Travel between them should still take elytra, pearls, bridges or a
  Stellar Gateway.

| Biome | Rarity | Mood | Main purpose |
|---|---|---|---|
| **Nebula Fields** | Common | Calm, glowing, dreamy | Flora, pollinators, the most livable biome |
| **Aurora Shallows** | Uncommon | Quiet, wide, windy | Drift Whales, open building space |
| **Comet Wake** | Uncommon | Scarred, sparkling | Stardust, Meteoric Iron, Comet Craters |
| **Dark Matter Belt** | Rare | Tense, dim, disorienting | Void Crystal, gravity inversion, danger |
| **Shattered Observatory** | Very rare | Ancient, sacred | Lore, Stellar Gateways, the Watcher |

---

## 4. Biomes

### 4.1 Nebula Fields
Soft, low islands wrapped in coloured fog (violet into teal), with faint drifting motes
in the air.

- **Terrain:** Rounded islands of End Stone topped with **Nebula Moss**. They have
  shallow craters and gentle overhangs.
- **Low gravity:** players and mobs get about **0.65× gravity**, jump about **1.5× higher**,
  and take less fall damage. Ender pearls thrown here fly roughly 30% farther.
  - Uses vanilla's `gravity`, `jump_strength` and `safe_fall_distance` attributes, applied
    while the player stands in the biome and removed when they leave.
  - This can be turned off in the config.
- **Flora:**
  - **Lumineshrooms**: glowing mushrooms that "breathe", slowly pulsing brighter and
    dimmer.
  - **Astral Vines**: vines that grow upward off island undersides, the reverse of weeping
    vines. They're climbable.
  - **Starblooms**: flowers that always face the central island.
- **Mobs:** End Pollinators, rare Drift Whales passing through.
- **Feel:** the place you'd build your End base. It should be safe and beautiful, but
  still quiet and far from everything.

### 4.2 Aurora Shallows
Huge, flat, thin "shelf" islands close to the void, under a sky full of slow aurora
ribbons.

- **Terrain:** wide plates of **Pale End Stone**, with thin edges you can see through
  from below. There are patches of **Shimmer Grass** (a short, swaying plant) and
  standing stones.
- **Sky:** the strongest auroras in the End. This is where the distant silhouettes (§6)
  are most often seen.
- **Mobs:** **Drift Whales** spawn here more than anywhere else. Pollinators visit the
  Shimmer Grass.
- **Purpose:** flat building space, and the best place to hear the End's new music.

### 4.3 Comet Wake
The trail a comet left when it broke apart: scorched islands, a glittering "debris river"
of small floating chunks, and craters.

- **Terrain:**
  - **Charred End Stone**.
  - Veins of **Stardust Blocks**, a sand-like block that **falls up** instead of down.
    It's great for building hanging structures and waterfalls-in-reverse.
  - Shards of glassy **Comet Ice** that never melt and are slippery like blue ice.
- **Comet Cores:** big boulders embedded in islands. Their centre holds **Meteoric Iron**
  ore.
- **Comet Craters** (structure, §9) are common here.
- **Flora:** **Comet Cress**, a small white sprout that only grows on Stardust. It's
  edible.
- **Sky:** comet streaks are more frequent here.

### 4.4 Dark Matter Belt
Near-invisible islands. You can only really see them from their edges, where light
bends.

- **Terrain:**
  - **Umbral Stone**, a very dark, low-contrast block with a faint "lensing" edge
    particle on exposed faces.
  - The islands are jagged and ring-shaped, orbiting an empty centre.
- **Ore:** **Void Crystal Ore**, embedded in Umbral Stone. It glints only when you're
  close, so you find it by searching carefully.
- **Gravity Inversion Pockets:** clearly marked spherical zones where gravity flips.
  - You fall upward until you hit a ceiling.
  - They're bounded, always have a surface above them (never open void), and are visible
    from outside through slow, upward-drifting particles.
  - They're used for puzzles, loot caches on island undersides, and chase sequences.
  - This can be turned off in the config.
- **Mobs:**
  - **Black Hole Guardians** are most common here.
  - **Void Wanderers** patrol the edges.
  - This is also home to the one new hostile, the **Umbral Stalker** (§5.5).
- **Feel:** the dangerous frontier. There's no natural light, sounds are muffled, and the
  fog makes it disorienting.

### 4.5 Shattered Observatory
The rarest biome: a cluster of islands around a huge, broken ring of obsidian and quartz.

- **Terrain:** polished stonework ruins merged into the islands, fallen pillars, and star
  mosaics set into the floor.
- **Structures:** always contains one **Stellar Gateway** and the **Observatory Ruins**.
- **Flora:** overgrown with Astral Vines and Starblooms, as if someone once tended a
  garden here.
- **The Watcher** can only be met here, sometimes.

---

## 5. Mobs

All mobs follow two rules:
- Spawn caps are low and spawns are biome-specific.
- No mob can be farmed in an automated way for its rarest drop.

### 5.1 End Pollinator (ambient)
Small jellyfish-like drifters about the size of a bee, with trailing glowing tendrils.

- **Behaviour:**
  - They drift between glowing flora (Lumineshrooms, Starblooms, Shimmer Grass) and hover
    over each for a few seconds.
  - A pollinated plant grows one stage.
  - They make a soft, chime-like hum.
- **Drops:**
  - They leave **Glowing Spores** behind every few minutes, the way a chicken lays eggs.
    You don't need to kill them.
  - Killing one drops nothing extra, so there's no reason to.
- **Uses of Glowing Spores:** Spore Lanterns, glow dye, and spreading Nebula Moss by
  hand.
- **Breeding:** feed them Starblooms. This lets players build a "garden" of pollinators
  at home.

### 5.2 Drift Whale (passive, extremely rare)
A huge, translucent whale-like creature that swims slowly through the void. It's roughly
the size of a Ghast ×3, with a faintly visible skeleton and a slow tail beat.

- **Behaviour:**
  - It follows long, lazy paths through the void and never touches ground.
  - When a player comes close, it turns, hums (a deep, harmonic sound), and slows down.
  - It can't be hurt by normal means; hits pass through it.
- **Spawning:** at most one Drift Whale is near any player at a time. It despawns
  when it's far away. It's most common in Aurora Shallows.
- **Reward (an advancement chain, not a drop):**
  1. *Something Big Out There*: see a Drift Whale.
  2. *Whale Song*: be within 16 blocks while it hums.
  3. *Tagalong*: follow one for 60 seconds while flying.
  4. Completing the chain awards the music disc **"Leviathan"**, once per player.
  Because it's an advancement reward, it can't be farmed.
- **Bonus:** whales very rarely shed a **Whalelight Scale** (a decoration block that glows
  softly) when they hum. It's cosmetic only.

### 5.3 Void Wanderer (neutral)
A floating crystalline probe: a small octahedron with a single lens.

- **Behaviour:**
  - It wanders the edges of Dark Matter Belts and Observatory ruins.
  - When it notices a player, it approaches and shines a slow **scanning beam** over them.
  - **If you stand still and let it finish (5 seconds), it drops a Data Shard and leaves.**
    This rewards curiosity instead of violence.
  - Each Wanderer only scans each player once.
  - If you attack it, it becomes hostile, fires short laser pulses, and teleports away
    when low. Killing it drops only a **Cracked Lens**, a crafting scrap.
- **Data Shards:** used in utility recipes (Portal Lens, Gateway repair, Echo Logs).

### 5.4 Black Hole Guardian (hazard mob)
A small, unstable singularity: a black sphere with a rotating accretion ring.

- **Behaviour:**
  - It **pulls** nearby entities and dropped items toward itself within a small radius.
  - It never breaks blocks.
  - It lives for 2–3 minutes, then collapses and despawns.
- **Item safety:** items it swallows are **kept, not deleted**. It drops them all when it
  collapses or is killed. This avoids the frustration of losing gear.
- **Killing it:** you can only damage it by hitting its core when the ring flickers, so
  it's a timing fight. Reward: **Singularity Shard**.
- **Anti-farming:**
  - It can't be spawned or kept alive artificially.
  - Shards drop only from a kill, not from natural collapse.
  - Only one can exist near a player at a time.
- **Uses of Singularity Shards:**
  - **Gravity Wells**: blocks that slowly pull nearby dropped items in, a magnet
    alternative to hoppers.
  - **Void Traps**: a trapdoor-like block that holds a mob in place for a few seconds.

### 5.5 Umbral Stalker (hostile)
The End needs one real new threat. This is a lanky, almost invisible predator that lives
in the Dark Matter Belts.

- **Visibility:**
  - It is hard to see: only a faint shimmer outline, plus distortion particles when it
    moves.
  - It becomes fully visible while attacking, when hit, or when lit by a Spore Lantern or
    seen through a Portal Lens.
- **Behaviour:**
  - It stalks the player from behind and retreats when the player turns to face it (the
    reverse of an Enderman).
  - When it reaches you, it hits hard and tries to knock you toward the void.
- **Drops:** **Umbral Membrane**, used for invisibility-adjacent items such as Stalker
  Cloaks (§8).
- **Counterplay:** light (spores), a Portal Lens, or keeping your back to a wall.

### 5.6 The Watcher (unique, never fought)
A tall, still silhouette standing at the very edge of render distance, on islands in the
Shattered Observatory.

- **Behaviour:**
  - It appears only when you're alone, only at a distance, and only for a few seconds.
  - If you look at it through a **spyglass**, it vanishes. A new Star Chart then appears
    in your inventory, pointing to the nearest undiscovered Observatory.
- **It never attacks and can't be reached.** It gives the advancement
  *Something Looks Back*.
- **Purpose:** mystery and guidance. It's the thread that pulls players deeper into the
  lore.

---

## 6. Atmosphere and the sky

Most of the "cosmic" feel costs nothing in performance because it lives in the sky
renderer, not in the world.

- **Moving stars:** the End sky becomes a two-layer starfield. One layer rotates very
  slowly, so it takes about 20 minutes to notice.
- **Auroras:** slow, translucent ribbons of colour.
  - Strongest over Aurora Shallows and faint elsewhere.
  - Colours shift with biome: violet in the Nebula Fields, green-gold in the Shallows,
    none in the Dark Matter Belts.
- **Comets:** every few in-game hours, a comet streak crosses the sky. It's purely
  visual.
- **Distant silhouettes:** very rarely, a huge shape (a whale, a ring, or something that
  isn't either) passes across the far sky.
  - These are drawn as part of the sky, not as entities, so they never lag the game.
- **Biome fog and ambience:**
  - Each biome has its own fog colour, particles, ambient sound loop and "mood" sounds,
    like cave sounds.
  - Nebula Fields hum softly. Dark Matter Belts are nearly silent.
- **Music:** 3–4 new End tracks that play only in the new biomes, plus the "Leviathan"
  disc.
- **Config:** every visual effect has an on/off toggle for low-end machines.

---

## 7. Blocks, flora and items

### 7.1 New ores and raw materials

| Material | Where | How you get it | What it's for |
|---|---|---|---|
| **Void Crystal** | Void Crystal Ore in Umbral Stone (Dark Matter Belts) | Mine with an iron pickaxe or better. Fortune works. | Starforged Alloy, Portal Lens, Resonance Cores |
| **Meteoric Iron** | Meteoric Iron Ore in Comet Cores (Comet Wake) | Mine with a diamond pickaxe. Smelt the raw chunks. | Starforged Alloy, heavy tools, Gravity Wells |
| **Temporal Crystal** | Loot only: Comet Crater cores and Observatory vaults | Can't be mined or farmed | Portal Lens, Temporal Resonance Core, Gateway repair |
| **Stardust** | Stardust Blocks (Comet Wake) | Shovel | Building, Astral Glass, Comet Cress farms |

**Starforged Alloy** = 2 Void Crystal + 2 Meteoric Iron Ingot + 1 Glowing Spore, crafted
at the Astral Forge (§8).

### 7.2 Building blocks
Every stone type gets the full vanilla set: slabs, stairs, walls, polished, bricks and
chiseled.

- **Umbral Stone** (+ Polished, Bricks, Chiseled "Eclipse" pattern)
- **Pale End Stone** (+ Bricks, Tiles)
- **Charred End Stone**
- **Observatory Quartz** (+ Star Inlay variant)
- **Stardust Block**: falls up.
- **Astral Glass**: smelted from Stardust. A dark glass with a faint star speckle.
  - A rare **Starfield Glass** variant shows the End-portal starfield effect.
  - Starfield Glass is expensive and limited to a handful per build, for performance.
- **Comet Ice**
- **Nebula Moss** (a ground cover that spreads like moss) and **Moss Carpet**
- **Spore Lantern** (light level 13) and **Whalelight Scale Block** (light level 10)
- **Gravity Well**, **Void Trap** (Singularity Shard utility blocks)

### 7.3 Cosmic plants

| Plant | Behaviour | Uses |
|---|---|---|
| **Lumineshroom** | Grows on Nebula Moss. It pulses brightness in a slow ~8-second "breath" cycle. Its actual light level only switches between two values every few seconds, so it doesn't cause lag. | Spore Lanterns, glowing stew, dye |
| **Astral Vine** | Grows **upward** from the undersides of blocks. Climbable. Shears stop growth. | Decoration, climbing up island edges |
| **Starbloom** | A flower that rotates to face the End's centre, working as a compass. | Pollinator breeding, dye, compass recipe |
| **Shimmer Grass** | A short, swaying grass in the Aurora Shallows | Decoration, Pollinator food |
| **Comet Cress** | Grows only on Stardust, in any light | Food (hunger 3), light crop farming |
| **Gravlily** | A rare flower from Dark Matter Belt inversion pockets. It grows upside down. | Brewing: **Potion of Buoyancy** (slow fall + a small jump boost; a sidegrade to Slow Falling) |

### 7.4 Tools, gear and utility items
- **Portal Lens** (Data Shard ×3 + Void Crystal + Temporal Crystal + Spyglass). A
  spyglass upgrade that:
  - shows distortion outlines (Umbral Stalkers, hidden ore glints);
  - points toward the nearest known Stellar Gateway;
  - reads Echo Logs from Data Shards;
  - is also the key used to repair and link Gateways.
- **Star Chart**: a lore map found in ruins or given by the Watcher.
- **Stalker Cloak**: a cape worn in the chest slot, like an elytra. Mobs lose track of
  you faster, but you give up both your chestplate and your elytra to wear it. A real
  choice, not a flat upgrade.
- **Glowing Stew**: Lumineshroom + Comet Cress + bowl. Gives a short Glowing and Night
  Vision effect.

---

## 8. Starforging: upgrades as self-expression

This is the heart of the update's progression. **Netherite stays the strongest raw armour
and weapons.** Starforging doesn't raise numbers; it adds a *personality* to a piece of
gear.

### 8.1 The Astral Forge
A new workstation:
- **Recipe:** Meteoric Iron block + Observatory Quartz + Void Crystal.
- **What it does:**
  - Add a Resonance Core to a piece of gear.
  - Swap a core out (the old core is lost; you pay again).
  - Craft Starforged Alloy.

### 8.2 Starforged gear
- Upgrade **diamond or netherite** gear with a **Starforge Template** plus 1 Starforged
  Alloy.
  - Templates are found in Observatory vaults and can be duplicated like smithing
    templates.
- **The upgrade keeps the base stats of the material underneath** (diamond stays diamond,
  netherite stays netherite). It adds one **Resonance slot** and a subtle star-speckle
  texture.

### 8.3 Resonance Cores
Every core has an **upside and a real downside**, so no core is simply "best".

| Core | Made from | Upside | Downside |
|---|---|---|---|
| **Nebular** | Glowing Spores + Void Crystal | +10% move speed, lower fall damage | −1 armour toughness on that piece |
| **Gravitic** | Meteoric Iron + Void Crystal | +knockback resistance, hits knock enemies further | −5% move speed, you sink faster in water |
| **Temporal** | Temporal Crystal | Shorter ender pearl, shield and item cooldowns | Durability drains 50% faster |
| **Singular** | Singularity Shard | Pulls nearby dropped items and XP to you (a magnet) | Black Hole Guardians and Umbral Stalkers notice you from further away |
| **Umbral** | Umbral Membrane | Endermen don't mind being looked at; hostile mobs lose track of you faster | Regeneration is slower while you stand in light |
| **Radiant** | Whalelight Scale + Glowing Spores | Enemies you hit glow briefly; +1 max health | You glow too, visible through walls to other players and mobs |

### 8.4 Constellations: rewards for creative combos
Wearing **specific combinations** of cores across your armour (not four of the same)
unlocks a small bonus and a matching particle trail.

| Constellation | Pattern | Bonus |
|---|---|---|
| **Binary Star** | 2 Nebular + 2 Gravitic | Sneak while mid-air in low gravity to do a short air dash |
| **Event Horizon** | Singular + Umbral + any 2 | Items you drop can't be picked up by mobs |
| **Aurora** | Radiant + Nebular + Temporal + any | Your glow becomes an aurora trail; Pollinators follow you |
| **Pulsar** | 4 different cores | Every 30 seconds, your next hit releases a small shockwave |

Stacking four of the same core gives **no set bonus**, which pushes players toward mixed,
personal builds.

### 8.5 Weapons and tools
Tools and weapons take one core too, with tool-specific effects. For example:
- a **Gravitic pickaxe** mines slower but breaks a 1×2 area;
- a **Temporal sword** attacks faster but loses durability quicker.

---

## 9. Structures

| Structure | Where | What's inside |
|---|---|---|
| **Comet Crater** | Comet Wake, sometimes other outer islands | A bowl of Stardust and Charred Stone around a Comet Core. There's a loot cache at the centre with a chance of **Temporal Crystals**, Meteoric Iron and Star Charts. Some craters are still "hot", with ember particles and fire around the rim. |
| **Observatory Ruins** | Shattered Observatory, rare in Nebula Fields | A domed ruin with star mosaics and a telescope made of blocks. Vaults (opened with Data Shards, like trial keys) hold Starforge Templates, Temporal Crystals, Echo-Log shards and the Astral Forge recipe book. |
| **Stellar Gateway** | Always in the Shattered Observatory, rarely elsewhere | A broken ring of Observatory Quartz. Repair it with a Portal Lens + 4 Temporal Crystals to **link it to any other Gateway you've repaired** (fast travel). Each jump costs 1 ender pearl, which keeps it fair. |
| **Wanderer Relay** | Dark Matter Belt edges | A thin floating spire where Void Wanderers dock. It has no loot, but Wanderers spawn here, and its top holds an Echo Log. |
| **Whalefall** | Aurora Shallows, very rare | The fossilised ribs of an ancient Drift Whale, arched over an island. Whalelight Scales grow on the bones, and it has one lore chest. |

**Comet landfall (a live event):**
- While a player is in a Comet Wake, there's a small chance per in-game day that a comet
  streak "lands": a small meteor entity strikes a nearby island.
- It makes a mini crater (a handful of blocks, respects `mobGriefing`) holding Stardust
  and a chance of Meteoric Iron.
- Big Comet Craters are generated with the world, not made live, to keep world edits
  small and safe.

---

## 10. Progression

1. **Kill the dragon** (unchanged), then head out through the End Gateway.
2. **The first ~1,000 blocks:** vanilla End Cities, an elytra, and shulker boxes.
3. **Nebula Fields:** your first new biome. Pollinators, glowing flora and low gravity.
   Build an outpost here.
4. **Comet Wake:** mine Meteoric Iron and Stardust, and raid Comet Craters for Temporal
   Crystals.
5. **Dark Matter Belt:** dangerous. Collect Void Crystal, meet the Umbral Stalker, fight
   Black Hole Guardians, and let Void Wanderers scan you for Data Shards.
6. **Craft a Portal Lens and build the Astral Forge.** Start Starforging.
7. **Shattered Observatory:** follow Star Charts and the Watcher. Repair Gateways and
   link your outposts.
8. **Optional long tail:** complete the Drift Whale chain, collect every Echo Log, and
   try every Constellation.

**Advancement tab: "The Astral Reaches".** About 20 advancements, including:
- *Falling Upward*: use a Stardust block.
- *Hold Still*: get scanned by a Void Wanderer.
- *Event Horizon*: kill a Black Hole Guardian.
- *Something Looks Back*
- *Star Road*: link two Gateways.
- *Leviathan*

---

## 11. Balance and anti-farming rules

- **No strict upgrade over netherite.** Starforging adds a choice, not raw stats.
- **The rarest rewards are one-time or limited:**
  - the Leviathan disc comes from an advancement;
  - Temporal Crystals are loot only;
  - Singularity Shards need a manual timing kill.
- **Data Shards** come from a non-violent interaction that's limited to once per
  Wanderer per player.
- **Low gravity and gravity inversion** can be turned off in the config, for servers and
  players who get motion sick.
- **Black Hole Guardians never delete items** and never break blocks.
- **Gateways cost ender pearls per jump** so they don't replace elytra travel entirely.

---

## 12. Performance budget

| System | Approach |
|---|---|
| Sky (stars, auroras, comets, silhouettes) | Drawn in the End sky renderer; no entities, no world data |
| Lumineshroom "breathing" | The visual pulse is an animated texture. The real light level changes only between 2 values, at most every few seconds, on random ticks. |
| Biome gravity | A player-only check about once per second that adds or removes an attribute modifier. No per-tick scans. |
| Gravity inversion pockets | Stored as small spheres generated with the world; entities check only when inside a chunk that has one |
| Drift Whale | One per player area. It doesn't collide with blocks, uses a simple model, and despawns aggressively. |
| Black Hole Guardian pull | Checks nearby entities every 5 ticks, within a radius of 6 blocks |
| Gravity Well block | Pulls items every 10 ticks, with a radius of 4 |
| Starfield Glass | Uses the end-portal effect; limited by its crafting cost |

---

## 13. How to build it (implementation roadmap)

Each phase is playable on its own. Later phases build on earlier ones.

| Phase | Content | Main technical work |
|---|---|---|
| **1. Blocks and flora** | Umbral Stone set, Stardust (falls up), Astral Glass, Lumineshroom, Astral Vine, Starbloom, Comet Cress, Void Crystal and Meteoric Iron ore/items | Registering blocks and items (like the Ruby), plus a custom `FallingBlock` subclass that falls up. Mostly JSON: models, loot tables, recipes, tags. |
| **2. First biome: Nebula Fields** | The biome, its terrain features and low gravity | The End's biome list is **hard-coded in vanilla**. Adding biomes needs a custom `BiomeSource` registered in Java, then a data-pack override of `dimension/the_end.json` to use it. Biomes and features themselves are JSON. |
| **3. Remaining biomes** | Aurora Shallows, Comet Wake, Dark Matter Belt, Shattered Observatory, inversion pockets | More JSON worldgen, plus Java for inversion pockets |
| **4. Mobs** | Pollinator, Void Wanderer, Black Hole Guardian, Umbral Stalker, then Drift Whale and the Watcher | Entity classes, AI goals, models (Blockbench → Java model code), spawn rules |
| **5. Structures** | Comet Crater, Observatory Ruins, Wanderer Relay, Whalefall, Stellar Gateway | Structure templates built in-game with structure blocks and saved as `.nbt`, plus jigsaw JSON. Gateway linking needs Java. |
| **6. Starforging** | Astral Forge, templates, Resonance Cores, Constellations | Data components on items, a menu and screen for the Forge, attribute modifiers, an event listener for Constellation checks |
| **7. Sky and atmosphere** | Moving stars, auroras, comets, silhouettes, music | A custom `DimensionSpecialEffects` for the End (client-only rendering), plus sounds and music |
| **8. Polish** | Advancements, Echo Logs, config toggles, balance passes | JSON advancements, a Forge config file, playtesting |

**Suggested first task:** Phase 1's **Stardust Block** and **Lumineshroom**. They use
the same registration pattern as the Ruby Block, add one small twist each (falling
upward; an animated texture), and prove out the art style before any worldgen work.

---

## 14. Open questions

1. **Mod name and ID:** `astralreaches` in a new repository, or rename `rubymod` and
   build it here?
2. **Where the new biomes start:** beyond 1,000 blocks, or only beyond 2,000–3,000
   blocks, so early End exploration stays exactly vanilla?
3. **Low gravity:** on by default for everyone, or only while wearing a Nebular core?
4. **Gateway cost:** ender pearls, chorus fruit, or a new fuel such as Stardust?
5. **Is one hostile mob enough,** or should Comet Wake get a small threat too (for
   example, burning "Cinder Mites" in hot craters)?
6. **Art:** should textures be drawn first (in Blockbench/Piskel), or should we start with
   placeholder textures and replace them later?
