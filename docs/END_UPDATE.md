# The Astral Reaches: an End update

A design plan for expanding the End into a sparse, mysterious network of floating
"astral" regions, built as a Forge mod for Minecraft 26.3. Nothing here is final; every
biome, mob and number is a starting point for playtesting.

**Working name:** *The Astral Reaches*. **Suggested mod ID:** `astralreaches`.

---

## 1. Design pillars

Every idea in this document has to pass these five tests.

1. **Space, seen through Minecraft's eyes.** Nebulae, comets, dark matter, flares and
   black holes become blocks, mobs and weather. They use 16×16 textures, a vanilla-style
   palette and chunky shapes. The mod adds no realistic space art, UI overlays or sci-fi
   HUDs. Even the alien technology looks like it belongs next to a copper bulb.
2. **The End stays strange.** The End is not a second Overworld. It stays mostly void,
   islands stay far apart, and nothing explains itself. Players can build a home there,
   but it should feel like an outpost at the edge of something bigger.
3. **Wonder, then usefulness.** You notice a thing because it is beautiful or odd, and
   only later learn what it is for.
4. **Upgrades are choices.** Starforging (§9) offers several upgrades that are all
   *strictly better* than no upgrade, but each improves a *different* thing. Every piece
   of gear holds only one, so the fun is in the judgement call: two late-game players
   can be equally strong and kitted out completely differently.
5. **Cheap to run.** No expensive light updates, no constant entity scans, no moving
   block structures. Most of the atmosphere lives in the sky, which costs almost nothing.

---

## 2. Lore: what the End actually is

This lore is never stated outright. Players piece it together from ruins, Star Charts,
Echo Logs and the behaviour of mobs.

- **The Star Leviathans.** Long ago, colossal creatures swam between the stars, and they
  could swallow a traveller and carry them from one star to another.
  - **Drift Whales** are their last, much smaller descendants.
- **The Cartographers.** A people who learned to ride the Leviathans. They mapped the
  sky, built **Orbital Stations** as waystations, and made the End their crossroads: a
  plaza of stone floating between the stars.
- **The Maws.** When a Leviathan died, the Cartographers kept its skull open. Its mouth
  still led where the creature had last travelled. These fossil skulls are the
  **Wormhole Maws**, the gates of the star road.
- **The collapse.** Something **collapsed a star** near the crossroads. The blast
  shattered the plaza into the islands we know and left scars that are still there:
  - the **Dark Matter Belts**, where space itself is bruised;
  - the **Void Comets**, fragments of the dead star, still orbiting;
  - the **Black Hole Guardians**, small tears that open and close on their own;
  - the **Stellar Flares**: the dead star's remnant still pulses, washing the End in
    storms every few days.
- **The Ender Dragon** is not a villain. It is the last warden of the broken crossroads,
  set to stop anyone from walking the star road again.
- **Endermen** are descendants of the Cartographers' workers. They still carry blocks
  around because they are trying, very slowly, to rebuild something none of them
  remembers.
- **Void Wanderers** are Cartographer probes, still running their surveys, still docking
  at stations that have been dead for ages.
- **Shulkers and End Cities** came later: settlers who moved in after the fall and built
  upward, away from the scars.
- **The Watcher** (§5.6) is the question the lore never answers: a Cartographer, the
  thing that collapsed the star, or something looking in from outside?

**How lore is delivered:**
- **Star Charts**: map-like items found in ruins. Each shows a fragment of the old star
  road and marks one real, nearby structure.
- **Echo Logs**: short, vague text read by holding a Data Shard in a Portal Lens (§8.4).
- **Constellations**: the star patterns in Constellation Mazes (§10) are the
  Cartographers' route markers. Solving one teaches a new Star Chart.
- **Architecture**: ruins in a style unlike End Cities, with rounded arches, star inlays,
  Leviathan bone and tarnished violet bronze.

### 2.1 Staying true to vanilla

The new lore is built to **explain** what vanilla already shows, never to contradict it.
Vanilla facts it hooks into:

