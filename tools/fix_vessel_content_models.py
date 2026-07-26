#!/usr/bin/env python3
"""Seal the visible upper corners of every filled RichStuff jar/jug model."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODELS = ROOT / "src/main/resources/assets/richstuff/models/block/vessels_centered"


def cube(start: list[float], end: list[float]) -> dict:
    return {
        "from": start,
        "to": end,
        "faces": {face: {"texture": "#content"} for face in ("north", "south", "east", "west", "up", "down")},
    }


def uses_content(element: dict) -> bool:
    return any(face.get("texture") == "#content" for face in element.get("faces", {}).values())


def patch(path: Path) -> bool:
    model = json.loads(path.read_text(encoding="utf-8"))
    if "content" not in model.get("textures", {}):
        return False
    elements = model.get("elements", [])
    content_indices = [i for i, element in enumerate(elements) if uses_content(element)]
    if not content_indices:
        return False

    is_jug = path.stem.endswith("_jug")
    body_index = content_indices[0]
    if is_jug:
        elements[body_index]["from"] = [5.72, 1.0, 5.72]
        elements[body_index]["to"] = [9.78, 13.25, 10.28]
        neck = cube([6.48, 12.9, 6.48], [8.92, 15.05, 9.52])
    else:
        elements[body_index]["from"] = [5.68, 1.0, 5.68]
        elements[body_index]["to"] = [10.32, 13.45, 10.32]
        neck = cube([6.08, 13.15, 6.08], [9.92, 14.82, 9.92])

    # Keep exactly one body and one neck content cuboid. Insert the neck before the lid/glass-neck
    # geometry so translucent sorting sees contents behind all exterior faces.
    for i in reversed(content_indices[1:]):
        del elements[i]
    elements.insert(body_index + 1, neck)
    path.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
    return True


def main() -> None:
    patched = sum(patch(path) for path in sorted(MODELS.glob("*.json")))
    print(f"Patched {patched} filled vessel models")


if __name__ == "__main__":
    main()
