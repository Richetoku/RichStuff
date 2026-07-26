# RichStuff 0.0.5 — Rikumi inventory layout

This patch rebuilds Rikumi Mita's owner-only inventory screen to prevent the portrait,
labels, controls, and slot grids from overlapping.

Changes:

- Enlarged the custom screen to 308×232 GUI pixels.
- Removed the mixed vanilla-inventory background that caused clipped labels and stray UI elements.
- Aligned all real menu slots with the screen's rendered slot backplates using shared constants.
- Moved Rikumi's portrait into a dedicated inset panel.
- Replaced the two full-width outfit controls with a compact selector:
  `◀  Outfit  ▶`.
- Kept the current outfit name centered below the selector.
- Separated Sit/Follow, Voice, and Nameplate into consistently sized controls.