| Vanilla fact | How the Astral Reaches explains or uses it |
|---|---|
| The End has **no day/night cycle, no weather**, and a static sky texture | The sky is the dead star's afterimage. Stellar Flares become the End's only weather; the sky moves only very slowly. |
| **Compasses and clocks spin** in the End; **beds explode** | Space here is "bent" by the collapse. Starblooms are the only thing that points reliably (to the central island). Beds still explode. |
| **Endermen** carry blocks, teleport, hate eye contact, take damage from water, and make garbled, distorted "speech" sounds | They are the Cartographers' old workers, still moving blocks toward a rebuild nobody remembers. Eye contact was forbidden to workers (a Cartographer taboo mentioned in Echo Logs). Their garbled sounds are a degraded version of Cartographer speech, and **Echo Logs use the same distorted sound** when read. Water was never part of the star road. |
| The End is where Endermen spawn most; they **never attack the dragon** | They see the dragon as part of the crossroads they serve. |
| The **Ender Dragon** heals from **End Crystals** on the **obsidian pillars**, perches on the **exit portal**, and can be **respawned** with four crystals | The pillars and crystals are the warden's anchor, the last working Cartographer machine. The fact that it can be summoned again shows it's a function, not a creature that dies. |
| Each dragon kill opens a new **End Gateway** (up to 20) | End Gateways are **tiny, dragon-made Maws**. Each defeat loosens the warden's grip and reopens one short stretch of the old star road. This is why Maws feel related to them. |
| The **Dragon Egg** is left on the exit portal and never hatches | Left unanswered on purpose. Echo Logs hint at it but never explain it. |
| **End Cities** are full of **Shulkers**, **purpur** and **end rods**, and **End Ships** carry the **elytra** | Unchanged. Shulkers are post-collapse settlers. The elytra is their wing technology, which is why it's the best way to reach the Maws and Stations. |
| **Chorus plants** grow on End Stone, and **chorus fruit** teleports you | Chorus is the one plant that survived the collapse, and it still "remembers" short jumps along the star road. It stays the End's staple food and is an alternative fuel for Maw jumps. |
| **Eyes of Ender** fly toward strongholds; **End Portal frames** sit in strongholds | The Overworld portals are the far end of the star road. Echo Logs suggest whoever built strongholds learned the trick from the Cartographers. |
| The **Ancient City** in the Deep Dark has a huge portal-shaped frame of reinforced deepslate that never works | A failed attempt to build a Maw from below. One rare Echo Log mentions "the deep builders who reached for us and went quiet". It's a hint only, never confirmed. |
| The End was first imagined as a floating **"Sky Dimension"** during early development | The Orbital Stations in the high sky are a quiet nod to it. |
| The **End Poem** plays after the first dragon kill, with two unseen voices discussing the player | The Watcher can be read as one of those voices, or not. **The mod never quotes or explains the End Poem;** it only leaves room for that reading. |

**Rules for new content:**
- Vanilla mobs, blocks and the dragon fight behave exactly as before.
- New lore may add meaning, but it never "solves" a vanilla mystery outright.
- Echo Logs are short and fragmentary, and use the same distorted Enderman-style voice
  sounds instead of voice acting.

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
  vanilla feel.
- **Void is still the dominant biome.** New biomes never raise island density above
  vanilla Highlands. Travel between them should still take elytra, pearls, bridges or a
  Wormhole Maw.
- **The high sky (Y 180+) is new territory.** Orbital Stations hang up there, far above
  the islands, so players have a reason to look (and fly) up.

| Biome | Rarity | Mood | Main purpose |
|---|---|---|---|
| **Nebula Fields** | Common | Calm, glowing, dreamy | Flora, pollinators, the most livable biome |
| **Aurora Shallows** | Uncommon | Quiet, wide, windy | Drift Whales, open building space |
| **Comet Wake** | Uncommon | Scarred, sparkling | Stardust, Meteoric Iron, craters, live comets |
| **Dark Matter Belt** | Rare | Tense, dim, disorienting | Void Crystal, gravity inversion, danger |
| **Shattered Observatory** | Very rare | Ancient, sacred | Lore, Wormhole Maws, Constellation Mazes, the Watcher |

---

## 4. Biomes

### 4.1 Nebula Fields
Soft, low islands wrapped in coloured fog (violet into teal), with faint drifting motes
in the air.

- **Terrain:** rounded islands of End Stone topped with **Nebula Moss**. They have
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
- **Sky:** the strongest auroras in the End. During a Stellar Flare, this is the most
  spectacular place to be.
- **Mobs:** **Drift Whales** spawn here more than anywhere else. Pollinators visit the
  Shimmer Grass.
- **Structures:** **Whalefalls**, the fossilised ribs of old Leviathans.
- **Purpose:** flat building space, and the best place to hear the End's new music.

### 4.3 Comet Wake
The trail a comet left when it broke apart: scorched islands, a glittering "debris river"
of small floating chunks, and craters.

