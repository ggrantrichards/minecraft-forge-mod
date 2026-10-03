# The Astral End: an End update

A design plan for expanding the End into a sparse, mysterious network of floating
"astral" regions, built as a Forge mod for Minecraft 26.3. Nothing here is final; every
biome, mob and number is a starting point for playtesting.

**Mod:** *The Astral End*, mod ID `astralend`, built in this repository.

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
constellations and the behaviour of mobs.

> **Phase 9:** *Echo Logs were cut.* Mojang never puts readable lore in the game - the Ancient
> Cities and Trail Ruins say nothing in words - so neither does this. Where the rows below mention
> Echo Logs, the hint now lives only in what the world shows.

- **The Star Leviathans.** Long ago, colossal creatures swam between the stars, and they
  could swallow a traveller and carry them from one star to another.
  - **Drift Whales** are their last, much smaller descendants.
- **The Cartographers.** A people who learned to ride the Leviathans. They mapped the
  sky, built **Orreries** as waystations, and made the End their crossroads: a
  plaza of stone floating between the stars.
- **The Maws.** When a Leviathan died, the Cartographers kept its skull open. Its mouth
  still led where the creature had last travelled. These fossil skulls are the
  **Wormhole Maws**, the gates of the star road.
- **The collapse.** Something **collapsed a star** near the crossroads. The blast
  shattered the plaza into the islands we know and left scars that are still there:
  - the **Dark Matter Belts**, where space itself is bruised;
  - the **Void Comets**, fragments of the dead star, still orbiting;
  - the **Black Hole Guardians**, small tears that open and close on their own;
  - **the Remnant**: the dead star's ember still hangs over one far region of the End.
    It pulses, and its **Stellar Flares** scorch the islands nearest to it, the
    **Corona**. Life there either learned to feed on the flares or died out.
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
- **Constellations**: the star patterns in Constellation Mazes (§10) are the
  Cartographers' route markers. Solving one teaches a new Star Chart.
- **Architecture**: ruins in a style unlike End Cities, with rounded arches, star inlays,
  Leviathan bone and tarnished violet bronze.

### 2.1 Staying true to vanilla

The new lore is built to **explain** what vanilla already shows, never to contradict it.
Vanilla facts it hooks into:

| Vanilla fact | How the Astral End explains or uses it |
|---|---|
| The End has **no day/night cycle, no weather**, and a static sky texture | Unchanged across almost all of the End: no weather, a sky that moves only very slowly. The one exception is the **Corona** biome, near the Remnant, which has Stellar Flares. |
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
| The End was first imagined as a floating **"Sky Dimension"** during early development | The Orreries in the high sky are a quiet nod to it. |
| The **End Poem** plays after the first dragon kill, with two unseen voices discussing the player | The Watcher can be read as one of those voices, or not. **The mod never quotes or explains the End Poem;** it only leaves room for that reading. |

**Rules for new content:**
- Vanilla mobs, blocks and the dragon fight behave exactly as before.
- New lore may add meaning, but it never "solves" a vanilla mystery outright.
- No text tells the story: the world does (Phase 9).

---

## 3. The map: how the End is laid out

Vanilla today has a central island with the dragon, then (1,000+ blocks out) outer islands
in four biomes: Highlands, Midlands, Barrens and Small Islands.

**The plan:**
- **The central island is untouched.** The dragon fight stays vanilla.
- The first **~1,000 blocks** of outer End keep the vanilla biomes, so End Cities and
  elytra hunting feel the same as always.
- Beyond that, the outer End becomes a **patchwork of vanilla biomes and six new astral
  biomes**, placed by a noise map. New biomes get rarer the further they are from the
  vanilla feel.
- **Void is still the dominant biome.** New biomes never raise island density above
  vanilla Highlands. Travel between them should still take elytra, pearls, bridges or a
  Wormhole Maw.
- **The high sky (Y 180+) is new territory.** Orreries hang up there, far above
  the islands, so players have a reason to look (and fly) up.

| Biome | Rarity | Mood | Main purpose |
|---|---|---|---|
| **Nebula Fields** | Common | Calm, glowing, dreamy | Flora, pollinators, the most livable biome |
| **Aurora Shallows** | Uncommon | Quiet, wide, windy | Drift Whales, open building space |
| **Comet Wake** | Uncommon | Scarred, sparkling | Stardust, Meteoric Iron, craters, live comets |
| **Dark Matter Belt** | Rare | Tense, dim, disorienting | Void Crystal, gravity inversion, danger |
| **Shattered Observatory** | Very rare | Ancient, sacred | Lore, Wormhole Maws, Constellation Mazes, the Watcher |
| **The Corona** | Rare | Scorched, blinding, rich | The only place with Stellar Flares. High-risk, high-value: Solarite, Charged Void Crystal, Flare Bottles |

---

## 4. Biomes

### 4.1 Nebula Fields
Soft, low islands wrapped in coloured fog (violet into teal), with faint drifting motes
in the air.

- **Built (Phase 2):** regions several hundred blocks across covering about a fifth of the
  outer End, placed by their own noise on top of vanilla's End biomes.
