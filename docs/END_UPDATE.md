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
- **Echo Logs**: short, vague text read by holding a Data Shard in a Portal Lens (§8.4).
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
- Beyond that, the outer End becomes a **patchwork of vanilla biomes and six new astral
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
| **The Corona** | Rare | Scorched, blinding, rich | The only place with Stellar Flares. High-risk, high-value: Solarite, Charged Void Crystal, Flare Cells |

---

## 4. Biomes

### 4.1 Nebula Fields
Soft, low islands wrapped in coloured fog (violet into teal), with faint drifting motes
in the air.

- **Built (Phase 2):** regions several hundred blocks across covering about a fifth of the
  outer End, placed by their own noise on top of vanilla's End biomes.
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

### 4.6 The Corona
The islands closest to **the Remnant**, the dying ember of the collapsed star. It hangs
low in the sky over this region as a huge, dim, slowly pulsing disc. You can see it from
far away, so players always know where the Corona is (and can choose to avoid it).

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
  - **Flare Cells:** the batteries that power Orbital Stations, charged only here.
  - **Solar Array Ruins** (§10): Cartographer mirror-farms that once harvested the
    flares, with some of the best loot in the End.
- **Feel:** the End's Nether. It's beautiful from a distance and dangerous up close.
  You come prepared, work fast between flares, and leave rich.

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
  - Wanderers avoid the Corona. The few that stray in during a flare malfunction,
    drifting and sparking, unable to scan.
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
- **Drops:** **Umbral Membrane**, used for the Stalker Cloak and the Voidwalker upgrade.
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

### 7.1 Stellar Flares (Corona only)
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
- **Flare Cells:** empty cells placed on a Collector Pedestal fill up. They power
  Orbital Stations anywhere in the End (§10.3).
- **Flareblooms open**, giving Flare Pollen.
- **Skimmers become shearable** in the afterglow.
- **Solar wind:** inside the Corona, elytra flight gets a strong push in the flare's
  direction.
- **Radiant armour** charges 5× faster.

**Config:** flare frequency, Flare Burn (on/off) and flare sky effects can each be
adjusted.

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
| **Charged Void Crystal** | Void Crystal charged during a flare, only in the Corona | Flare Charging (§7.1) | Wormhole Maws, Radiant upgrade |
| **Solarite** | Solarite Ore, only in the Corona. Rich exposed surface veins, sparse deep veins. | Mine with a diamond pickaxe. Smelt the raw chunks. | Radiant upgrade, Flare Cells, Solar Lanterns, Sunshade frames |
| **Flare Cell** | Crafted from Solarite and Astral Glass, then charged during a Corona flare | Collector Pedestal in the Corona | Powers Orbital Stations |
| **Meteoric Iron** | Meteoric Iron Ore in Comet Cores (Comet Wake), live comets | Mine with a diamond pickaxe. Smelt the raw chunks. | Starforge Alloy, Gravity Wells, the Astral Forge |
| **Temporal Crystal** | Loot only: craters, station vaults, maze rewards | Can't be mined or farmed | Portal Lens, Temporal upgrade, Maw repair |
| **Stardust** | Stardust Blocks (Comet Wake), comet tails | Shovel, or fly through a comet tail | Building, Astral Glass, Comet Cress farms |
| **Orrery Bronze** | Salvaged from Orbital Stations | Mine station hull blocks | Station-style building, lamps and grates |

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
- **Stardust Block**: falls up, drifting one block at a time until it hits something.
- **Astral Glass**: smelted from Stardust. A dark glass with a faint star speckle.
  - A rare **Starfield Glass** variant shows the End-portal starfield effect.
  - Starfield Glass is expensive and limited to a handful per build, for performance.
- **Comet Ice**
- **Nebula Moss** (a ground cover that spreads like moss) and **Moss Carpet**
- **Spore Lantern** (light level 13) and **Whalelight Scale Block** (light level 10)
- **Gravity Well**, **Void Trap** (Singularity Shard utility blocks)
- **Collector Pedestal**: charges Void Crystal and Flare Cells during Corona flares.
- **Sunshade**: a canopy block (Skimmer Film + Solarite frame) that blocks Flare Burn.
  Quick to place and break, for shelter while mining.
- **Solar Lantern** (light level 15): Solarite + Cinder.
- **Sunscorched Stone** (+ Polished, Bricks), **Sunscale Lichen**, **Pollinator Husk**
  (decoration).

### 8.3 Cosmic plants

