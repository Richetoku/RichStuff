# Rich Stuff Recipe and Use Audit

This audit describes the acquisition and use policy for the complete Rich mod-family catalog in requested revision v11.

## Scope

The catalog currently registers **3,944 IDs** across natural blocks, material forms, crafting components, tools, equipment, machines, chambers, vessels, tanks, crates, casting states, and compatibility content.

The recipe/use scan found **804 `*_filled_*_mold` IDs** that are intentionally not ordinary hand-crafting inputs or outputs. These are transient Foundry casting states used while molten material occupies a mold. They are hidden machine-state forms, not missing player recipes.

Every ordinary catalog entry is assigned at least one meaningful acquisition or consumption role through one or more of the following categories:

- natural/world-generation or mob-drop acquisition;
- crate packing/unpacking;
- furnace, blasting, washing, crushing, compacting, cutting, sanding, laser, centrifuge, or Foundry processing;
- tool, weapon, armor, machine, chamber, tank, casing, controller, or upgrade construction;
- compatibility-tag participation for recipes supplied by Create or another installed mod.

This does not mean every natural ore, plant, or mob drop is hand-craftable. Natural content is acquired from the world and then used by processing or construction recipes.

## Recipe design rules

1. **RichCore is the only mandatory shared dependency.**
2. **Create is preferred but optional.** Create-shaped factory recipes are condition-gated, with sensible vanilla or native Rich Machine fallback recipes where necessary.
3. **Other Rich modules are optional.** A recipe may prefer Rich Stuff plates, rods, springs, gears, tanks, or materials only when that module is loaded.
4. **Components have downstream uses.** Plates, rods, springs, gears, wires, dusts, shards, polished gems, and similar forms feed tools, armor parts, machine frames, controllers, upgrades, or later processing stages.
5. **No duplicate fluid ecosystem is forced.** Existing external fluids remain canonical through shared tags; Rich Stuff fallback fluids are used only when no compatible fluid exists.

## Crates

- Valid compactable resources have a crate recipe and a reverse recipe that returns nine stored items.
- Crate wood rebasing uses an exact 3×3 cross: the existing crate in the center and four identical target planks on the top, bottom, left, and right.
- Sugar cane, sticks, bamboo, kelp, dried kelp, and mob drops use their normal item/block forms instead of generated crate variants.
- Existing ordinary storage blocks remain ordinary blocks rather than duplicate crates where appropriate.

## Processing progression

Metals and gems use the staged progression documented in `RichMachines/ORE_PROCESSING_PROGRESSION.md`. Direct smelting remains a lower-yield escape route, while additional Create/Rich Machines operations increase expected output and give intermediate forms a production purpose.

## Machine and structure ingredients

Machine recipes prioritize thematic components:

- springs for moving assemblies;
- plates for casings and structural shells;
- rods/shafts for kinetic connections;
- gears/cogs for mechanical processing;
- tanks and fluid components for Foundry/fluid machines;
- gems, crystals, and precision parts for laser and advanced control systems.

Fallback recipes avoid hard dependencies but retain comparable material cost and progression position.
