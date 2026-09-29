# Ruby Mod: your first Minecraft Forge mod

A complete, minimal Forge mod for **Minecraft Java Edition 26.3** with **Forge 66.0.8**.
It adds a Ruby item, a Block of Ruby, crafting recipes both ways, and a welcome
message when you join a world.

## 1. Install the tools (one time)

| Tool | Why | Get it |
|---|---|---|
| **JDK 25** | Minecraft 26.x runs on Java 25 | [Adoptium Temurin 25](https://adoptium.net/) (Gradle can also auto-download it) |
| **IntelliJ IDEA Community** | Java IDE with the best Gradle support | [jetbrains.com/idea](https://www.jetbrains.com/idea/download/) |
| **Minecraft Java Edition** | To play your mod for real (not needed for `runClient`) | minecraft.net |
| **Git** (optional) | Version control | git-scm.com |

You do **not** install Gradle yourself. The `gradlew` script in this folder downloads the right version.

## 2. Get this project

Copy this `minecraft-forge-mod` folder anywhere on your computer. To start a mod from
scratch instead, download the **MDK** (Mod Development Kit) for your Minecraft version
from <https://files.minecraftforge.net/> and unzip it. This project is that MDK plus the example content.

## 3. Open it in IntelliJ, which also gets you the Minecraft source

1. IntelliJ → **Open** → select this folder (the one with `build.gradle`) → *Trust project*.
2. Wait for the Gradle sync. **The first sync takes 5–15 minutes.** ForgeGradle downloads
   Minecraft, applies Forge's patches, and **decompiles Minecraft into readable Java source**.
3. When it finishes, you have the Minecraft source code:
   - Press **Shift Shift** (Search Everywhere) and type a class name such as `Block`, `Item`,
     `Player`, `Zombie`, or `Blocks`, then open it.
   - Or **Ctrl+click** (Cmd+click on Mac) any Minecraft class in `RubyMod.java`.
   - In the Project panel, look under **External Libraries** for the Minecraft/Forge jar.
   If IntelliJ shows a "decompiled .class file" banner, that's fine; it's still readable Java.
   Since 26.1, Mojang ships Minecraft **unobfuscated**, so you see the real names Mojang's
   developers use.
   Reading vanilla code is the most important modding skill: to make something like a
   vanilla thing, find the vanilla thing and copy its approach.

   > You may read and learn from the decompiled code, but you may not redistribute
   > Minecraft's code. Your mod jar contains only *your* code.

From a terminal instead: `./gradlew build` (Windows: `gradlew.bat build`).

## 4. Run the game with your mod

- In IntelliJ's Gradle panel (elephant icon, right side): open **rubymod → Tasks**, find **runClient**, and double-click it, or
- in a terminal: `./gradlew runClient`

A development copy of Minecraft opens with your mod loaded, and you can use it offline.
Make a Creative world, then:
- look in the **Ingredients** tab for the Ruby and **Building Blocks** for the Block of Ruby, or
- `/give @s rubymod:ruby 64`
- craft 9 rubies into a block and back. Mine the block with an iron pickaxe or better.
- the chat shows "Welcome! Go find some rubies." when you join.

Game files for this dev copy (worlds, logs, options) go in `run/`. Crash logs are in `run/logs/latest.log`.

## 5. How the project is laid out

```
build.gradle                  Minecraft + Forge versions, run configs
settings.gradle               project name
src/main/java/com/yourname/rubymod/
  RubyMod.java                entry point (@Mod), event listeners
  ModBlocks.java              block registrations
  ModItems.java               item registrations
src/main/resources/
  META-INF/mods.toml          mod ID, name, version, dependencies
  assets/rubymod/             CLIENT stuff: what things look like
    textures/item/ruby.png        16x16 PNG
    textures/block/ruby_block.png
    models/item/ruby.json         "flat item using this texture"
    models/block/ruby_block.json  "cube with the same texture on all sides"
    items/*.json                  which model to use for each item
    blockstates/ruby_block.json   which model to use for each block state
    lang/en_us.json               display names
  data/                       SERVER stuff: game rules
    rubymod/recipe/*.json         crafting recipes
    rubymod/loot_table/blocks/    what a block drops
    minecraft/tags/block/...      "mineable with pickaxe", "needs iron tool"
```

Rule of thumb: **Java registers that a thing exists and what it does. JSON says what it
looks like, what it's called, how it's crafted, and what it drops.**
Everything uses IDs like `rubymod:ruby`, where the namespace is your mod ID.

## 6. Adding your own item: checklist

Example: a "Sapphire".
1. `ModItems.java`: copy the `RUBY` line and change `ruby` to `sapphire` in all 3 places.
2. `assets/rubymod/items/sapphire.json` and `models/item/sapphire.json`: copy the ruby ones and rename inside.
3. `assets/rubymod/textures/item/sapphire.png`: a 16×16 PNG (use Aseprite, Piskel, or Paint.NET).
4. `lang/en_us.json`: add `"item.rubymod.sapphire": "Sapphire"`.
5. Optional: add it to a creative tab in `RubyMod.addToCreativeTabs`, and add a recipe.
6. `./gradlew runClient`.

If the item shows as a **purple/black checkerboard**, a model or texture path is wrong.
If it shows as **`item.rubymod.sapphire`**, the lang entry is missing.

## 7. Build a jar and play it in real Minecraft

1. `./gradlew build` → your mod is `build/libs/rubymod-1.0.0.jar`.
2. Install Forge for 26.3 in your normal launcher: download the **Installer** from
   files.minecraftforge.net, run it, and choose *Install client*.
3. Put the jar in your `.minecraft/mods/` folder.
   - Windows: `%APPDATA%\.minecraft\mods`
   - macOS: `~/Library/Application Support/minecraft/mods`
   - Linux: `~/.minecraft/mods`
4. Pick the **forge** profile in the Minecraft Launcher → Play.

## 8. Renaming the mod

The mod ID `rubymod` appears in `RubyMod.MODID`, `mods.toml` (twice, including
`[[dependencies.rubymod]]`), `build.gradle` (the runs), `settings.gradle`, and the
`assets/rubymod` and `data/rubymod` folder names and every `rubymod:` inside the JSON.
Rename the Java package `com.yourname.rubymod` with IntelliJ's Refactor → Rename.

## 9. Where to go next

- Forge docs: <https://docs.minecraftforge.net/>
- Official MDK examples (Mixins, Access Transformers, and more): <https://github.com/MinecraftForge/MDKExamples>
- Forge source: <https://github.com/MinecraftForge/MinecraftForge> (the `26.3` branch)
- Forge Discord and the forums at forums.minecraftforge.net
- Ideas to try next: a custom sword (`SwordItem`/tool materials), a food item
  (`Item.Properties().food(...)`), ore that generates in the world (worldgen JSON),
  a block with a right-click action (override `useWithoutItem` in a `Block` subclass),
  or a custom creative tab (see the MDK's `ExampleMod.java`).

## Updating to a newer Minecraft version

Change `net.minecraftforge:forge:26.3-66.0.8` in `build.gradle` and the version ranges in
`mods.toml`, then re-sync. Minecraft changes its code every version, so expect some fixes.
Forge's changelog and the MDK for the new version show what changed.
