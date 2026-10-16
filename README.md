# The Astral End

**The End is alive.** A Minecraft mod that adds new biomes, creatures and structures to the outer
End, the islands more than 1,000 blocks from the dragon's island.

[![Fifteen seconds of the trailer, filmed in the game. Click for the full trailer on YouTube.](docs/modrinth/teaser.webp)](https://youtu.be/ZUQVTpnqRTQ)

**[Download on Modrinth](https://modrinth.com/mod/the-astral-end)** ·
**[The field guide](https://ggrantrichards.github.io/the-astral-end/)** (every biome, structure and creature) ·
**[The trailer](https://youtu.be/ZUQVTpnqRTQ)**

## Requirements

| Minecraft | Forge | File |
|---|---|---|
| **26.3** | 66 or newer | `astralend-<version>.jar` |
| **1.21.1** | 52.1 or newer | `astralend-<version>-mc1.21.1.jar` |

Install it on the client, and on the server too for multiplayer. Start a new world, or explore past
1,000 blocks from the centre of an old one's End: the mod changes only the outer End, and only where it
hasn't been generated yet. (Minecraft 1.21.1 asks about "experimental settings" for any mod that adds
world generation. The mod answers it for you.)

## What's in it

![The Leviathan, cut out of the world](docs/images/distilled/diorama_leviathan.png)

- **Six new biomes on the outer islands, and two caves.**
  - *Nebula Fields*: pink moss, glowing gas clouds and low gravity.
  - *Deep Hollows*: a dark forest, with the *Hollow Roots* caves underneath.
  - *Glimmering Crags* and *Shattered Crags*: crystal peaks, with the *Crystal Vaults* caves below.
  - *Ashen Barrens*: ash and old Leviathan bones.
  - *The Gilded Reach*: a rare golden grassland. Its star causes Sunstorms.
- **The Leviathan.** A skeleton about 370 blocks long with End City towers on it. Inside are the
  Curator (a mini-boss), Hollowbacks, a Constellation Maze, and a skull that works as a portal.
- **Ten new mobs.** Drift Whales you can tame and ride, Hushbacks, Hydragrips, the Mirror Wretch, the
  Watcher, the Umbral Stalker, the Shardback, Pollinators, Hollowbacks and the Curator. They react to
  each other.
- **Starforging.** Five upgrades for diamond and netherite gear, made at the Astral Forge.
- **Also:** three smaller structures, Sunstorms, a Lodestar respawn point, new building blocks and a
  wood set, four music tracks and a disc, ambient sounds and advancements.

The mod sticks to the End's colours and doesn't add new armour tiers, friendly villagers or on-screen
text. The design notes are in [docs/DISTILLATION.md](docs/DISTILLATION.md).

## Credits

- **Music** from OpenGameArt.org: "Out There" by yd, "Ambient Relaxing Loop" and "Heavenly Loop" by
  isaiah658, "Observing the Star" by madeso (all CC0), and "Space Music" by HitCtrl (CC-BY 3.0). Full
  list and links in [sounds/credits.txt](src/main/resources/assets/astralend/sounds/credits.txt).
- **Textures:** some block textures are based on vanilla textures. Those belong to Mojang and aren't
  covered by this mod's licence.

## Licence

The mod's code, its own textures and models: MIT (see [LICENSE](LICENSE)).

## Building it

```
python3 tools/generate_resources.py      # the blockstates, models, loot, recipes, worldgen... (src/main/resources)
./gradlew build                          # the jar, in build/libs
```

Every push runs CI (`.github/workflows/build.yml`). It checks the resources, builds, loads the mod in a
server and a real client, surveys a generated End (`dev/WorldgenSurvey.java`), and takes in-game
screenshots and playtests. The **1.21.1** build lives on the `port-1.21` branch: there,
`tools/port/to_1_21.py` converts the generator's output to 1.21.1's formats. Releases go out with the
*Release to Modrinth* workflow (`tools/release/modrinth.py`, with the page in `docs/modrinth/`). The
development history is in [docs/DEVLOG.md](docs/DEVLOG.md).