- **Terrain:**
  - **Charred End Stone**.
  - Veins of **Stardust Blocks**, a sand-like block that **falls up** instead of down.
    It's great for building hanging structures.
  - Shards of glassy **Comet Ice** that never melt and are slippery like blue ice.
- **Comet Cores:** big boulders embedded in islands. Their centre holds **Meteoric Iron**
  ore.
- **Comet Craters** (§10) are common here.
- **Live comets** (§7.2) pass through most often here.
- **Flora:** **Comet Cress**, a small white sprout that only grows on Stardust. It's
  edible.

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
- **Structures:**
  - always one **Wormhole Maw**;
  - usually one **Constellation Maze**;
  - often an **Orbital Station** hanging in the sky above.
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
- **During a Stellar Flare:** they glow brighter and drop spores twice as often.

### 5.2 Drift Whale (passive, extremely rare)
A huge, translucent whale-like creature that swims slowly through the void. It's roughly
the size of a Ghast ×3, with a faintly visible skeleton and a slow tail beat. It's the
last small descendant of the Star Leviathans.

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
  softly) when they hum.

### 5.3 Void Wanderer (neutral)
A floating crystalline probe: a small octahedron with a single lens. It's a Cartographer
survey drone that is still running.

- **Behaviour:**
  - It wanders the edges of Dark Matter Belts, Orbital Stations and Observatory ruins.
  - When it notices a player, it approaches and shines a slow **scanning beam** over them.
  - **If you stand still and let it finish (5 seconds), it drops a Data Shard and leaves.**
    This rewards curiosity instead of violence.
  - Each Wanderer only scans each player once.
  - If you attack it, it becomes hostile, fires short laser pulses, and teleports away
    when low. Killing it drops only a **Cracked Lens**, a crafting scrap.
  - **During a Stellar Flare,** Wanderers malfunction: they drift aimlessly and spark,
    and can't scan.
- **Data Shards:** used in utility recipes (Portal Lens, powering stations, Echo Logs).

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
- **Drops:** **Umbral Membrane**, used for the Stalker Cloak and the Umbral Core.
- **Counterplay:** light (spores), a Portal Lens, or keeping your back to a wall.

### 5.6 The Watcher (unique, never fought)
A tall, still silhouette standing at the very edge of render distance, on islands in the
Shattered Observatory.