- **Rebuilt as a world of its own (Phase 3.5):** territories a few thousand blocks across, on big
  rolling moss islands; forests of **giant Lumineshrooms** (glowing caps up to 15 blocks high,
  like the Nether's huge fungi) with **Nebula Tendrils** dripping from their rims and from island
  undersides; a carpet of **Nebula Tufts**. The giant stems give **Lumine wood** (a full wood set,
  like crimson and warped).
- **Terrain:** vanilla End islands, with every island floor topped with **Nebula Moss** (a
  material rule). No chorus grows here, since chorus needs bare End Stone.
- **Low gravity:** players and mobs get **0.65× gravity**, which makes them jump about
  **1.5× higher** and fall more slowly, plus 3 extra blocks of safe fall. Ender pearls thrown
  here fly about 20% farther.
  - Uses vanilla's `gravity` and `safe_fall_distance` attributes, checked once a second.
  - Applies in biomes tagged `#astralend:low_gravity`; a datapack can empty the tag to turn it
    off.
- **Flora:**
  - **Lumineshrooms**: glowing mushrooms that "breathe", slowly pulsing brighter and
    dimmer.
  - **Astral Vines**: vines that grow upward from the moss, like twisting vines. They're
    climbable.
  - **Starblooms**: flowers that always face the central island.
- **Mobs:** End Pollinators, rare Drift Whales passing through.
- **Feel:** the place you'd build your End base. It should be safe and beautiful, but
  still quiet and far from everything.

### 4.2 Aurora Shallows
Huge, flat, thin "shelf" islands close to the void, under a sky full of slow aurora
ribbons.

- **Phase 3.5:** big shelf landmasses, a meadow of Shimmer Grass with tall **Shimmer Reeds**, and
  whole **Leviathan ribcages** (a spine with rib arches over it) lying on the shelves.
- **Built (Phase 3):** islands reshaped into flat-topped shelves (level at y=54, thinning to a
  see-through edge) of **Pale End Stone**, with **Shimmer Grass**, standing stones of Pale End
  Stone Bricks and Tiles, and Leviathan Bone ribs. Aurora skies and Drift Whales come in
  Phases 5 and 4; Whalefalls in Phase 6.

- **Terrain:** wide plates of **Pale End Stone**, with thin edges you can see through
  from below. There are patches of **Shimmer Grass** (a short, swaying plant) and
  standing stones.
- **Sky:** the strongest auroras in the End.
- **Mobs:** **Drift Whales** spawn here more than anywhere else. Pollinators visit the
  Shimmer Grass.
- **Structures:** **Whalefalls**, the fossilised ribs of old Leviathans.
- **Purpose:** flat building space, and the best place to hear the End's new music.

### 4.3 Comet Wake
The trail a comet left when it broke apart: scorched islands, a glittering "debris river"
of small floating chunks, and craters.

- **Phase 3.5:** broad scorched islands pocked with **impact craters** (a lump of Meteoric Iron at
  the bottom of each) and **Comet Ice spires** driven into the ground at an angle.
- **Built (Phase 3):** **Charred End Stone** islands with a field of small floating debris
  between them, **Stardust** veins, glassy **Comet Ice** shards, **Comet Cores** (boulders with a
  heart of Meteoric Iron Ore), and Stardust gathered under island undersides with **Comet
  Cress** hanging from it. Craters come with the other structures in Phase 6.

- **Terrain:**
  - **Charred End Stone**.
  - Veins of **Stardust Blocks**, a sand-like block that **falls up** instead of down.
    It's great for building hanging structures.
  - Shards of glassy **Comet Ice** that never melt and are slippery like blue ice.
- **Comet Cores:** big boulders embedded in islands. Their centre holds **Meteoric Iron**
  ore.
- **Comet Craters** (§10) are common here.
- **Live comets** (§7.2) pass through most often here.
- **Flora:** **Comet Cress**, a small white sprout that hangs from the underside of Stardust. It's
  edible.

### 4.4 Dark Matter Belt
Near-invisible islands. You can only really see them from their edges, where light
bends.

- **Phase 3.5:** ring archipelagos (land follows broken rings and arcs around empty centres) under
  dense forests of twisted **Nullwood** trees: black bark, violet heartwood, dark leaves hung with
  glowing **Null Lanterns** (the forest's only light). A full wood set; its saplings grow on End
  ground.
- **Built (Phase 3):** ragged ring-shaped islands (big islands are hollowed out) of **Umbral
  Stone** with **Void Crystal Ore**, near-black fog, and **Gravity Inversion Pockets**: a
  **Gravity Anomaly** set into an island's underside, with a sphere of flipped gravity (60%
  strength, pointing up) below it, marked by rising motes. It uses vanilla's gravity attribute,
  so players and mobs both fall upward to the island above. Turned off by emptying the
  `#astralend:gravity_inversion` biome tag. Gravlilies arrive with the other plants' uses later.

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

- **Phase 3.5:** larger, rare regions on one big island, with the **great ring**: a circle of tall
  quartz pillars and lintels around a star mosaic, mostly fallen.
- **Built (Phase 3):** small, rare regions of vanilla-shaped islands with **Observatory Quartz**
  ruins set into them (round floors with a **Star Inlay** at the centre, broken and fallen
  pillars), overgrown with Starblooms, Astral Vines and chorus. The great ring, Wormhole Maws
  and Constellation Mazes are structures, in Phase 6.

- **Terrain:** polished stonework ruins merged into the islands, fallen pillars, and star
  mosaics set into the floor.
- **Structures:**
  - always one **Wormhole Maw**;
  - usually one **Constellation Maze**;
  - often an **Orrery** hanging in the sky above.
- **Flora:** overgrown with Astral Vines and Starblooms, as if someone once tended a
  garden here.
- **The Watcher** can only be met here, sometimes.

### 4.6 The Corona
The islands closest to **the Remnant**, the dying ember of the collapsed star. It hangs
low in the sky over this region as a huge, dim, slowly pulsing disc. You can see it from
far away, so players always know where the Corona is (and can choose to avoid it).

- **Phase 3.5:** flat-topped mesas with sheer cliffs, fields of Flareblooms over glowing lichen, and
  the **petrified stalks** of giant Astral Vines that died here, up to 20 blocks tall.
- **Built (Phase 3):** smoother, low-relief islands (few overhangs) with a crust of
  **Sunscorched Stone** over **Charred End Stone**; **Solarite Ore** in rich veins at the surface
  and sparse ones deep inside; **Flareblooms** (shut tight), glowing **Sunscale Lichen**, and the
  dead: **Ashen Lumineshrooms**, **Petrified Astral Vines** and **Pollinator Husks**. Since
  Phase 5 the Remnant hangs over it and the Stellar Flares burn it (§7.1).

- **The only place with Stellar Flares** (§7.1). Everywhere else in the End stays calm:
  no burning, no storms. Even at the Corona's border, a flare is only a bright glow on the
  horizon.
- **Terrain:**
  - **Charred End Stone** and **Sunscorched Stone**, a bleached, cracked stone.
  - Glassy streaks where flares melted the surface.
  - Few overhangs, so shelter isn't free: players have to build or dig it.
- **Life that adapted** (it thrives here):
  - **Flareblooms**: tall flowers that stay tightly shut and only open during a flare.
    Harvesting one while it's open gives **Flare Pollen**.
  - **Sunscale Lichen**: a ground cover that soaks up flares and glows for a while
    afterwards. A natural light source.
  - **Corona Skimmers** (§5.7): manta-like fliers that surf the flares.
  - **Cinder Mites** (§5.8): burrowing pests that emerge hungry after each flare.
- **Life that died out** (environmental storytelling):
  - **Ashen Lumineshrooms**: grey, lightless husks of the Nebula Fields' glowing
    mushrooms.
  - **Pollinator Husks**: bleached, fossil-like shells of End Pollinators that tried to
    settle here and failed. Found as a decorative block.
  - **Petrified Astral Vines**: stone-hard vines that stopped growing long ago.
  - No Pollinators, Drift Whales or Void Wanderers live here.
- **Why go (the Corona is where the best materials are):**
  - **Solarite** (§8.1), found nowhere else. **Rich veins sit on island surfaces,**
    exposed to flares. **Deep veins are safe but much sparser.** Surface mining is fast
    and risky; deep mining is slow and safe.
  - **Charged Void Crystal:** Void Crystal only becomes charged here, during a flare.
  - **Flare Bottles:** the batteries that power Orreries, charged only here.
  - **Solar Array Ruins** (§10): Cartographer mirror-farms that once harvested the
    flares, with some of the best loot in the End.
- **Feel:** the End's Nether. It's beautiful from a distance and dangerous up close.
  You come prepared, work fast between flares, and leave rich.

---

## 5. Mobs

> **Built (Phase 4):** all eight, with models, animations, glowing layers, spawn eggs, drops and
> the advancements below. Since Phase 5, Cinder Mites swarm out in each flare's afterglow and
> Skimmer wings refill then. Since Phase 6 the Star Chart marks a structure (a region of another
> biome only if no structure is in reach). Adapted until later phases exist: the Drift Whale's
> "Leviathan" music disc waits for the music of Phase 9. A pollinated plant spreads to the ground
> beside it (the End's plants have no growth stages). The Spore Lantern is a glowing block, and it
> (like any light of 7 or more) reveals Umbral Stalkers.

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
- **Never found in the Corona.** They tried to settle there and died out; their husks
  still litter the islands.

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
> **Revamped in Phase 9.** It was a "survey drone" with a scanning beam and a laser - space-station
> tech, which isn't Minecraft. Now it's a clockwork construct of the Cartographers, in the same family
> as the copper golem and the breeze: magic and craft, not machinery.

A floating clockwork construct: a rounded bronze body gone green with verdigris, void crystal set
in its seams, one great glass lens, a crystal-tipped finial and a gimbal ring that spins round it.
The Cartographers built them to chart the dark, and some still do.

- **Behaviour:**
  - It drifts the edges of Dark Matter Belts and Observatory ruins, and roosts in Abandoned Orreries.
  - When it notices a player, it floats over and **studies** them: a ring of light sweeps up and down.
  - **If you stand still and let it finish (5 seconds), it leaves you a Chart Fragment** - a shard of
    the star map it carries - and drifts away. This rewards curiosity instead of violence.
  - Each Wanderer studies each player once.
  - If you attack it, it turns hostile and looses **starbolts** (homing shulker bullets: dodge them
    or knock them away), and blinks away when low. Killing it drops its **Cracked Lens**.
- **Chart Fragments** open Observatory vaults (like trial keys) and go into the Portal Lens, with the
  Cracked Lens.

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
The End needs one real new threat. This is a hunched, almost invisible brute that lives
in the Dark Matter Belts: it walks on its knuckles like a gorilla, with dark-matter crystals
growing from its back and a violet rift glowing in its chest.

- **Visibility:**
  - Until it picks a target it is completely invisible in the dark. Once it is hunting, a
    faint shimmer outline shows, plus distortion particles when it moves.
  - It becomes fully visible while attacking, when hit, or when lit by a Spore Lantern or
    seen through a Portal Lens.
- **Behaviour:**
  - It stalks the player from behind and retreats when the player turns to face it (the
    reverse of an Enderman).
  - When it reaches you, it hits hard and tries to knock you toward the void.
  - It hunts like a lion: hidden, it creeps in low and slow, and from a few blocks behind
    an unsuspecting player it pounces - a leap that reveals it and lands a heavy ambush blow.
    Then it fights in the open briefly and melts back into the dark.
- **Drops:** **Umbral Membrane**, used for the Stalker Cloak and the Voidwalker upgrade.
- **Counterplay:** light (spores), a Portal Lens, or keeping your back to a wall.

### 5.6 The Watcher (unique, never fought)
A floating sentinel of pale observatory stone with one great eye - as if one of the
Observatory's own instruments had started looking back. It hangs in the air at the very edge
of render distance, on islands in the Shattered Observatory, with stone shards circling it.

- **Behaviour:**
  - It appears only when you're alone, only at a distance, and only for a few seconds.
  - If you look at it through a **spyglass or telescope**, it comes apart into starlight. A new Star Chart
    then appears in your inventory, pointing to the nearest undiscovered Maw, Maze or
    Station.
- **It never attacks and can't be reached.** It gives the advancement
  *Something Looks Back*.
- **Purpose:** mystery and guidance. It's the thread that pulls players deeper into the
  lore.

### 5.7 Corona Skimmer (passive, Corona only)
A flat, manta-shaped flier with thin, glowing wing-membranes. It evolved to surf the
flares.

- **Behaviour:**
  - Between flares, Skimmers rest flat on island surfaces and look almost like stone.
  - During a flare, they lift off in flocks and ride the light.
  - In the afterglow, their wings are "full" and shimmer.
- **Drops:**
  - **Shear them during the afterglow** (like sheep) to get **Skimmer Film**, a thin,
    heat-proof membrane. It grows back after the next flare. No killing needed.
  - Killing one drops nothing useful.
- **Uses of Skimmer Film:**
  - the **Potion of Flare Ward**;
  - **Sunshades** (§8.2).
  The life that adapted to the flares gives players the means to survive them.

### 5.8 Cinder Mite (hostile, Corona only)
Small, glowing burrowers, about the size of an Endermite. They hide in Sunscorched Stone.

- **Behaviour:**
  - They **emerge in swarms of 3–6 right after a flare**, hungry.
  - They go for players and set them on fire briefly.
  - They burrow back into stone after about a minute, so they're a timed pressure, not
    a constant threat.
  - They're immune to fire and Flare Burn.
- **Drops:** **Cinder**, a small fuel item (smelts 4 items). Also used in Solar
  Lanterns.
- **Counterplay:**
  - Water puts them out (they die in 2 seconds).
  - A sealed shelter keeps them from reaching you.

---

## 6. Atmosphere and the sky
> **Built (Phase 5):** the two-layer starfield, biome auroras, the Remnant over the nearest Corona,
> background comets and rare silhouettes (a whale, a ring). Biome ambience, music and the
> visual toggles come with Phase 9.

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
- **Music:** *cut in Phase 9.* New tracks and a "Leviathan" disc would need real composed music;
  without it, a disc would be a placeholder, which is worse than none.
- **Config:** *cut in Phase 9.* Every sky effect is drawn cheaply in the sky renderer (§13), so there's
  nothing to switch off; the gameplay switches are data-pack tags (§15), the way vanilla does it.

---

## 7. Events

### 7.1 Stellar Flares (Corona only)
> **Built (Phase 5):** the whole cycle as described below, Flare Burn (armour doesn't help; cover,
> Sunshades and Flare Ward do), Flareblooms opening (Flare Pollen), Sunscale Lichen charging, Skimmers
> riding the flare and refilling in the afterglow, Cinder Mite swarms, the solar wind for elytra,
> and Collector Pedestals charging Void Crystal and Flare Bottles. Not yet: the Radiant upgrade (Phase 8)
> and the config file (Phase 9); `/astralend flare` lets operators set the phase.

The Remnant pulses about every **15–25 minutes**. **Flares happen only in the Corona
biome.** Everywhere else in the End, including the vanilla central island and all other
new biomes, there is no flare, no burning and no storm. From elsewhere, a flare appears
only as a brief glow on the horizon where the Remnant hangs.

**The three phases:**

| Phase | Length | What happens in the Corona |
|---|---|---|
| **Warning** | ~1 minute | The Remnant brightens, Flareblooms twitch, Skimmers stir, and a low rising hum plays. There's always time to get protected. |
| **Flare** | 1–2 minutes | Sheets of light roll across the sky, and charged particles stream sideways. Flareblooms open and Skimmers take off. |
| **Afterglow** | ~2 minutes | Everything glows. Sunscale Lichen lights up, Skimmers can be sheared, and **Cinder Mites** swarm out of the stone. |

**Flare Burn: only in the Corona, and only if you're unprotected.**
- Anyone with open sky above them during the flare gets **Flare Burn**. It deals half a
  heart every 3 seconds at first, then a full heart after 30 seconds of exposure.
- It never hits you instantly, and it never happens outside the Corona.
- **Ways to deal with it** (any one is enough):
  - **Cover:** any solid block or glass overhead. Astral Glass lets you watch safely.
    Digging into an island counts.
  - **Sunshade:** a quick-to-place canopy block made from Skimmer Film. You drop it
    above you while mining surface veins, so you don't have to abandon your spot.
  - **Potion of Flare Ward:** Awkward Potion + Skimmer Film. Lasts 3 minutes, or 8
    minutes when extended.
  - **Radiant Starforge upgrade** (§9) on 2 or more armour pieces. The late-game
    solution for regular Corona miners.

**What flares are good for (why you'd stay):**
- **Flare Charging:** Void Crystal becomes **Charged Void Crystal** if it's on a
  **Collector Pedestal** under open sky in the Corona during a flare. You bring the
  crystal in from the Dark Matter Belts, so you have to guard your pedestals while they
  charge.
  - Charged Void Crystal is needed for Maws and the Radiant upgrade.
- **Flare Bottles:** empty cells placed on a Collector Pedestal fill up. They power
  Orreries anywhere in the End (§10.3).
- **Flareblooms open**, giving Flare Pollen.
- **Skimmers become shearable** in the afterglow.
- **Solar wind:** inside the Corona, elytra flight gets a strong push in the flare's
  direction.
- **Radiant armour** charges 5× faster.

**Config:** flare frequency, Flare Burn (on/off) and flare sky effects can each be
adjusted.

### 7.2 Live comets and meteors
> **Built (Phase 7):** as described below. The comet is one entity with its own model (a rock in a
> coma of two turning shells of light, with a widening tail) and a particle tail, rather than block
> displays. A landfall crater is a bowl with a thrown-up rim, an ice-glazed floor and the Comet Core in
> the middle; "built area" means any block entity, planks, glass, wool, doors, beds, rails or torches
> within 6 blocks. Comets aren't saved with the world.

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
| **Charged Void Crystal** | Void Crystal charged during a flare, only in the Corona | Flare Charging (§7.1) | Wormhole Maws, Radiant upgrade |
| **Solarite** | Solarite Ore, only in the Corona. Rich exposed surface veins, sparse deep veins. | Mine with a diamond pickaxe. Smelt the raw chunks. | Radiant upgrade, Flare Bottles, Solar Lanterns, Sunshade frames |
| **Flare Bottle** | Crafted from Solarite and Astral Glass, then charged during a Corona flare | Collector Pedestal in the Corona | Powers Orreries |
| **Meteoric Iron** | Meteoric Iron Ore in Comet Cores (Comet Wake), live comets | Mine with a diamond pickaxe. Smelt the raw chunks. | Starforge Alloy, Gravity Wells, the Astral Forge |
| **Temporal Crystal** | Loot only: craters, Orrery vaults, maze rewards | Can't be mined or farmed | Portal Lens, Temporal upgrade, Maw repair |
| **Stardust** | Stardust Blocks (Comet Wake), comet tails | Shovel, or fly through a comet tail | Building, Astral Glass, Comet Cress farms |
| **Orrery Bronze** | Salvaged from Orreries | Mine station hull blocks | Station-style building, lamps and grates |

**Starforge Alloy** = 2 Void Crystal + 2 Meteoric Iron Ingot + 1 Glowing Spore, crafted
on a crafting table like a Netherite Ingot (decided in Phase 8: the Forge's own slots are for
upgrading).

### 8.2 Building blocks
> **Phase 9:** this list now says what was built. Ideas that didn't make it are marked *cut*, with why.

- **Umbral Stone** (+ Polished, Bricks, Chiseled, stairs, slabs, walls), like deepslate.
- **Pale End Stone** (+ Bricks, Tiles, stairs, slabs, walls).
- **Charred End Stone** (+ stairs, slab, wall), **Sunscorched Stone** (+ Polished, Bricks, stairs,
  slabs, walls).
- **Observatory Quartz** (+ Pillar, Star Inlay, stairs, slab, wall).
- **Leviathan Bone Block** (rotates like a bone block). *Carved and Fossil Plate variants: cut* - the
  bone block already does the job, as vanilla's does.
- **Orrery Bronze**, with copper's whole family since Phase 9: **Cut Orrery Bronze**, its **stairs** and
  **slabs**, and **Orrery Bronze Grates**, each in three stages (bright, tarnished, weathered violet). It
  **tarnishes over time** like copper oxidising; an axe scrapes it back a stage; **honeycomb waxes it**
  (like copper, rather than the Glowing Spores first planned), and an axe takes the wax off. Plus the
  **Orrery Lamp**, **Bronze Shutters** and the **Orrery Heart** (Orrery-only).
- **Stardust Block**: falls up, drifting until it hits something. **Astral Glass**: smelted from
  Stardust. *Starfield Glass: cut* - an end-portal-effect glass needs its own renderer and only
  duplicates the End Portal's look.
- **Comet Ice**, **Nebula Moss** and (Phase 9) **Nebula Moss Carpet**.
- **Spore Lantern** (light 15) and **Whalelight Scale** (light 10).
- **Collector Pedestal**: fills a Flare Bottle or charges Void Crystal during Corona flares.
- **Sunshade**: a canopy (Skimmer Film over Solarite) that blocks Flare Burn.
- **Sunscale Lichen**, **Pollinator Husk**, **Ashen Lumineshroom**, **Petrified Astral Vine** (decoration).
- *Gravity Well and Void Trap: cut* - Singularity Shards became a Starforge material instead, and
  "utility" blocks that move mobs about were the least Minecraft-like part of the plan.
- *Solar Lantern: cut* - a third full-brightness light would add nothing to the Spore Lantern; Cinder
  got a second use instead (it makes fire charges, like blaze powder).

### 8.3 Cosmic plants

| Plant | Behaviour | Uses |
|---|---|---|
| **Lumineshroom** | Grows on Nebula Moss, End Stone or Umbral Stone. Its cap "breathes" (an animated texture); its light stays at 10. | Spore Lanterns, Glowing Stew |
| **Astral Vine** | Grows **upward**. Climbable. Bone meal grows it. | Decoration, climbing up island edges |
| **Starbloom** | A star-shaped flower that turns toward the End's centre. | Yellow dye, Night Vision suspicious stew, Pollinator breeding, the **Star Compass** |
| **Shimmer Grass**, **Shimmer Reeds**, **Nebula Tufts**, **Nebula Tendrils**, **Flarebloom** | Ground cover and flowers of their biomes (§4). | Decoration; Flarebloom gives orange dye and Flare Pollen |
| **Comet Cress** | Hangs from the underside of Stardust and grows downward, in any light. | Food, Glowing Stew |
| *Gravlily* | *Cut* - a flower that only grows inside inversion pockets is too rare to find and too fiddly to farm, and the potion it made duplicated Slow Falling. | - |

### 8.4 Tools and utility items
- **Portal Lens** (a spyglass, a Void Wanderer's Cracked Lens, Void Crystal, a Temporal Crystal and two
  Chart Fragments): the key that wakes Wormhole Maws and attunes you to them (§10.1). *Its other planned
  powers (outlines, a Maw finder, reading Echo Logs) were cut* - one clear job per item.
- ~~**Maze Telescope**~~: cut in Phase 11; the mazes are played with a vanilla spyglass (§15).
- **Star Chart**: found in ruins or left by the Watcher; points toward a biome or structure.
- **Star Compass** (Phase 9: a compass with four Starblooms round it): points to the End's central
  island, like a lodestone compass with no lodestone. Vanilla compasses spin in the End, so this is
  the way home.
- **Glowing Stew** (Phase 9: Lumineshroom, Comet Cress and a bowl): food, and 30 seconds of Night
  Vision. (Not Glowing as first planned: making yourself glow for others to see is a curse, not a meal.)
- *Stalker Cloak: cut* - a cape in the chest slot needs its own wearable renderer and competed with
  the elytra and with Starforging; the Umbral Membrane went to the Voidwalker upgrade instead.

---

## 9. Starforging: upgrades as self-expression

Starforging is the late-game upgrade system. **Every upgrade is strictly better than no
upgrade. The choice is which one.**

> **Built (Phase 8):** everything in this section. Choices made while building it: the Forge has five
> input slots (template, item, alloy, and one or two materials, since Voidwalker and Radiant take two);
> a Rift Pearl goes into a Voidwalker chestplate by right-clicking it on, like filling a bundle; "Lighter
> Gravity" and Radiant's room for golden hearts are ordinary attribute modifiers on the item, so they
> show on its tooltip; the sweep pull happens on a full-strength sword hit on the ground (the sweep's
> own conditions); Echo Strike is melee only. Starforged gear works the same on mobs that wear it,
> except the parts that need a player (Rift Walker, Void Recall, Soak Light).

### 9.1 The rules
- **Vanilla-style:** each upgrade does one clear thing that fits on one tooltip line, like
  an enchantment. It works in every dimension, not just the End.
- **Stacks per piece,** like Protection: more armour pieces with the same upgrade means a
  stronger effect.
- **No set bonuses.** Vanilla doesn't have them, so the choice is simply which upgrade
  goes on which piece.
- **What can be upgraded:**
  - diamond or netherite armour, weapons (swords, axes, bows, crossbows, tridents) and
    tools;
  - **not elytra.**
- **One upgrade per item.** Upgrades stack with enchantments, and each does something no
  enchantment does.
- **Not every upgrade fits every item,** as with enchantments. The table shows where each
  one applies.
- **How to apply one:** at the **Astral Forge** (Meteoric Iron block + Observatory Quartz
  + Void Crystal), using a **Starforge Template** (from Orrery vaults and maze rewards,
  duplicable like smithing templates), **1 Starforge Alloy**, and the upgrade's material.
  Applying a new upgrade replaces the old one.
- **Looks:** each upgrade adds a colour accent to the item, like an armour trim, so you can
  read someone's build at a glance.

### 9.2 The six upgrades

| Upgrade | Material | Armour | Weapons | Tools |
|---|---|---|---|---|
| **Voidwalker** | Void Crystal + Umbral Membrane | **Boots, Rift Walker:** step off an edge and a fading tile of rift appears under your feet, like Frost Walker's ice. **Chestplate, Void Recall:** holds one Rift Pearl; falling into the void uses it up and returns you to your last footing. | — | — |
| **Meteoric** | Meteoric Iron | **Heat Shield:** fire and lava don't hurt you for the first 2 seconds per piece (8 seconds with 4 pieces) | — | **Smelting:** mined blocks drop already smelted (ores become ingots, sand becomes glass). Works with Fortune. |
| **Stardust** | Stardust | **−5% gravity per piece:** higher jumps, softer landings. **Boots:** also step up full blocks. | **Bows, crossbows and tridents:** projectiles fly flatter and 25% further | — |
| **Temporal** | Temporal Crystal | **Time Stretch:** good potion effects last 10% longer per piece, and bad ones wear off 10% faster | **Echo Strike:** 25% of every hit repeats 1 second later | — |
| **Singularity** | Singularity Shard | **Lensing:** 10% chance per piece that an incoming projectile curves around you | **Swords:** sweeping attacks pull nearby mobs toward your target | **Pull:** drops and experience fly straight to you |
| **Radiant** | Solarite + Charged Void Crystal | **Soak Light:** in bright light, slowly gain golden hearts, up to 1 per piece. **2+ pieces:** immune to Flare Burn. | **Glow:** targets you hit glow for 5 seconds, visible through walls | — |

### 9.3 Details

**Voidwalker**
- **Rift Walker (boots):** tiles form only while you're walking or sprinting, one step
  ahead of you, and crumble after 1 second, the same way Frost Walker's ice melts.
  - Good for crossing short gaps between End islands, or a lava lake in the Nether.
  - Stop moving and you fall.
- **Void Recall (chestplate):** works like a Totem of Undying for the void.
  - The chestplate holds **one Rift Pearl**. When you fall into the void in any
    dimension, the pearl is used up and you're returned to your last footing.
  - **Rift Pearls are rare:** every boss drops one (Ender Dragon, Wither, Warden, Elder
    Guardian, Orrery Sentinel), and they're rare vault loot. That keeps Void Recall from
    being a free safety net.

**Meteoric**
- **Heat Shield:** named after the plating that lets meteors survive falling through the
  sky.
  - It's different from Fire Protection, which only reduces damage. Heat Shield gives a
    few seconds of full immunity, then the normal burn takes over.
  - The timer resets after 10 seconds out of fire.
- **Smelting:** includes raw iron, gold and copper, ancient debris, sand, cobblestone and
  logs (which become charcoal). Silk Touch overrides it.

**Stardust**
- It uses vanilla's own gravity and step-height stats, so the tooltip reads like any
  attribute:
  - "−5% Gravity" per piece;
  - "+0.5 Step Height" on boots.
- The bow effect lowers the projectile's gravity, not its speed. So arrows arc less and
  land further, but hit just as hard.

**Temporal**
- **Echo Strike:** the echoed hit happens even if the target has moved, with a chime and
  a clock-hand particle.
- **Time Stretch:** with 4 pieces, a Strength potion lasts 40% longer, and Poison or
  Wither wears off 40% faster.

**Singularity**
- **Lensing** works on arrows, tridents, Shulker bullets and fireballs. A curved
  projectile flies past you with a warp effect.
- **Sweep pull** moves mobs within 3 blocks 1–2 blocks toward your target, grouping them
  for the next sweep.
- **Pull** on tools matters most out in the End, where ore mined at an island's edge
  would otherwise fall into the void.

**Radiant**
- **Soak Light:** "bright" means light level 13 or more: daylight, glowstone, lava,
  Solar Lanterns and Corona flares.
  - You gain 1 golden heart every 20 seconds, up to 1 per Radiant piece.
  - Corona flares charge it 5× faster.
  - These are the same golden hearts a golden apple gives, and they absorb damage first.
- **Flare Burn immunity** with 2+ pieces makes Radiant the long-term answer for regular
  Corona miners.

## 10. Structures

| Structure | Where | What's inside |
|---|---|---|
| **Comet Crater** | Comet Wake, sometimes other outer islands | A bowl of Stardust and Charred Stone around a Comet Core. There's a loot cache at the centre with a chance of **Temporal Crystals**, Meteoric Iron and Star Charts. Some craters are still "hot", with ember particles and fire around the rim. |
| **Wormhole Maw** | Always in the Shattered Observatory, rarely elsewhere | See §10.1 |
| **Constellation Maze** | Shattered Observatory, rarely Nebula Fields | See §10.2 |
| **Abandoned Orrery** | High sky (Y 180+), mostly above Observatories | See §10.3 |
| **Observatory Ruins** | Shattered Observatory, rare in Nebula Fields | A domed ruin with star mosaics and a telescope made of blocks. Vaults (opened with Chart Fragments, like trial keys) hold Starforge Templates, Temporal Crystals and Charged Void Crystal. |
| **Solar Array Ruins** | The Corona | Fields of broken Cartographer mirrors that once gathered flare light, around a sunken chamber. The best loot in the End: Starforge Templates, Solarite blocks, Temporal Crystals, a guaranteed Bottled Flare. The mirrors focus flares, so Flare Burn is doubled on the open field. |
| **Wanderer Relay** | Dark Matter Belt edges | A thin floating spire with mooring rings where Void Wanderers rest. A landmark with no loot, like a fossil. |
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

### 10.3 Abandoned Orreries
> **Revamped in Phase 9** from the "Abandoned Orbital Station": a space station with crew quarters,
> hydroponics, a docking bay and a power core isn't something Mojang would build. An orrery - a
> clockwork model of the heavens - is: the Cartographers charted the stars, so their greatest work is
> a model of them, hung in the sky.

The Cartographers' great **orrery**: a giant spoked wheel of **Orrery Bronze** hanging high in the
sky, its axle crowned with a gilded sun.

- **Materials:** Orrery Bronze (tarnished violet with age), Astral Glass and Leviathan Bone,
  overgrown with Astral Vines, and very quiet.
- **Getting there:** there's no stair or ladder. You need an elytra, pearls or a Gravity
  Inversion "lift" from the island below.
- **Rooms**, round a ring gallery:
  - **The roost:** where dormant Void Wanderers rest; the easiest place to be charted for Chart
    Fragments.
  - **The garden:** overgrown Lumineshroom and Comet Cress beds.
  - **The keepers' quarters:** bunks and a chest.
  - **The Orrery Heart**, at the hub: cold and still. **Pour in 3 Bottled Flares** (from the Corona)
    and it blazes: the rings shudder, the lamps kindle, and the **Bronze Shutters** of the vault slide
    open for 5 minutes. Orreries aren't in the Corona, so the Corona's risk pays off elsewhere: you
    fill the bottles there, then carry them to an Orrery.
- **Vault loot:** Starforge Templates, Temporal Crystals, Charged Void Crystal and, rarely, a Rift Pearl.
- **Orrery Sentinel (mini-boss):** it guards the vault and wakes with the Heart. It's a large, slow
  clockwork construct built like a Wanderer.
  - Its attack is a beam of focused starlight with a long wind-up, so reading the beam is the fight.
  - It always drops a **Starforge Template** and a **Rift Pearl** (§9.3).
- *Not built:* the rotating "artificial gravity" floor (it fought the camera, and it was the
  station idea, not the orrery's).

## 11. Progression

1. **Kill the dragon** (unchanged), then head out through the End Gateway.
2. **The first ~1,000 blocks:** vanilla End Cities, an elytra, and shulker boxes.
3. **Nebula Fields:** your first new biome. Pollinators, glowing flora and low gravity.
   Build an outpost here.
4. **Comet Wake:** mine Meteoric Iron, chase live comets, and raid craters for Temporal
   Crystals.
5. **Dark Matter Belt:** collect Void Crystal, meet the Umbral Stalker, fight Black Hole
   Guardians, and let Void Wanderers chart you for Chart Fragments.
6. **Craft a Portal Lens and build the Astral Forge.**
7. **Shattered Observatory:** solve Constellation Mazes for Starforge Templates, and wake
   your first Wormhole Maw.
8. **The Corona:** once you have Flare Ward potions or Sunshades, raid the Corona for
   Solarite, Charged Void Crystal and Flare Bottles, and loot Solar Array Ruins.
9. **Orreries:** bring Bottled Flares, rekindle an Orrery Heart, and fight the Sentinel
   for the vault.
10. **Long tail:** link every Maw, complete the Drift Whale chain, visit every astral biome,
    and experiment with Starforge builds.

**Advancements** (built in Phase 9). Not a tab of their own: like every Mojang update, they join the
existing tab, as a branch of the **End** tab growing from *Remote Getaway*. Most use vanilla triggers.

- *Astral Horizons*: set foot in an astral biome. *Chart the Stars* (challenge): visit all six.
- *Falling Upward*: pick up Stardust. *Catch a Falling Star*: catch a Stardust Mote from a comet.
- *Iron From the Sky*: smelt Meteoric Iron. *Forged in Starlight*: forge a Starforge upgrade.
  *Starforged From Head to Toe* (goal): wear four Starforged pieces. *Not Today, Void* (hidden
  challenge): be saved by Void Recall.
- *Hold Still*: be charted by a Void Wanderer. *Connect the Dots*: open a Constellation Maze's dais
  chest. *Say Ahh* (goal): wake a Wormhole Maw. *Something Looks Back* (hidden): spot the Watcher.
- *Weather the Storm*: be in the Corona when a flare dies down. *Harvest Moon*: shear a Skimmer.
  *Bottled Sunlight*: fill a Flare Bottle. *Wind It Up* (goal): rekindle an Orrery Heart.
  *Clockwork Undone* (challenge): defeat an Orrery Sentinel.
- *Event Horizon* (goal): defeat a Black Hole Guardian.
- *Something Big Out There*, *Whale Song*, *Tagalong* (challenge): the Drift Whale.

Every recipe also unlocks in the recipe book when you pick up one of its ingredients, as vanilla's do.

---

## 12. Balance and anti-farming rules

- **Starforging is an upgrade, but one per piece.** Its power is capped by slot count,
  and every upgrade targets a different stat, so no single "correct" build exists.
- **The rarest rewards are one-time or limited:**
  - Temporal Crystals are loot only;
  - Singularity Shards need a manual timing kill;
  - comets have limited hits.
- **Chart Fragments** come from a non-violent interaction that's limited to once per
  Wanderer per player.
- **Risk is opt-in.** Flares and Flare Burn exist only in the Corona. The rest of the End,
  vanilla or new, never burns you.
- **The Corona pays for its risk.** Solarite, Charged Void Crystal and Flare Bottles come
  only from there. Surface veins are richer than deep veins, so taking the risk is
  rewarded.
- **Flares can't be triggered on demand.** Charging takes planning, not a farm.
- **Low gravity, gravity inversion, Stellar Flares and live comets** can each be turned off with a
  data pack, by emptying a biome tag (`#astralend:low_gravity`, `#astralend:gravity_inversion`,
  `#astralend:stellar_flares`, `#astralend:live_comets`), the way vanilla switches things by tag.
  Comet landfall respects `mobGriefing`.
- **Black Hole Guardians never delete items** and never break blocks.
- **Maws cost ender pearls per jump,** so they don't replace elytra travel entirely.

---

## 13. Performance budget

| System | Approach |
|---|---|
| Sky (stars, auroras, background comets, silhouettes, flare light) | Drawn in the End sky renderer; no entities, no world data |
| Stellar Flare | One server-wide timer. The burn check runs once per second, only for players inside the Corona: "is there sky above?" |
| Live comet | One entity per player area, with a small model and particles. Block changes happen only on landfall (<40 blocks). |
| Lumineshroom "breathing" | The pulse is an animated texture only; the light level never changes. |
| Biome gravity | A player-only check about once per second that adds or removes an attribute modifier |
| Gravity inversion | Small zones generated with the world; entities check only when inside a chunk that has one |
| Constellation star-bridges | Temporary blocks placed and cleared by the maze, only while a player is looking through the telescope |
| Drift Whale | One per player area. It doesn't collide with blocks, uses a simple model, and despawns aggressively. |
| Black Hole Guardian pull | Checks nearby entities every 5 ticks, within a radius of 6 blocks |
| Singularity upgrade | Tool drops are moved when the block breaks (no scanning). Projectile lensing is checked only when a projectile is about to hit you. |

---

## 14. How to build it (implementation roadmap)

Each phase is playable on its own. Later phases build on earlier ones.

| Phase | Content | Main technical work |
|---|---|---|
| **1. Blocks and flora** ✅ | Umbral Stone set, Stardust (falls up), Astral Glass, Leviathan Bone, Nebula Moss, Lumineshroom, Astral Vine, Starbloom, Comet Cress, Void Crystal and Meteoric Iron. **Done.** Orrery Bronze (tarnishing) moves to Phase 6 with the Orreries it comes from. | Block and item registration, a block that rises instead of falls, a hanging crop, and generated JSON with an automatic checker |
| **2. First biome: Nebula Fields** ✅ | The biome, its terrain features and low gravity | The End's biome list is **hard-coded in vanilla**. Adding biomes needs a custom `BiomeSource` registered in Java, then a data-pack override of `dimension/the_end.json` to use it. Biomes and features themselves are JSON. |
| **3. Remaining biomes** ✅ | Aurora Shallows, Comet Wake, Dark Matter Belt, Shattered Observatory, the Corona, inversion pockets, 42 new blocks and 2 new items. **Done.** | Biome-shaped terrain as JSON density functions, four Java features, a block entity for the pockets |
| **3.5. Biome worlds** ✅ | Territories thousands of blocks across on big biome-shaped islands, dense ground cover, giant flora (Lumineshrooms, Nullwood trees, petrified stalks), ribcages, craters, ice spires, the great ring, Lumine and Nullwood wood sets | Landmass density functions, eight Java features, in-game screenshots in CI |
| **4. Mobs** ✅ | Pollinator, Void Wanderer, Black Hole Guardian, Umbral Stalker, Corona Skimmer, Cinder Mite, Drift Whale and the Watcher, their drops, spawn eggs and advancements. **Done.** | Entity classes and AI goals; models and textures generated together from one description (tools/mobs/make_mobs.py); biome spawn lists plus a hand-placed spawner for the three "one at a time" mobs |
| **5. Sky and Stellar Flares** ✅ | Moving stars, auroras, silhouettes, comets, the Remnant, Corona flare phases, Flare Burn, solar wind, flare charging, Sunshade, Flare Ward, Collector Pedestal, Flare Bottles. **Done.** | 26.3 has no `DimensionSpecialEffects` hook any more: the End's `SkyRenderer` is swapped for a subclass that draws the vanilla sky, then the mod's. A saved server-side flare timer (`SavedData`), synced to clients with one small packet. Brewing is data (26.3 brewing recipes). |
| **6. Structures** ✅ | Comet Crater, Observatory Ruins, Wanderer Relay, Whalefall, Solar Array Ruins, Wormhole Maw, Constellation Maze, Orrery + Sentinel, Orrery Bronze, Portal Lens, Temporal Crystal, Data Core. **Done.** Simplified for now: stations have normal gravity (no rotating floor), and Starforge Templates and Echo Logs (vault loot in the plan) wait for Phases 8 and 9. | Structures are built block by block in Python (`tools/structures/`) and written as structure templates (26.3 format: palette entries are `{id, properties}`), each placed by a jigsaw wrapped in a small `astralend:island` structure type that checks the ground first, so nothing generates over empty void. Maw travel, maze bridges, station power and the Sentinel are Java. The survey finds, generates and tests every structure. |
| **7. Live comets** ✅ | The comet entity, tail riding and Stardust Motes, chunks knocked off the head, landfall craters, `/astralend comet`. **Done.** | One server-driven entity with a mob-style model (not block displays: one entity, one model, cheap to sync) flying a quadratic arc; it isn't saved. A level-tick listener sends comets past players. The crater placer changes at most 40 blocks and checks `mobGriefing` and nearby built blocks. The survey sends one, hits it and lands one. |
| **8. Starforging** ✅ | Astral Forge, Starforge Templates and Alloy, the six upgrades, Rift Pearls, upgrade accents. **Done.** | An `ItemCombinerMenu` and screen (like the smithing table's, with five inputs); a data component for the upgrade; vanilla attribute modifiers for gravity, step height and max absorption; a temporary rift-tile block; one global loot modifier for Smelting and Pull; Forge events for hits, projectiles, fire, effects and the void. Accents are drawn over the vanilla item models (`assets/minecraft/items`) and, on worn armour, by a player render layer. |
| **9. Polish** ✅ | The audit (below), the Orrery revamp, the full Orrery Bronze family, Nebula Moss Carpet, Glowing Stew, Star Compass, the advancement tree, recipe unlocks, data-pack switches. **Done.** | Renames across code, data, structures and textures; a copper-style aging/waxing system shared by four block classes; vanilla advancement triggers; generated recipe-unlock advancements; biome-tag switches |
| **10. The Mojang pass** | Bring every feature in line with what a real Mojang End update would ship (§14.1): defang the hazards, consolidate the materials, replace Starforging with one smithing upgrade, cut what doesn't fit, rename the science words, and add what's missing: ties to the vanilla End, music and ambience, and a reason to come back. **Next.** | World upgrade path for renamed and removed blocks and items (data fixers or remapping), structure templates rebuilt, loot and recipes rebalanced, a sound and music pipeline, changes to vanilla End content through data packs and events |
| **11. New additions** | The new content a real End update would add, from the two concepts (§14.2): the vanilla End's barrens and highlands reworked into the **Ashen Barrens** and the **Chorus Thicket**; two new biomes, the **Glimmering Crags** and the **Obsidian Wastes**; the **Drifter**, the **Weaver**, the **Keeper** and **Chorus Moths**; **Nightstone** and **Chorus Wood**; **Resonance Crystal** and **Rift Shards**; the **Resonant Spires** and their **Resonance Vault**; **Fossilized End Ships**; **Voidgel**, **End Chimes**, the **Resonance Anchor**; a music disc and soundscapes. **After Phase 10.** | Biome overrides for vanilla End biomes, a rideable flying mob with fishing-rod pull, a temporary light-bridge block, sound-muffling blocks that also stop vibrations, a vault keyed to a mob drop, new structure templates, composed music |

**Phases 1-9 are done. Phase 10 is next, then Phase 11.**

### 14.1 Phase 10: the Mojang pass

Phase 9 asked whether each feature was polished. Phase 10 asks a harder question: **would Mojang
ship it at all, in this form, in a real End update?** The pattern Mojang follows: fix the End's real
problems (an empty outer End after the dragon and the elytra, slow travel), build on what the End
already has, keep the feature list small and sharp, use fantasy words rather than science words, and
never ship unfair item loss, punishing weather or power creep.

**The core to keep and build around:** the Nebula Fields, the Aurora Shallows and the new sky; the
Drift Whale, Pollinator, Corona Skimmer and Void Wanderer; Stardust; the Observatory vaults, the
Whalefall, Star Charts and the Star Compass; and a gateway-based travel network.

#### Rework: good ideas, wrong execution

| Feature | Problem | Phase 10 version |
|---|---|---|
| **Stellar Flares** | The sky burning you is weather that hurts players. Mojang has never shipped that (lightning aside). | Keep the gold sky, the blooming Flareblooms, the solar wind's elytra boost and the Skimmer flocks; **remove Flare Burn**. A flare becomes a spectacle and a harvest window, not a hazard. Sunshades, Flare Ward and the burn-related loot and advancements go or change with it. |
| **Live comets** | Landfall that blasts craters into islands is terrain change Mojang wouldn't risk near builds. | **Shooting stars across the sky**, with Comet Cores found only in naturally generated craters. Keep riding the tail and knocking chunks off the head; **remove landfall**. |
| **Umbral Stalker** | Invisible, plus a pounce that knocks you toward the void: the items are lost for good. An unfair death. | Keep the stealth and light-reveals-it (good, Warden-like counterplay); **remove the void knockback**. |
| **Abandoned Orrery + Orrery Bronze** | A second tarnishing metal copies copper's whole gimmick. | Build the Orrery from **copper or purpur variants**, or give the bronze a mechanic of its own instead of tarnish. The Sentinel stays as its mini-boss guardian. |
| **Gravity Inversion pockets / low gravity** | Physics zones are fun but an edge-case nightmare (redstone, mob AI). | Low gravity is plausible now that gravity is an attribute: **keep it to one biome**. **Cut the inversion pockets** (and the Gravity Anomaly). |
| **Constellation Maze** | Puzzle structures are rare in vanilla. | Shrink it to a **smaller set piece**, not a headline structure. |
| **Materials** | Void Crystal, Meteoric Iron, Solarite, Temporal Crystal, Singularity Shard, Stardust, Umbral Membrane, Skimmer Film, Chart Fragment... far too many. A real update adds one or two. | **Consolidate to about three: one End metal, one gem, one mob drop.** Every recipe, loot table and structure that used the others is reworked to match. |

#### Cut or fundamentally rethink

- **Starforging** (six stacking upgrade sets and a new forge screen). A parallel perk system with set
  bonuses is the power creep Mojang avoids; enchantments already fill that role.
  - The salvageable piece is a **single End-themed upgrade through the smithing table** (the
    netherite precedent). The Astral Forge, the templates, the alloy and the six upgrades go.
  - **Void Recall** (a Rift Pearl that saves you from the void) can stand alone, the way the totem
    does, because void safety is a real End need.
- **Black Hole Guardian.** The name is pure sci-fi, and a hostile that swallows your items is the kind
  of mechanic players hate, even though the items come back. Cut it or rethink it from scratch.
- **The Watcher.** A creepy presence that vanishes when you approach leans into creepypasta territory
  that Mojang steers away from. **Move the spyglass interaction to a friendly mob** and cut the Watcher.
- **Science names throughout.** Dark Matter Belt becomes something like **Hollow Isles**; Black Hole
  Guardian, Singularity and Temporal get mystical equivalents; "Corona" and "Nebula" are borderline and
  get a second look.
- **Comet Ice and the Cinder Mite.** Comet Ice is blue ice again; the Mite is an endermite or silverfish
  variant. Both are filler: cut.

#### Fill the gaps a real End update wouldn't have

1. **Touch the vanilla End.** Endermen, shulkers, chorus, End Cities, gateways and the dragon fight
   are all untouched so far. Rework the outer islands' relationship with **End Cities and chorus**, and
   build the travel network on **End gateways**; look at the post-dragon loop too.
2. **Music and soundscapes.** Cut in Phase 9 for lack of real composed music, but every major Mojang
   update ships new music and ambience. Phase 10 needs them: biome ambience, mob sounds where they're
   missing, and new tracks.
3. **A "reason to return" loop.** The pieces exist (the Drift Whale, the travel network, flight boosts)
   but don't yet add up to one clear answer to *why go back after the elytra?* Tie them together; the
   **Drift Whale gets a real purpose** (something to do with it, like riding, leading or being guided
   by it).

### 14.2 Phase 11: new additions

Phase 10 cuts and reworks. Phase 11 adds the content a real End update would add, held to the same
rules: fix the End's real problems (*why come back?* and *how do I get around?*), build on what the End
already has, keep the list small and sharp, use fantasy words, and no unfair item loss, punishing
weather or power creep. **The mood stays alien and stagnant, not lush.**

It takes the best of two concepts: the backbone of the second (biomes, Drifter, Weaver, the vault)
with the strongest pieces of the first (Obsidian Wastes, Voidgel, End Chimes, fossil ships, an End City
guardian), and fixes the problems in both.

#### Biomes: four zones with a strong colour each

Two of them **rework the vanilla End** rather than adding to it, the way the Nether Update reworked the
Nether's biomes. Phase 11 then settles the outer End at the two reworked vanilla biomes, Phase 10's
core (the Nebula Fields and the Aurora Shallows) and the two new biomes below. The four remaining
Phase 3 biomes (Comet Wake, the Dark Matter Belt, the Corona and the Shattered Observatory) are folded
into these or retired, so the outer End doesn't sprawl.

| Biome | Colour | What it is |
|---|---|---|
| **Ashen Barrens** (reworks the End Barrens and small islands) | Pale grey | The classic empty End, made emptier. The ground is dotted with **Ashdust**, a snow-like layer that **muffles footsteps and block-breaking**. Like wool, it also **stops vibrations**, so sculk sensors can't hear you on it. Dead-silent and eerie. |
| **Chorus Thicket** (reworks the End Highlands) | Bleached magenta | Huge **Chorus Stems** grow as trees, **brittle and bleached** rather than lush, and drop the **Chorus Wood** family (a pale, slightly translucent magenta wood set). The ground is **Chorus Moss**, which **cushions falls**. |
| **Glimmering Crags** (new) | Dark, desaturated teal | Jagged crystalline peaks rising out of the void, built from **Nightstone**: a dark, teal-tinted block family (stairs, slabs, polished, bricks), the End's deepslate. **Resonance Crystals** are embedded in the rock. The teal is kept dark and cold so it sits beside the End's purples, as warped did in the Nether. |
| **Obsidian Wastes** (new) | Black | Dark, jagged islands with sharp obsidian spikes that echo the main island's pillars, and scattered ruins. **The Endermen's home**, and where **Fossilized End Ships** lie. |

#### Mobs

| Mob | Role | What it does |
|---|---|---|
| **The Drifter** | Signature friendly mob | A massive, passive, jellyfish-like beast that floats lazily between islands. **Lure it with chorus fruit.** **Hook it with a fishing rod** to be pulled across the void, or **saddle it and steer it with a Chorus Flower on a Stick** (like a strider's Warped Fungus on a Stick). It glides between landmasses rather than flying freely. It solves crossing the void **before you have an elytra**. It absorbs the Void Ray idea, and is where the Drift Whale's role goes: one signature mount, not two. |
| **The Weaver** | Memorable threat | A spindly, pale creature found only in the Resonant Spires. Instead of heavy melee it shoots **Levitation Webs**: get hit and you levitate for 3 seconds, disoriented. You can fight back, and **Silk Touch harvests its webs** to use as vertical jump pads. It drops the **Resonant Key** that opens the vault. |
| **The Keeper** | Guardian of End Cities and the Obsidian Wastes ruins | A hulking, golem-like guardian that **can't be knocked back**. Its shockwave **grounds a gliding player only when there's land beneath them** (they drift down safely, as with Slow Falling). **Over the void it never touches your flight**, so it can never cause a fall into the void. It forces a ground fight, not an unfair death. *(Not "Sentinel": the Orrery already has one.)* |
| **Chorus Moths** | Ambient | Small glowing purple moths around the Chorus Thicket. **Sparse**, a flicker of life in a dead place, not a swarm. |

#### Materials: two, to keep the list small

- **Resonance Crystal.** Mined in the Glimmering Crags; it hums when held. It makes the Resonance
  Anchor.
- **Rift Shards** (the rework of "Aethel Ore", renamed so it doesn't read as a nod to the *Aether* mod).
  An exceptionally rare ore found **only on the undersides of End islands**: you risk the void to mine
  it, as you risk lava for ancient debris. Two uses:
  - **Rift Shards and Phantom Membranes make Rift Thread.** At a smithing table it **reinforces an
    elytra so it wears 50% slower**, and that's all: no wearing a chestplate with it (that's the power
    creep Mojang has refused for years).
  - **Rift Shards make the Rift Pearl**, Phase 10's standalone Void Recall. Void safety comes from
    braving the void.

These are the "one gem, one End metal" that Phase 10's material consolidation lands on. Weaver silk
and Chorus Carapace-style extras aren't added.

#### Blocks and mechanics

- **Nightstone** block family (stairs, slabs, polished, bricks) and the **Chorus Wood** family.
- **Chorus Moss** (cushions falls) and **Ashdust** (muffles sound, blocks vibrations).
- **Voidgel.** A gelatinous block found deep in the Glimmering Crags. Bouncing on it gives **5 seconds
  of Levitation** instead of upward momentum: vertical launch pads for builders, like slime and honey.
- **End Chimes** (End Rods and Resonance Crystal). Hung up, they hum low. **If an Enderman teleports
  within 16 blocks, the chime rings sharply and no Enderman can teleport in that radius for 10
  seconds.**
- **Levitation Web.** A Weaver web taken with Silk Touch: a jump pad.
- **Resonance Anchor** (*renamed from "Echo Anchor": "echo" is taken by echo shards and the recovery
  compass*). Crafted from Resonance Crystals and Purpur. Place it on one island, then throw an Ender
  Pearl: instead of teleporting you, it lays a **shimmering bridge of light** between you and the Anchor
  **for 30 seconds**. You can walk across the gap, and so can your friends.

#### The major structure

- **Resonant Spires** (*renamed from "Echo Spires"*). Tall, hollow Nightstone towers, rare in the
  Glimmering Crags. A vertical dungeon that **mixes climbing and combat**, as trial chambers mix rooms
  and fights. You climb by bouncing off Weaver webs and Voidgel and by riding **shulker bullets'
  Levitation** (shulkers guard the shafts, a vanilla tie). You fight Weavers on the way up.
- **The Resonance Vault**, at the top. It opens with a **Resonant Key** dropped by Weavers (the trial
  vault pattern), and its rewards are purely lateral, as in Trails & Tales and Tricky Trials:
  - **two banner patterns**;
  - the **Eclipse armour trim** (cosmetic only; it glows);
  - **a new music disc** capturing the End's isolation.

  It answers *why come back?* without power creep.

#### Lore, told by the world

- **Fossilized End Ships**: rare ancient End Ships embedded in the Obsidian Wastes, built from a
  **different wood than Purpur**. The End was once sailed by builders, before something broke it.
  No text, just the wrecks.

#### Ties to the vanilla End

Phase 11 is where the mod finally builds on the End it starts from:

| Vanilla piece | What Phase 11 does with it |
|---|---|
| **Chorus** | The Chorus Thicket, Chorus Wood, Chorus Moss; it lures the Drifter, and the Chorus Flower on a Stick steers it. |
| **Endermen** | The Obsidian Wastes are their home, and End Chimes stop their teleports. |
| **End Cities** | Guarded by the Keeper; the Fossilized End Ships are the ancient ships' ancestors. |
| **Shulkers** | Their bullets' Levitation is how you climb the Resonant Spires. |
| **Elytra** | Rift Thread makes it last; the Drifter and the Resonance Anchor carry you before you have one. |
| **The barrens and highlands** | Reworked into the Ashen Barrens and the Chorus Thicket. |

#### Music and soundscapes

- A **new music disc** (the vault reward).
- **Biome ambience**, with the Ashen Barrens near-silent, as Ashdust means.
- **Sounds for every new mob.**

This needs real composed music, as Phase 9 noted.

---

## 15. Decisions made

| Question | Decision |
|---|---|
| Telescope | Vanilla's **spyglass** does the job: in a Constellation Maze, raise one and hold a constellation in view and its bridges light. (There was a Maze Telescope block; it was cut in Phase 11 - it did what the spyglass already does.) |
| Mod name and ID | **The Astral End**, mod ID `astralend`. The former `rubymod` project was renamed; its Ruby example content stays until Phase 1 replaces it. |
| Flares | **Only in the Corona biome.** Unprotected players there get Flare Burn; cover, Sunshades, Flare Ward potions and the Radiant upgrade protect them. The vanilla End and all other biomes never burn. The Corona makes up for the risk with exclusive materials. |
| Another hostile mob | **Yes: Cinder Mites**, Corona only. |
| Where new biomes start | **Beyond 1,000 blocks**, the same distance where vanilla's outer islands begin. |
| Turning features off | Low gravity and gravity inversion are each controlled by a biome tag (`#astralend:low_gravity`, `#astralend:gravity_inversion`); an empty tag in a datapack turns them off. A config file can come in Phase 9. |
| Existing worlds | Nebula Fields kept exactly the same placement when Phase 3 added the other biomes, so End areas generated before Phase 3 still line up with new chunks. |

## 16. Open questions

1. **Art:** should textures be drawn first (in Blockbench/Piskel), or should we start with
   placeholder textures and replace them later?
