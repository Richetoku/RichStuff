# Silent Gear molten casting and vanilla recipe conversion

RichStuff now includes a server-configurable Silent Gear bridge.

## Server config

`RichStuffConfig` adds these server/common settings:

- `silent_gear.enableMoltenSilentGearCasting = true`
- `silent_gear.convertVanillaToolRecipesToSilentGear = false`

The vanilla conversion option is intentionally **off by default**. When enabled, RichStuff adds guarded vanilla-pattern recipes and a server-side crafted-item bridge that replaces vanilla tool/armor outputs with Silent Gear item IDs when Silent Gear is loaded.

## Molten material identity

Generated molten material metadata lives in:

```text
src/main/resources/data/richstuff/silentgear_molten_materials/*.json
```

Each file maps a RichStuff molten metal fluid tag to the matching RichStuff Silent Gear material definition and marks `grade_passthrough: true`. This is the compatibility layer for keeping Silent Gear grade/modifier data attached when molten metals are cast back into Silent Gear parts.

## Create casting recipes

Generated Create Spout recipes live in:

```text
src/main/resources/data/richstuff/recipe/silent_gear/molten_casting/<material>/<part>.json
```

They are gated behind:

- `create` loaded
- `silentgear` loaded
- `richstuff:server_config` with `enableMoltenSilentGearCasting = true`
- target Silent Gear part item registered

Generated molten metals: 38
Generated molten casting recipes: 494
Generated guarded vanilla-pattern recipes: 342
