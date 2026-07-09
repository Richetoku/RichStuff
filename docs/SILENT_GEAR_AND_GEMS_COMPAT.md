# Silent Gear and Silent's Gems compatibility

RichStuff ships data-only compatibility for Silent Gear 4.x and Silent's Gems on NeoForge 1.21.1.

## Silent Gear

Generated material files live in:

```text
src/main/resources/data/richstuff/silentgear_materials/*.json
```

Each RichStuff metal, alloy, gem, and crystal has a `silentgear:simple` material definition using the appropriate common tag:

- metals/alloys: `c:ingots/<material>`
- gems/crystals: `c:gems/<material>`
- rods/tips are supplied through RichStuff `c:rods/<material>`, `c:nuggets/<material>`, and `c:shards/<material>` tags where those items exist.

Generated material count: 62

- metals/alloys: 38
- gems/crystals: 24

## Silent's Gems

RichStuff also exposes gem and gem block tags under the `silentgems` namespace so Silent's Gems add-ons or modpacks can discover RichStuff gems without hard-coding item IDs:

```text
data/silentgems/tags/item/gems/*.json
data/silentgems/tags/item/gem_blocks/*.json
data/silentgems/tags/block/gem_blocks/*.json
```

These tags are optional compatibility data. They do not make Silent's Gems required.
