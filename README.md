# RichStuff

**RichStuff** is the processing, storage, and food companion mod for Minecraft 1.21.1 (NeoForge), forming one half of the RichStuff + AllTheOres pair.

- **Mod ID:** `richstuff`
- **Version:** 1.0.1
- **Minecraft:** 1.21.1
- **NeoForge:** 21.1.235
- **Group:** `com.richetoku.richstuff`

It is the processing/storage/food side of a converted KubeJS pack. AllTheOres owns ore blocks and ore world generation; RichStuff owns everything else built on top of those ores.

## What RichStuff adds

RichStuff owns dusts, nuggets, tiny dusts, tiny nuggets, plates, rods, wires, springs, gears, coins, coin stacks, molds, machines, crates, bags, jars, jugs, and all processing recipes.

Highlights:

- Ingots use iron-ingot-like recolored 16x16 item textures.
- Gears are produced with the Gear Mold Press + plate via Create deploying, with a fallback crafting recipe.
- Coin stacks craft from 9 coins and unpack back into 9 coins.
- Crates craft from 9 items; melon/pumpkin crates use 8 items around one plank.
- Seed bags craft from 9 seeds.
- Jams / jellies / sauces jars and fluid jugs occupy one quarter of a blockspace and stack up to 4 visible vessels per block. Sneak-right-click removes one.
- Vanilla dust tags include gunpowder, glowstone dust, and redstone.

Recipe priority system: Create > Vintage Improvements > Vanilla fallback.

### Metal slimes

RichStuff registers colored metal slime entities for AllTheOres metals that also have RichStuff ingots and nuggets. Each slime drops the matching RichStuff nugget and has biome modifiers plus Y-level spawn checks that mirror the ore's broad dimension/depth profile. Configure them in `richstuff-common.toml` under `metal_slimes`.

### Silent Gear / Silent's Gems compatibility

RichStuff includes optional data-pack compatibility for Silent Gear and Silent's Gems. All RichStuff metals, alloys, gems, and crystals are exported as Silent Gear material definitions, and RichStuff gems/crystals are exposed under Silent's Gems compatibility tags. Silent Gear / Silent's Gems remain optional runtime integrations; RichStuff does not compile against their Java APIs.

### Silent Gear molten casting

RichStuff includes Create Spout casting recipes that combine RichStuff molten metal fluid tags with Silent Gear molds to produce Silent Gear parts when Silent Gear is installed. The recipes are gated by `silent_gear.enableMoltenSilentGearCasting` (enabled by default). Vanilla tool/armor recipe conversion into Silent Gear equivalents is guarded by `silent_gear.convertVanillaToolRecipesToSilentGear` (off by default). See `docs/SILENT_GEAR_MOLTEN_CASTING.md`.

## Dependencies

### Required runtime mods

- **AllTheOres** 1.0.0 or newer
- **Create**
- **Pam's HarvestCraft 2 Food Core**
- **Pam's HarvestCraft 2 Crops**
- **Pam's HarvestCraft 2 Trees**

### Optional runtime mods

- **Create: Vintage Improvements** (`createvintageneoforged`) — for coiling, vibrating, pressurizing, polishing, and centrifugation-style recipe support. Fallback recipes are included.
- **FTB Quests** — for the included quest chapter stub.
- **Silent Gear** and **Silent's Gems** — optional data-pack compatibility.

## Building

Requires Java 21 and Gradle with NeoForge dependency access.

```bash
cd RichStuff
gradle build
```

Output jars are produced in `build/libs`. A `data` gradle task generates/refreshes data resources into `src/generated/resources`.

## Project layout

```
RichStuff/
├── src/main/                 # Mod source & resources
├── src/generated/resources/  # Generated data (recipes, tags, loot, etc.)
├── docs/                     # Supplemental documentation
├── scripts/                  # Build/helper scripts
├── gradle.properties         # Mod metadata & versions
├── build.gradle
└── settings.gradle