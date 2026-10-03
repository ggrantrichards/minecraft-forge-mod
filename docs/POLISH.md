# Polish: how The Astral End is tested, and what's still open

The bar is "could ship in vanilla": nothing breaks, nothing looks unfinished, every place has one
strong idea, and moving through the End feels continuous. This file lists how each of those is
checked, and the open items each check has turned up. BetterEnd (the best-known End mod) is the
reference for what "polished" means for End content; its lessons are in the last section.

Every push to `textures` runs all three automated passes in CI (`.github/workflows/build.yml`):

| Pass | What it is | Where the results go |
|---|---|---|
| **Survey** | A real dedicated server generates a fresh End and checks it by code (`dev/WorldgenSurvey`, `MechanicsSurvey`, `StarforgeSurvey`, `PolishSurvey`). Fails the build on any broken check. | `ci-logs` branch, `survey.txt` |
| **Screenshots** | The real client photographs every biome (ground and aerial), every structure (outside and in), every mob, the sky in each phase. Judged by eye. | `ci-screenshots` branch |
| **Playtests** | The real client *plays* scenes with real key presses in survival and films them (`client/ClientPlaytests`). Judged by eye and by their log lines. | `ci-playtests` branch, contact sheets |

## 1. Automated checks (survey) - pass/fail

World generation
- [x] Every astral biome exists, is big and coherent (territory size, % of the outer End, "coherence").
- [x] Every biome has land, its own surface block, its plants and its ores (block counts per biome).
- [x] The central island and the dragon fight are untouched; the stronghold portal leads to the End.
- [x] Every structure generates and is actually built (not just "started"); its chest has loot.
- [x] Rift Maw: four eye sockets, wakes when lit, its throat becomes a wormhole with an exit to stand on.
- [x] Constellation Maze: four platforms, every constellation's bridge lights, a spyglass in the barrel.
- [x] The Remnant: a heart, a hollow chamber, glowing fissures and a crust, high over the Corona.
- [x] Resonant Spire: the Wyrm is asleep on its nest.

Mechanics
- [x] Every spawn egg, used the way a player uses it, gives its mob, still there 2 s later (the Stalker visible).
- [x] A hand-built End portal lights when its frames face in, and doesn't when they face out (vanilla's rule).
- [x] The Spire Wyrm fight: a minute against a stand-in player - it must wake, circle, wind up and dive,
      lie grounded, wind up and breathe, and never leave its spire (<= 28 blocks) or sink below the platform.
- [x] Lodestar, Drift Whale (health, flees), Umbral Stalker (fears fire), Upwelling (gravity, Slow Falling
      after), End Chimes, Voidgel, Chorus Moss, Starforge upgrades, low gravity, comets.

To add
- [ ] Chorus Thicket: share of the biome under trees (groves, not a carpet) - between 25% and 60%.
- [ ] Nebula Fields: at least one oasis per few chunks, water held (no leaks into caves).
- [ ] Biome borders: along a line across each border, the biome must switch back and forth (interlock)
      over 10-30 blocks, not once.
- [ ] Every block can be mined with the right tool and drops itself (loot table round trip).
- [ ] Every recipe's result is obtainable in survival (no recipe needs a creative-only item).

## 2. Screenshot review - by eye, every run

For each picture: does it read as finished vanilla content at a glance? Look for
- textures that don't match vanilla's palette or detail level, or tile visibly;
- floating or cut-off features (half a tree at a chunk edge, plants on air);
- empty places: a biome with nothing to look at in the middle distance;
- structures that are too small for their role, or empty inside;
- lighting: dark interiors that should be lit, glowing things that don't glow.

## 3. Playtests - by eye and log, every run

| Scene | What a player does | What must hold |
|---|---|---|
| `whale_ride` | mount a Drift Whale, fly forward, turn, climb, third person | seated on its back, clear view, smooth turning, no jitter |
| `wyrm_fight` | step onto a spire's top in survival | it wakes on the nest, fights on and over the platform, can be hit, never vanishes or teleports |
| `corona_drop` | teleport high over the Corona and fall | land on real ground; the Remnant is a place in the sky, not a colour |
| `upwelling` | drink Upwelling under a ledge, walk, turn, jump | flip upside down, stand on the underside, controls follow the screen, drop off cleanly |
| `biome_border` | fly out across a biome's edge | sky and fog shift over 10-15 blocks; ground and plants interlock |
| `maw_hall` | stand in a Rift Maw's hall, look round | huge, lit, things to see; the way in and the eyes reachable |
| `remnant_flight` | fly in toward the Remnant | the sky picture of it hands over to the real structure without a jump |

To add: the Constellation Maze with a spyglass; a Sunstorm caught in the open and under a Sunshade;
the Whalefall village; mining Solarite; brewing Upwelling; a Starforge upgrade used in a fight.

## 4. Open items found so far (Phase 11)

From the user's play and the checks above. Done ones are kept for the record.

- [x] Spyglass instead of a bespoke telescope in the mazes.
- [x] Spire Wyrm fight rebuilt (own voice, starts asleep on its nest, stays at its spire, readable phases).
- [x] Sky/aurora colour eases across borders (blended from the biomes round you, eased over 10-15 blocks).
- [x] Biome borders interlock (vanilla and astral alike).
- [x] Oases in the Nebula Fields; Chorus Thicket in groves.
- [x] Rift Maw: a colossal skull with a hall inside.
- [x] Drift Whale: rider sits on its back; smooth turning.
- [x] Upwelling turns you upside down onto ceilings.
- [x] The Corona: the Remnant is real; spires and fissures on its ground.
- [ ] Spawn eggs: confirm the Void Wanderer and Umbral Stalker in the survey's egg check.
- [ ] Biome ambient sound loops (each biome its own; see section 5).
- [ ] Living water: something glowing in the oases.

## 5. What BetterEnd does that we measure ourselves against

From BetterEnd's wiki (biomes and mobs). Not to copy - to match the level of finish.

1. **One strong idea per biome**, visible from a distance: geysers (Sulphur Springs), tiered lakes
   (Megalake), glowing lantern trees (Lantern Woods). *Ours:* Nebula Fields (giant Lumineshrooms, oases),
   Chorus Thicket (groves), Corona (the Remnant overhead, spires, fissures), Glimmering Crags (crystal
   spikes, the Spire), Comet Wake (ice spires, live comets), Hollow Isles (undersides), Ashen Barrens (auroras).
2. **Living water**: lakes with glowing fish, jellyfish and dragonflies. *Ours:* oases are new; they need life.
3. **Ambient life in the air** in every biome: fireflies, spores, moths. *Ours:* every biome has ambient
   particles; Pollinators and Void Rays fly. Add an ambient sound loop per biome.
4. **Shores and edges** belong to their biome (biome-specific shores). *Ours:* interlocking borders; the
   islands' undersides are biome-specific (Hollow Isles' tendrils, Nebula undersides).
5. **Caves** are biomes of their own. *Ours:* not yet - a candidate for after 1.0.