- **Behaviour:**
  - It appears only when you're alone, only at a distance, and only for a few seconds.
  - If you look at it through a **spyglass or telescope**, it vanishes. A new Star Chart
    then appears in your inventory, pointing to the nearest undiscovered Maw, Maze or
    Station.
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
- **Distant silhouettes:** very rarely, a huge shape (a whale, a ring, or something that
  isn't either) passes across the far sky.
  - These are drawn as part of the sky, not as entities, so they never lag the game.
- **Background comets:** frequent, purely visual streaks far away. They're separate from
  the live comets you can chase (§7.2).
- **Biome fog and ambience:**
  - Each biome has its own fog colour, particles, ambient sound loop and "mood" sounds,
    like cave sounds.
  - Nebula Fields hum softly. Dark Matter Belts are nearly silent.
- **Music:** 3–4 new End tracks that play only in the new biomes, plus the "Leviathan"
  disc.
- **Config:** every visual effect has an on/off toggle for low-end machines.

---

## 7. Events

### 7.1 Stellar Flares (storms)
The collapsed star's remnant still pulses. Roughly every **60–100 minutes of play**, the
outer End goes through a flare. The End has no weather and no day/night cycle in vanilla,
so flares become its only "weather", much like a thunderstorm in the Overworld. The
central island is never affected.

**The three phases:**

| Phase | Length | What happens |
|---|---|---|
| **Warning** | ~1 minute | The sky's stars brighten, auroras flicker faster, a low rising hum plays, and Pollinators head for cover. Experienced players learn to read it. |
| **Flare** | 2–3 minutes | Sheets of bright light roll across the sky. Charged particles stream horizontally, and auroras go wild. |
| **Afterglow** | ~2 minutes | Everything glows softly and settles. |

**Effects during the Flare phase:**
- **Danger:** players with open sky above them take small, periodic "radiant burn"
  damage, like standing near a campfire.
  - Any solid block overhead blocks it, and so does a Radiant Starforge upgrade (§9).
  - The point is to give shelter a purpose, as rain and thunder do. It's never a death
    sentence on its own.
- **Solar wind:** elytra flight gets a push in the flare's direction, so flying with
  it is fast and flying against it is slow.
- **Wanderers malfunction,** and Pollinators drop more spores.
- **Orbital Stations power up** (§10) and their sealed vaults open.
- **Flare Charging:** Void Crystal exposed to the sky becomes **Charged Void Crystal**.
  So does any Void Crystal item placed on a **Collector Pedestal** in open air.
  - Charged Void Crystal is needed for the strongest recipes, so players plan for
    flares instead of just hiding from them.
- **Afterglow bonus:** Lumineshrooms bloom (a guaranteed growth tick).

**Config:** frequency, damage (on/off) and sky effects can each be adjusted.

### 7.2 Live comets and meteors
**Honest limitation:** Minecraft can't move real block structures smoothly, and moving
islands would lag and break. So:

- **A live comet is an entity, not blocks.** It's a glowing rock built from a few
  block-display parts, with a long particle tail. It crosses the outer End slowly, on a
  long arc that takes about 1–2 minutes.
- **What players can do with one:**
  - **Chase it with an elytra:** flying through the tail gives a speed boost and collects
    **Stardust Motes**.
  - **Hit the head** (like hitting a boat) to knock off chunks: Stardust, Comet Ice and a
    small chance of raw Meteoric Iron. Each comet can be hit only a limited number of
    times.
  - **Watch it land:** some comets end their arc by striking an island. On impact they
    become real blocks and leave a **small crater** (under ~40 blocks changed, respects
    `mobGriefing`, and never hits a player's claimed or built area). It contains
    Stardust, a Comet Core and sometimes a Temporal Crystal.
- **Rarity:** at most one live comet near a player at a time, and roughly one per
  10–15 minutes while you're in the Comet Wake. Far rarer elsewhere.
- **Large Comet Craters** are generated with the world, not made live, so big world edits
  stay safe.

**Verdict:** live comets are worth it in this limited form. They turn the sky into
something you can interact with, without the lag and bugs of moving islands.

---

## 8. Blocks, flora and items

### 8.1 New ores and raw materials

| Material | Where | How you get it | What it's for |
|---|---|---|---|
| **Void Crystal** | Void Crystal Ore in Umbral Stone (Dark Matter Belts) | Mine with an iron pickaxe or better. Fortune works. | Starforge Alloy, Portal Lens, Starforge upgrades |
| **Charged Void Crystal** | Void Crystal exposed to a Stellar Flare | Flare Charging (§7.1) | Advanced upgrades, powering stations |
| **Meteoric Iron** | Meteoric Iron Ore in Comet Cores (Comet Wake), live comets | Mine with a diamond pickaxe. Smelt the raw chunks. | Starforge Alloy, Gravity Wells, the Astral Forge |
| **Temporal Crystal** | Loot only: craters, station vaults, maze rewards | Can't be mined or farmed | Portal Lens, Temporal upgrade, Maw repair |
| **Stardust** | Stardust Blocks (Comet Wake), comet tails | Shovel, or fly through a comet tail | Building, Astral Glass, Comet Cress farms |
| **Orrery Bronze** | Salvaged from Orbital Stations | Mine station hull blocks | Station-style building, Telescope Mounts |

**Starforge Alloy** = 2 Void Crystal + 2 Meteoric Iron Ingot + 1 Glowing Spore, crafted
at the Astral Forge (§9).

### 8.2 Building blocks
Every stone type gets the full vanilla set: slabs, stairs, walls, polished, bricks and
chiseled.

- **Umbral Stone** (+ Polished, Bricks, Chiseled "Eclipse" pattern)
- **Pale End Stone** (+ Bricks, Tiles)
- **Charred End Stone**
- **Observatory Quartz** (+ Star Inlay variant)
- **Leviathan Bone** (+ Carved, Fossil Plate). This is the building material of the Maws.
- **Orrery Bronze** (+ Cut, Grate, Lamp).
  - It **tarnishes from bronze to violet over time**, like copper oxidising.
  - Waxing it with Glowing Spores stops the tarnishing.
- **Stardust Block**: falls up.
- **Astral Glass**: smelted from Stardust. A dark glass with a faint star speckle.
  - A rare **Starfield Glass** variant shows the End-portal starfield effect.
  - Starfield Glass is expensive and limited to a handful per build, for performance.
- **Comet Ice**
- **Nebula Moss** (a ground cover that spreads like moss) and **Moss Carpet**
- **Spore Lantern** (light level 13) and **Whalelight Scale Block** (light level 10)
- **Gravity Well**, **Void Trap** (Singularity Shard utility blocks)
- **Collector Pedestal**: charges Void Crystal during flares.

### 8.3 Cosmic plants

| Plant | Behaviour | Uses |
|---|---|---|
| **Lumineshroom** | Grows on Nebula Moss. It pulses brightness in a slow ~8-second "breath" cycle. Its actual light level only switches between two values every few seconds, so it doesn't cause lag. | Spore Lanterns, glowing stew, dye |
| **Astral Vine** | Grows **upward** from the undersides of blocks. Climbable. Shears stop growth. | Decoration, climbing up island edges |
| **Starbloom** | A flower that rotates to face the End's centre. Vanilla compasses and clocks spin uselessly in the End, so this is the End's only natural way to find your bearings. | Pollinator breeding, dye, **Star Compass** (points to the central island, works in the End) |
| **Shimmer Grass** | A short, swaying grass in the Aurora Shallows | Decoration, Pollinator food |
| **Comet Cress** | Grows only on Stardust, in any light | Food (hunger 3), light crop farming |
| **Gravlily** | A rare flower from Dark Matter Belt inversion pockets. It grows upside down. | Brewing: **Potion of Buoyancy** (slow fall + a small jump boost) |

### 8.4 Tools and utility items
- **Portal Lens** (Data Shard ×3 + Void Crystal + Temporal Crystal + Spyglass). A
  spyglass upgrade that:
  - shows distortion outlines (Umbral Stalkers, hidden ore glints);
  - points toward the nearest known Wormhole Maw;
  - reads Echo Logs from Data Shards;
  - is also the key used to wake and link Maws.
- **Telescope use:**
  - Constellation Mazes are designed around a telescope: star-bridges appear only while
    you look through it (§10).
  - **If Minecraft 26.3 has a vanilla telescope, the mod uses it directly.** If not, the
    mod adds a **Telescope Mount**: a placeable Orrery Bronze block you look through.
  - Either way, the Portal Lens also works, at shorter range.
- **Star Chart**: a lore map found in ruins or given by the Watcher.
- **Stalker Cloak**: a cape worn in the chest slot, like an elytra. Mobs lose track of
  you faster, but you give up both your chestplate and your elytra to wear it.
- **Glowing Stew**: Lumineshroom + Comet Cress + bowl. Gives a short Glowing and Night
  Vision effect.

---

## 9. Starforging: upgrades as self-expression

Starforging is the late-game upgrade system. **Every upgrade is strictly better than no
upgrade. The choice is which one.**

### 9.1 The rules
- A piece of gear holds **one Starforge upgrade**.
  - Armour, weapons, tools and elytra can all take one.
  - Diamond and netherite gear qualify.
- There are **six upgrades**. Each makes a *different* thing better, with no downside.
- You **can't** stack all six on one piece, so every piece is a decision: speed or
  safety? Loot or survival?
- **Swapping is allowed but costs materials,** so experimenting is possible, but
  commitment still means something.
- Upgrading is done at the **Astral Forge**, a new workstation (Meteoric Iron block +
  Observatory Quartz + Void Crystal). Each upgrade needs:
  - a **Starforge Template** (found in station vaults and maze rewards, duplicable like
    smithing templates);
  - **1 Starforge Alloy**;
  - the upgrade's **catalyst**.
- Starforged gear gets a subtle star-speckle shimmer, plus a colour accent for the chosen
  upgrade, so you can read someone's build at a glance.

### 9.2 The six upgrades

| Upgrade | Catalyst | Improves | On armour (per piece) | On tools and weapons |
|---|---|---|---|---|
| **Nebular** | Glowing Spores | **Mobility** | +4% movement speed, −15% fall damage | +15% mining speed / +10% attack speed |
| **Gravitic** | Meteoric Iron block | **Stability** | +0.1 knockback resistance, you can't be pulled by Black Hole Guardians or Stalker knockback | Hits knock enemies back further; pickaxes mine a 1×2 area while sneaking |
| **Temporal** | Temporal Crystal | **Tempo** | −10% cooldown on ender pearls, shields, wind charges and other item cooldowns | Tool durability slowly regenerates while in your inventory |
| **Singular** | Singularity Shard | **Collection** | Pulls nearby dropped items and XP toward you (radius grows with more pieces) | Mined blocks and mob drops go straight into your inventory |
| **Umbral** | Umbral Membrane | **Stealth** | Hostile mobs notice you from 10% shorter range; Endermen don't mind eye contact | +25% damage on hits from behind; quieter footsteps and mining |
| **Radiant** | Charged Void Crystal + Whalelight Scale | **Vitality** | +1 heart; full protection from flare burn with 2+ pieces | Hits heal you for a small amount (capped per second) |

**On elytra:** Nebular gives better glide, Temporal makes rockets last longer, Singular
lets you collect comet Stardust from further away, and Radiant makes you ride flare solar
wind harder. That's a separate choice from your chestplate.

### 9.3 Alignments: combo bonuses
On top of each upgrade's own effect, wearing certain **combinations** across your four
armour pieces unlocks a small extra bonus and a matching particle trail. This rewards
thinking about your whole kit, not just single pieces.

| Alignment | Pattern | Bonus |
|---|---|---|
| **Full Focus** | 4 of the same upgrade | That upgrade's armour effect gets +25% |
| **Binary Star** | 2 Nebular + 2 Gravitic | Sneak mid-air in low gravity for a short air dash |
| **Event Horizon** | Singular + Umbral + any 2 | Items you drop can't be picked up by mobs or lost to Black Hole Guardians |
| **Aurora** | Radiant + Nebular + Temporal + any | Pollinators follow you and drop spores on you; flares recharge your hunger instead of hurting |
| **Pulsar** | 4 different upgrades | Every 30 seconds, your next hit releases a small shockwave |

A specialist (Full Focus) and a mixer (Pulsar) are both valid, strong builds. That's the
judgement call.

---

## 10. Structures

| Structure | Where | What's inside |
|---|---|---|
| **Comet Crater** | Comet Wake, sometimes other outer islands | A bowl of Stardust and Charred Stone around a Comet Core. There's a loot cache at the centre with a chance of **Temporal Crystals**, Meteoric Iron and Star Charts. Some craters are still "hot", with ember particles and fire around the rim. |
| **Wormhole Maw** | Always in the Shattered Observatory, rarely elsewhere | See §10.1 |
| **Constellation Maze** | Shattered Observatory, rarely Nebula Fields | See §10.2 |
| **Abandoned Orbital Station** | High sky (Y 180+), mostly above Observatories | See §10.3 |
| **Observatory Ruins** | Shattered Observatory, rare in Nebula Fields | A domed ruin with star mosaics and a telescope made of blocks. Vaults (opened with Data Shards, like trial keys) hold Starforge Templates, Temporal Crystals and Echo Logs. It has the only guaranteed Astral Forge recipe book. |
| **Wanderer Relay** | Dark Matter Belt edges | A thin floating spire where Void Wanderers dock. It has no loot, but Wanderers spawn here, and its top holds an Echo Log. |
| **Whalefall** | Aurora Shallows, very rare | The fossilised ribs of a Leviathan, arched over an island. Whalelight Scales grow on the bones, and it has one lore chest. |

### 10.1 Wormhole Maws
The fossil skull of a Star Leviathan, **the size of a small mountain**. It sits on its own
island with its jaws half open toward the void. Rows of Leviathan Bone teeth ring a dark
throat, and the throat is filled with a slow, swirling **wormhole surface** (a starfield
like the End portal's, but spiralling).

- **Dormant:** jaws half closed, and the throat is dark stone.
- **Waking it:**
  1. Place **Charged Void Crystal** in the four "eye sockets".
  2. Use a **Portal Lens** on the throat.
  3. The jaws grind open with a deep groan, and the wormhole lights.
- **Travel:**
  - Walk into the throat to travel to **any other Maw you have woken**.
  - Each jump costs 1 ender pearl or 4 chorus fruit, which keeps it fair.
  - Arrival: you're "spat out" of the destination Maw's mouth with a gust.
- **Discovery:** Star Charts and the Watcher point to unwoken Maws. The network grows as
  you explore.

### 10.2 Constellation Mazes
A ring of broken platforms floating in open void around a central dais. Most of the path
between them is **invisible**.

- **The puzzle:**
  1. From a **telescope point** on the dais, look at the sky. Specific constellations are
     picked out in the starfield: the Leviathan, the Maw, the Wanderer, and others.
  2. While you look at a constellation through the telescope, a matching path of faint
     **star-bridges** lights up in the world below for about 20 seconds.
  3. Walk (or float) them before they fade. They're solid while lit.
  4. Each platform you reach holds the next telescope point, pointed at a different part
     of the sky.
- **Difficulty:** later mazes add Gravity Inversion Pockets, bridges that show only one
  constellation's stars, and Umbral Stalkers.
- **Safety:** a fall drops you onto a "catch" platform far below, not into the void.
  Mazes should challenge, not grief.
- **Reward (at the centre vault):** a Starforge Template, a Temporal Crystal, and a Star
  Chart revealing a Maw. Each maze also unlocks a constellation you can then see in the
  sky everywhere, like a collection.

### 10.3 Abandoned Orbital Stations
What it would look like if ancient aliens built in Minecraft's style: a giant **ring** or
**spoked wheel** hanging high in the sky.

- **Materials:** Orrery Bronze, Astral Glass and Leviathan Bone. It's tarnished violet
  with age, overgrown with Astral Vines, and very quiet.
- **Getting there:** there's no stair or ladder. You need an elytra, pearls or a Gravity
  Inversion "lift" from the island below.
- **Artificial gravity:** the ring's floor is on the *inside* of the wheel.
  - Gravity-plating blocks point "down" toward the outer rim, so as you walk around the
    ring, the whole world rotates relative to you.
  - This is the gravity-inversion system reused, so it isn't a new engine feature.
- **Rooms:**
  - **Docking bay:** where Void Wanderers park and charge. Many Wanderers live here, and
    they're the easiest place to be scanned for Data Shards.
  - **Hydroponics:** overgrown Lumineshroom and Comet Cress beds; seeds and spores.
  - **Crew quarters:** Echo Logs describing the Cartographers' last days.
  - **Solar core:** sealed. **It powers up only during a Stellar Flare.** When it does,
    lights come on, doors unlock, and the central **vault** opens for the flare's
    duration.
- **Vault loot:** Starforge Templates, Temporal Crystals, Charged Void Crystal and the
  Starfield Glass recipe.
- **Station Sentinel (mini-boss):** it guards the vault and wakes with the power. It's a
  large, slow Wanderer-style automaton.
  - It uses a Wanderer's scanning beam as its attack wind-up, so reading the beam is the
    fight.
  - It drops a guaranteed Data Core, used to craft a **Wanderer Beacon** that calls a
    friendly Wanderer.

---

## 11. Progression

1. **Kill the dragon** (unchanged), then head out through the End Gateway.
2. **The first ~1,000 blocks:** vanilla End Cities, an elytra, and shulker boxes.
3. **Nebula Fields:** your first new biome. Pollinators, glowing flora and low gravity.
   Build an outpost here.
4. **First Stellar Flare:** learn to shelter, and learn that flares are an opportunity.
5. **Comet Wake:** mine Meteoric Iron, chase live comets, and raid craters for Temporal
   Crystals.
6. **Dark Matter Belt:** collect Void Crystal, meet the Umbral Stalker, fight Black Hole
   Guardians, and let Void Wanderers scan you for Data Shards.
7. **Craft a Portal Lens and build the Astral Forge.**
8. **Shattered Observatory:** solve Constellation Mazes for Starforge Templates, and wake
   your first Wormhole Maw.
9. **Orbital Stations:** reach one, wait for a flare, and fight the Sentinel for the
   vault.
10. **Long tail:** link every Maw, complete the Drift Whale chain, collect every Echo Log
    and constellation, and experiment with Starforge builds and Alignments.

**Advancement tab: "The Astral Reaches".** About 25 advancements, including:
- *Falling Upward*: use a Stardust block.
- *Hold Still*: get scanned by a Void Wanderer.
- *Weather the Storm*: survive a Stellar Flare outdoors.
- *Tail Chaser*: collect Stardust from a comet's tail.
- *Connect the Dots*: finish a Constellation Maze.
- *Say Ahh*: wake a Wormhole Maw.
- *Houston?*: power an Orbital Station.
- *Something Looks Back*
- *Leviathan*

---

## 12. Balance and anti-farming rules

- **Starforging is an upgrade, but one per piece.** Its power is capped by slot count,
  and every upgrade targets a different stat, so no single "correct" build exists.
- **The rarest rewards are one-time or limited:**
  - the Leviathan disc comes from an advancement;
  - Temporal Crystals are loot only;
  - Singularity Shards need a manual timing kill;
  - comets have limited hits.
- **Data Shards** come from a non-violent interaction that's limited to once per
  Wanderer per player.
- **Flares can't be triggered on demand.** Charging Void Crystal takes planning, not a
  farm.
- **Low gravity, gravity inversion, flare damage and live comet landfall** can each be
  turned off in the config.
- **Black Hole Guardians never delete items** and never break blocks.
- **Maws cost ender pearls per jump,** so they don't replace elytra travel entirely.

---

## 13. Performance budget

| System | Approach |
|---|---|
| Sky (stars, auroras, background comets, silhouettes, flare light) | Drawn in the End sky renderer; no entities, no world data |
| Stellar Flare | One server-wide timer. The burn check runs once per second per player: "is there sky above?" |
| Live comet | One entity per player area, with a few block-display parts and particles. Block changes happen only on landfall (<40 blocks). |
| Lumineshroom "breathing" | The visual pulse is an animated texture. The real light level changes only between 2 values, at most every few seconds, on random ticks. |
| Biome gravity | A player-only check about once per second that adds or removes an attribute modifier |
| Gravity inversion and station gravity plating | Small zones generated with the world; entities check only when inside a chunk that has one |
| Constellation star-bridges | Temporary blocks placed and cleared by the maze, only while a player is looking through the telescope |
| Drift Whale | One per player area. It doesn't collide with blocks, uses a simple model, and despawns aggressively. |
| Black Hole Guardian pull | Checks nearby entities every 5 ticks, within a radius of 6 blocks |
| Singular upgrade magnet | Checks every 10 ticks, within a radius of 3–6 blocks |
| Starfield Glass | Uses the end-portal effect; limited by its crafting cost |

---

## 14. How to build it (implementation roadmap)

Each phase is playable on its own. Later phases build on earlier ones.

| Phase | Content | Main technical work |
|---|---|---|
| **1. Blocks and flora** | Umbral Stone set, Stardust (falls up), Astral Glass, Orrery Bronze (tarnishing), Leviathan Bone, Lumineshroom, Astral Vine, Starbloom, Comet Cress, Void Crystal and Meteoric Iron | Registering blocks and items (like the Ruby), a custom `FallingBlock` subclass that falls up, and a tarnishing block modelled on vanilla's copper. Mostly JSON. |
| **2. First biome: Nebula Fields** | The biome, its terrain features and low gravity | The End's biome list is **hard-coded in vanilla**. Adding biomes needs a custom `BiomeSource` registered in Java, then a data-pack override of `dimension/the_end.json` to use it. Biomes and features themselves are JSON. |
| **3. Remaining biomes** | Aurora Shallows, Comet Wake, Dark Matter Belt, Shattered Observatory, inversion pockets | More JSON worldgen, plus Java for inversion pockets |
| **4. Mobs** | Pollinator, Void Wanderer, Black Hole Guardian, Umbral Stalker, then Drift Whale and the Watcher | Entity classes, AI goals, models (Blockbench → Java model code), spawn rules |
| **5. Sky and Stellar Flares** | Moving stars, auroras, silhouettes, flare phases, burn, solar wind, flare charging | A custom `DimensionSpecialEffects` for the End (client), a saved server-side flare timer, a network packet to sync flare state |
| **6. Structures** | Comet Crater, Observatory Ruins, Wanderer Relay, Whalefall, Wormhole Maw, Constellation Maze, Orbital Station + Sentinel | Structure templates built in-game and saved as `.nbt`, plus jigsaw JSON. Maw linking, maze bridges and station power need Java. |
| **7. Live comets** | The comet entity, tail harvesting, landfall craters | An entity with block-display parts, a flight path, and a small crater placer |
| **8. Starforging** | Astral Forge, templates, the six upgrades, Alignments | Data components on items, a menu and screen for the Forge, attribute modifiers, event listeners |
| **9. Polish** | Advancements, Echo Logs, music, config toggles, balance passes | JSON advancements, a Forge config file, playtesting |

**Suggested first task:** Phase 1's **Stardust Block** and **Lumineshroom**. They use
the same registration pattern as the Ruby Block, add one small twist each (falling
upward; an animated texture), and prove out the art style before any worldgen work.

---

## 15. Open questions

1. **Telescope:** does Minecraft 26.3 have a vanilla telescope? Check by opening
   `~/minecraft-source` and pressing Cmd+P, then typing `Telescope`. If it exists,
   Constellation Mazes use it directly; if not, the mod adds a Telescope Mount.
2. **Mod name and ID:** `astralreaches` in a new repository, or rename `rubymod` and
   build it here?
3. **Where the new biomes start:** beyond 1,000 blocks, or only beyond 2,000–3,000
   blocks, so early End exploration stays exactly vanilla?
4. **Flare damage:** keep it (shelter matters) or make flares purely an opportunity,
   with no damage?
5. **Is one hostile mob plus the Sentinel enough,** or should Comet Wake get a small
   threat too (for example, burning "Cinder Mites" in hot craters)?
6. **Art:** should textures be drawn first (in Blockbench/Piskel), or should we start with
   placeholder textures and replace them later?
