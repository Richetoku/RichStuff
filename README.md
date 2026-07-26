# RichStuff — NeoForge 1.21.1

RichStuff is the processing, storage, food, machine, mold, and companion side of the suite. RichOres supplies the ore blocks and configurable generation.

Both mods remain internally at version **0.0.1** until Richetoku explicitly changes the version.

## Seven-tier processing

RichStuff reads the shared seven-tier material catalog. Higher tiers require progressively greater Create RPM and use type-specific processing chains:

- Metals: crush, wash, separate, refine, melt, cast, and cool.
- Gems/crystals: crush, cut, refine, polish, or grow from budding blocks.
- Dust ores: mill directly into dust plus tiny-dust byproducts.
- Utility/fuel ores: extract and purify with material-specific byproducts.

Direct vanilla-style material shortcuts are disabled by default with `enableVanillaFallbackRecipes=false`.

## Ore Extractor

The Ore Extractor is an end-game Create-powered multiblock chunk miner.

- Controller centered in a vertical 3×3 frame of Ore Extractor Casings.
- Requires water and lava catalysts in dedicated labeled slots; either buckets or matching RichStuff jugs are accepted.
- Requires Create rotation; the default minimum is 256 RPM, with migration of untouched older 1024/512 defaults.
- Six filters select ore blocks or material forms.
- Uses configurable +/- range controls from 2×2 through 48×48 blocks.
- Uses normal non-Silk loot tables and replaces mined ore with cobblestone to avoid large underground voids.
- Outputs through the rear-center connection into an inventory, chute, funnel, or belt-compatible transport.
- Displays an explicit rear-output error when nothing valid is connected.

## Data-driven material families

Material controls live at `data/<namespace>/richstuff_materials/<material>.json`. Definitions cover metal, gem, dust, fuel, utility, and crystal families. An empty highest-priority JSON object disables that family for runtime material systems; `forms` and `skipForms` provide per-form control. Registries remain a build-time superset because Minecraft freezes item and block registration before data packs reload.

`python tools/generate_material_definitions.py` creates missing definitions without overwriting authored files. Use `--refresh` only when intentionally rebuilding definitions from `RichStuffCatalog`.

## Crystals

Every RichStuff gem/crystal family has:

- budding block
- small bud
- medium bud
- large bud
- full cluster

Growth follows vanilla amethyst behavior and sounds. All stages are directional and waterloggable.

## Molds and machines

Molds are actual placeable blocks using the original hand-authored blank, unfired, and cut-part models with matching family hitboxes. Full-block molds use 1,296 mB of molten material. Create Spout recipes fill molds and Create splashing recipes cool them, returning the mold plus the cast part. Tool-part molds and normal forms such as rods, plates, wires, gears, blocks, and other parts are supported.

RichStuff machines render animated components and only process when supplied with sufficient adjacent Create RPM.

## Coins and vessels

- Coins place into a 1–9 coin pyramid: three left, three right, then three centered on top. Each coin contributes two pixels of height and the full arrangement reaches twelve pixels.
- Sneak-right-click removes one coin.
- Jars and jugs occupy one of four 6×6×16 positions with one-pixel spacing.
- Vessel collision and selection shapes include only occupied positions, so empty quarters are not targetable.
- Sneak-right-click removes the vessel at the selected quarter.

## Native Rikumi Mita

Rikumi is implemented directly in RichStuff. Ownership is persisted by Minecraft UUID. Her current in-game owner name is resolved from that UUID, with `Player` as the only fallback. No Windows account information is used.
