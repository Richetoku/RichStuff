# Conversion notes

Source pack: `/mnt/data/kubejs.zip`

NeoForge 1.21.1 setup was based on the public NeoForge 1.21.1 MDK/ModDevGradle pattern. NeoForge docs state Java 21 is required and that `gradlew build` emits the jar in `build/libs`. The MDK repository for 1.21.1 uses ModDevGradle and the current sample properties list NeoForge `21.1.235`.

Converted systems:

- `startup_scripts/richstuff_startup.js`: material catalog.
- `startup_scripts/richstuff_items.js`: generated material parts, blocks, molds, crates/bags.
- `startup_scripts/richstuff_molten.js`: mold/fluid assets copied; recipe path represented in Create JSON.
- `server_scripts/richstuff_recipes.js`: generated machine/processing recipe families.
- `server_scripts/richstuff_tags.js`: common `c` tags generated for item/block unification.
- `server_scripts/create.js`: Create recipes are gated with `neoforge:mod_loaded` condition.

This is a first-pass KubeJS-to-Java conversion. The largest behavioral implementation is stackable jars/jugs and budding crystal growth. More complex custom machine GUIs/block entities were represented as blocks and Create recipe endpoints because the KubeJS scripts mostly define recipe-machine steps rather than complete custom GUI machines.
