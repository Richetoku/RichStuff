# Native Rikumi Mita and material visuals — 0.0.5

Rikumi Mita is a native RichStuff entity. Her model is compiled into `RikumiMitaModel`, her renderer selects RichStuff outfit atlases, and her server-backed menu is owner-only. There is no external maid entity, model pack, or GUI dependency.

The placeable present stores the placer's UUID. Its first right-click opens the lid; its scheduled server tick creates Rikumi, assigns the owner, plays a harmless burst, removes the present, and leaves the native companion in the world.

Material visuals use shared silhouettes for consistency while preserving each material's configured color. Coins use blisk silhouettes with mob-face embossing. Filled molds use actual block-model geometry rather than flat generated item sprites.