| Plant | Behaviour | Uses |
|---|---|---|
| **Lumineshroom** | Grows on Nebula Moss, End Stone or Umbral Stone. Its cap pulses in a slow "breath" cycle (an animated texture). Its light level stays at 10, so it costs nothing. | Spore Lanterns, glowing stew, dye |
| **Astral Vine** | Grows **upward** from the undersides of blocks. Climbable. Shears stop growth. | Decoration, climbing up island edges |
| **Starbloom** | A flower that rotates to face the End's centre. Vanilla compasses and clocks spin uselessly in the End, so this is the End's only natural way to find your bearings. | Pollinator breeding, dye, **Star Compass** (points to the central island, works in the End) |
| **Shimmer Grass** | A short, swaying grass in the Aurora Shallows | Decoration, Pollinator food |
| **Comet Cress** | Hangs from the underside of Stardust and grows downward, in any light. (Stardust rises until it hits a ceiling, so its underside is the only free face.) | Food (2 hunger), farming |
| **Gravlily** | A rare flower from Dark Matter Belt inversion pockets. It grows upside down. | Brewing: **Potion of Buoyancy** (slow fall + a small jump boost) |

### 8.4 Tools and utility items
- **Portal Lens** (Data Shard ×3 + Void Crystal + Temporal Crystal + Spyglass). A
  spyglass upgrade that:
  - shows distortion outlines (Umbral Stalkers, hidden ore glints);
  - points toward the nearest known Wormhole Maw;
  - reads Echo Logs from Data Shards;
  - is also the key used to wake and link Maws.
- **Telescope use:**
  - Constellation Mazes are built around **vanilla 26.3's telescope**: star-bridges
    appear only while you look through it (§10).
  - The Portal Lens also works, at shorter range.
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
  + Void Crystal), using a **Starforge Template** (from station vaults and maze rewards,
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
    Guardian, Station Sentinel), and they're rare vault loot. That keeps Void Recall from
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
| **Abandoned Orbital Station** | High sky (Y 180+), mostly above Observatories | See §10.3 |
| **Observatory Ruins** | Shattered Observatory, rare in Nebula Fields | A domed ruin with star mosaics and a telescope made of blocks. Vaults (opened with Data Shards, like trial keys) hold Starforge Templates, Temporal Crystals and Echo Logs. It has the only guaranteed Astral Forge recipe book. |
| **Solar Array Ruins** | The Corona | Fields of broken Cartographer mirrors that once gathered flare light, around a sunken control room. The best loot in the End: Starforge Templates, Solarite blocks, Temporal Crystals, a guaranteed charged Flare Cell. The mirrors focus flares, so Flare Burn is doubled on the open field. |
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
  - **Solar core:** sealed and dead. **Insert 3 charged Flare Cells** (from the Corona)
    to power it. Lights come on, doors unlock, and the central **vault** opens for 5
    minutes.
    - Stations aren't in the Corona, so the Corona's risk pays off elsewhere: you
      charge the cells there, then carry them to a station.
- **Vault loot:** Starforge Templates, Temporal Crystals, Charged Void Crystal and the
  Starfield Glass recipe.
- **Station Sentinel (mini-boss):** it guards the vault and wakes with the power. It's a
  large, slow Wanderer-style automaton.
  - It uses a Wanderer's scanning beam as its attack wind-up, so reading the beam is the
    fight.
  - It drops a guaranteed Data Core, used to craft a **Wanderer Beacon** that calls a
    friendly Wanderer, plus a **Rift Pearl** (§9.3).

---

## 11. Progression

1. **Kill the dragon** (unchanged), then head out through the End Gateway.
2. **The first ~1,000 blocks:** vanilla End Cities, an elytra, and shulker boxes.
3. **Nebula Fields:** your first new biome. Pollinators, glowing flora and low gravity.
   Build an outpost here.
4. **Comet Wake:** mine Meteoric Iron, chase live comets, and raid craters for Temporal
   Crystals.
5. **Dark Matter Belt:** collect Void Crystal, meet the Umbral Stalker, fight Black Hole
   Guardians, and let Void Wanderers scan you for Data Shards.
6. **Craft a Portal Lens and build the Astral Forge.**
7. **Shattered Observatory:** solve Constellation Mazes for Starforge Templates, and wake
   your first Wormhole Maw.
8. **The Corona:** once you have Flare Ward potions or Sunshades, raid the Corona for
   Solarite, Charged Void Crystal and Flare Cells, and loot Solar Array Ruins.
9. **Orbital Stations:** bring charged Flare Cells, power one up, and fight the Sentinel
   for the vault.
10. **Long tail:** link every Maw, complete the Drift Whale chain, collect every Echo Log
    and constellation, and experiment with Starforge builds.

**Advancement tab: "The Astral End".** About 25 advancements, including:
- *Falling Upward*: use a Stardust block.
- *Hold Still*: get scanned by a Void Wanderer.
- *Weather the Storm*: survive a Stellar Flare in the Corona without cover.
- *Harvest Moon*: shear a Corona Skimmer.
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
- **Risk is opt-in.** Flares and Flare Burn exist only in the Corona. The rest of the End,
  vanilla or new, never burns you.
- **The Corona pays for its risk.** Solarite, Charged Void Crystal and Flare Cells come
  only from there. Surface veins are richer than deep veins, so taking the risk is
  rewarded.
- **Flares can't be triggered on demand.** Charging takes planning, not a farm.
- **Low gravity, gravity inversion, flare damage and live comet landfall** can each be
  turned off in the config.
- **Black Hole Guardians never delete items** and never break blocks.
- **Maws cost ender pearls per jump,** so they don't replace elytra travel entirely.

---

## 13. Performance budget

| System | Approach |
|---|---|
| Sky (stars, auroras, background comets, silhouettes, flare light) | Drawn in the End sky renderer; no entities, no world data |
| Stellar Flare | One server-wide timer. The burn check runs once per second, only for players inside the Corona: "is there sky above?" |
| Live comet | One entity per player area, with a few block-display parts and particles. Block changes happen only on landfall (<40 blocks). |
| Lumineshroom "breathing" | The pulse is an animated texture only; the light level never changes. |
| Biome gravity | A player-only check about once per second that adds or removes an attribute modifier |
| Gravity inversion and station gravity plating | Small zones generated with the world; entities check only when inside a chunk that has one |
| Constellation star-bridges | Temporary blocks placed and cleared by the maze, only while a player is looking through the telescope |
| Drift Whale | One per player area. It doesn't collide with blocks, uses a simple model, and despawns aggressively. |
| Black Hole Guardian pull | Checks nearby entities every 5 ticks, within a radius of 6 blocks |
| Singularity upgrade | Tool drops are moved when the block breaks (no scanning). Projectile lensing is checked only when a projectile is about to hit you. |
| Starfield Glass | Uses the end-portal effect; limited by its crafting cost |

---

## 14. How to build it (implementation roadmap)

Each phase is playable on its own. Later phases build on earlier ones.

| Phase | Content | Main technical work |
|---|---|---|
| **1. Blocks and flora** ✅ | Umbral Stone set, Stardust (falls up), Astral Glass, Leviathan Bone, Nebula Moss, Lumineshroom, Astral Vine, Starbloom, Comet Cress, Void Crystal and Meteoric Iron. **Done.** Orrery Bronze (tarnishing) moves to Phase 6 with the Orbital Stations it comes from. | Block and item registration, a block that rises instead of falls, a hanging crop, and generated JSON with an automatic checker |
| **2. First biome: Nebula Fields** ✅ | The biome, its terrain features and low gravity | The End's biome list is **hard-coded in vanilla**. Adding biomes needs a custom `BiomeSource` registered in Java, then a data-pack override of `dimension/the_end.json` to use it. Biomes and features themselves are JSON. |
| **3. Remaining biomes** | Aurora Shallows, Comet Wake, Dark Matter Belt, Shattered Observatory, the Corona, inversion pockets | More JSON worldgen, plus Java for inversion pockets |
| **4. Mobs** | Pollinator, Void Wanderer, Black Hole Guardian, Umbral Stalker, Corona Skimmer, Cinder Mite, then Drift Whale and the Watcher | Entity classes, AI goals, models (Blockbench → Java model code), spawn rules |
| **5. Sky and Stellar Flares** | Moving stars, auroras, silhouettes, the Remnant, Corona flare phases, Flare Burn, solar wind, flare charging | A custom `DimensionSpecialEffects` for the End (client), a saved server-side flare timer, a network packet to sync flare state |
| **6. Structures** | Comet Crater, Observatory Ruins, Wanderer Relay, Whalefall, Solar Array Ruins, Wormhole Maw, Constellation Maze, Orbital Station + Sentinel | Structure templates built in-game and saved as `.nbt`, plus jigsaw JSON. Maw linking, maze bridges and station power need Java. |
| **7. Live comets** | The comet entity, tail harvesting, landfall craters | An entity with block-display parts, a flight path, and a small crater placer |
| **8. Starforging** | Astral Forge, templates, the six upgrades, Rift Pearls | Data components on items, a menu and screen for the Forge, vanilla attribute modifiers (gravity, step height), a temporary rift-tile block, loot modifiers for smelting and pull, event listeners for hits, projectiles, fire damage and falling into the void |
| **9. Polish** | Advancements, Echo Logs, music, config toggles, balance passes | JSON advancements, a Forge config file, playtesting |

**Next:** Phase 3, the other four biomes (Aurora Shallows, Comet Wake, Dark Matter Belt, Shattered Observatory) and the Corona, plus gravity inversion pockets.

---

## 15. Decisions made

| Question | Decision |
|---|---|
| Telescope | Minecraft 26.3 has one. Constellation Mazes and the Watcher use the vanilla telescope. |
| Mod name and ID | **The Astral End**, mod ID `astralend`. The former `rubymod` project was renamed; its Ruby example content stays until Phase 1 replaces it. |
| Flares | **Only in the Corona biome.** Unprotected players there get Flare Burn; cover, Sunshades, Flare Ward potions and the Radiant upgrade protect them. The vanilla End and all other biomes never burn. The Corona makes up for the risk with exclusive materials. |
| Another hostile mob | **Yes: Cinder Mites**, Corona only. |
| Where new biomes start | **Beyond 1,000 blocks**, the same distance where vanilla's outer islands begin. |

## 16. Open questions

1. **Art:** should textures be drawn first (in Blockbench/Piskel), or should we start with
   placeholder textures and replace them later?
