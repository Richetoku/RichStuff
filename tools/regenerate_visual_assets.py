#!/usr/bin/env python3
"""Regenerate RichStuff ingots, budding blocks, and full-height mixed vessel models."""
from __future__ import annotations

import json
import math
import random
import re
from pathlib import Path
from typing import Iterable

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
JAVA_CATALOG = ROOT / "src/main/java/com/richetoku/richstuff/RichStuffCatalog.java"
ASSETS = ROOT / "src/main/resources/assets/richstuff"


def extract_array(source: str, name: str) -> list[str]:
    match = re.search(
        rf"public static final String\[\] {re.escape(name)} = new String\[\] \{{(.*?)\n    \}};",
        source,
        re.S,
    )
    if not match:
        raise RuntimeError(f"Could not find {name}")
    return re.findall(r'"([^"]+)"', match.group(1))


def material_colors(source: str) -> dict[str, tuple[int, int, int]]:
    result: dict[str, tuple[int, int, int]] = {}
    for name, hex_color in re.findall(
        r'new MaterialDef\("([^"]+)",\s*"[^"]+",\s*"#([0-9A-Fa-f]{6})"', source
    ):
        result[name] = tuple(int(hex_color[i : i + 2], 16) for i in (0, 2, 4))
    return result


def opaque_average(image: Image.Image) -> tuple[int, int, int]:
    pixels = [(r, g, b) for r, g, b, a in image.convert("RGBA").getdata() if a > 32]
    if not pixels:
        return 180, 180, 180
    return tuple(round(sum(p[index] for p in pixels) / len(pixels)) for index in range(3))


def recolor_ingots(colors: dict[str, tuple[int, int, int]]) -> int:
    item_dir = ASSETS / "textures/item"
    base_path = item_dir / "iron_ingot.png"
    base = Image.open(base_path).convert("RGBA")
    opaque_lums = [
        (0.2126 * r + 0.7152 * g + 0.0722 * b)
        for r, g, b, a in base.getdata()
        if a > 0
    ]
    low, high = min(opaque_lums), max(opaque_lums)
    changed = 0

    for path in sorted(item_dir.glob("*_ingot.png")):
        material = path.stem[: -len("_ingot")]
        if material == "iron":
            continue
        old = Image.open(path).convert("RGBA")
        target = colors.get(material, opaque_average(old))
        out = Image.new("RGBA", base.size)
        out_pixels = []
        for r, g, b, a in base.getdata():
            if a == 0:
                out_pixels.append((0, 0, 0, 0))
                continue
            lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
            t = 0.5 if high == low else (lum - low) / (high - low)
            # Preserve the exact iron-ingot shading pattern while tinting the palette.
            shade = 0.48 + 0.62 * t
            highlight = max(0.0, (t - 0.70) / 0.30) * 0.32
            rgb = []
            for channel in target:
                value = channel * shade
                value = value * (1.0 - highlight) + 255.0 * highlight
                rgb.append(max(0, min(255, round(value))))
            out_pixels.append((*rgb, a))
        out.putdata(out_pixels)
        out.save(path)
        changed += 1
    return changed


def nearest_material_texture(material: str) -> Path | None:
    candidates = [
        ASSETS / f"textures/block/block/{material}.png",
        ASSETS / f"textures/block/crystal/{material}.png",
        ASSETS / f"textures/block/raw_block/{material}.png",
        ASSETS / f"textures/item/{material}_gem.png",
        ASSETS / f"textures/item/{material}_crystal.png",
        ASSETS / f"textures/item/{material}.png",
    ]
    return next((path for path in candidates if path.exists()), None)


def make_budding_texture(material: str, source: Path | None) -> Image.Image:
    if source:
        base = Image.open(source).convert("RGBA").resize((16, 16), Image.Resampling.NEAREST)
    else:
        base = Image.new("RGBA", (16, 16), (108, 102, 118, 255))
    avg = opaque_average(base)
    rng = random.Random(material)
    pixels = base.load()

    # Darken the host slightly so the embedded buds remain visible.
    for y in range(16):
        for x in range(16):
            r, g, b, a = pixels[x, y]
            noise = rng.randint(-10, 8)
            pixels[x, y] = (
                max(0, min(255, round(r * 0.72) + noise)),
                max(0, min(255, round(g * 0.72) + noise)),
                max(0, min(255, round(b * 0.72) + noise)),
                a,
            )

    bright = tuple(min(255, round(c * 1.35 + 28)) for c in avg)
    medium = tuple(min(255, round(c * 1.12 + 12)) for c in avg)
    dark = tuple(max(0, round(c * 0.62)) for c in avg)
    clusters = [(2, 3), (11, 2), (6, 7), (13, 10), (3, 13), (9, 14)]
    for cx, cy in clusters:
        for dx, dy, color in [
            (0, 0, bright),
            (1, 0, medium),
            (0, 1, medium),
            (-1, 0, dark),
            (0, -1, dark),
        ]:
            x, y = cx + dx, cy + dy
            if 0 <= x < 16 and 0 <= y < 16:
                pixels[x, y] = (*color, 255)
    return base


def regenerate_budding_blocks() -> int:
    model_dir = ASSETS / "models/block"
    texture_dir = ASSETS / "textures/block/budding"
    texture_dir.mkdir(parents=True, exist_ok=True)
    count = 0
    for model_path in sorted(model_dir.glob("budding_*_crystal.json")):
        block_id = model_path.stem
        material = block_id[len("budding_") : -len("_crystal")]
        texture = make_budding_texture(material, nearest_material_texture(material))
        texture.save(texture_dir / f"{material}.png")
        model = {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": f"richstuff:block/budding/{material}"},
        }
        model_path.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
        item_model = ASSETS / f"models/item/{block_id}.json"
        item_model.write_text(
            json.dumps({"parent": f"richstuff:block/{block_id}"}, indent=2) + "\n",
            encoding="utf-8",
        )
        count += 1
    return count


FRUIT_COLORS: dict[str, str] = {
    "apple": "#B83D35", "apricot": "#EE9845", "banana": "#E7CE55",
    "blackberry": "#3C244F", "blueberry": "#3F4F9B", "breadfruit": "#C6A96A",
    "cactusfruit": "#C33E83", "candleberry": "#93B55B", "cantaloupe": "#E68F4C",
    "cherry": "#B91F3B", "chocolate": "#633A28", "chorus_fruit": "#8D5AA7",
    "cloudberry": "#E39A2D", "cranberry": "#A51E35", "cream_frosting": "#F2E8D2",
    "date": "#75452C", "dragonfruit": "#D63B83", "elderberry": "#392B58",
    "fig": "#754563", "fruit": "#C84F55", "glow_berry": "#E7A72B",
    "golden_apple": "#E1B93B", "gooseberry": "#88A744", "grape": "#67428A",
    "grapefruit": "#E97172", "greengrape": "#8DBB4A", "guava": "#E16E73",
    "huckleberry": "#3D477D", "jackfruit": "#D9B643", "juniperberry": "#35496F",
    "kiwi": "#6F9E3A", "lemon": "#E8D746", "lime": "#70A93B",
    "lychee": "#D98993", "mango": "#E69A2C", "melon": "#D95A63",
    "mulberry": "#642443", "orange": "#E78128", "papaya": "#E67D36",
    "passionfruit": "#7A3B74", "pawpaw": "#D7AE43", "peach": "#E99A79",
    "pear": "#A8B85B", "persimmon": "#D97028", "pineapple": "#D6B739",
    "plum": "#633C72", "pomegranate": "#AD293F", "rambutan": "#C83F4C",
    "raspberry": "#B6254A", "soursop": "#9BAF64", "starfruit": "#D8C442",
    "strawberry": "#C93445", "sweet_berry": "#B52B43", "tamarind": "#70432B",
    "wolfberry": "#D45432",
}

JUG_COLORS: dict[str, str] = {
    "milk_jug": "#EEECE2", "water_jug": "#4D9DDA", "honey_jug": "#D89524",
    "lava_jug": "#E84C19", "cream_jug": "#F0E4C9", "chocolate_milk_jug": "#81553C",
    "apple_cider_jug": "#B86B2D", "juice_jug": "#DD6D31", "maple_syrup_jug": "#8C431F",
    "olive_oil_jug": "#B6A63C",
}


def hex_rgb(value: str) -> tuple[int, int, int]:
    value = value.lstrip("#")
    return tuple(int(value[i : i + 2], 16) for i in (0, 2, 4))


def jar_base(item_id: str) -> str:
    suffixes = ["_cream_frosting_jar", "_jam_jar", "_jelly_jar", "_sauce_jar", "_jar"]
    for suffix in suffixes:
        if item_id.endswith(suffix):
            return item_id[: -len(suffix)]
    return item_id


def adjusted_vessel_color(item_id: str) -> tuple[int, int, int]:
    if item_id in JUG_COLORS:
        return hex_rgb(JUG_COLORS[item_id])
    base = jar_base(item_id)
    color = hex_rgb(FRUIT_COLORS.get(base, "#A14E6B"))
    if "cream_frosting" in item_id:
        return tuple(round(c * 0.35 + 255 * 0.65) for c in color)
    if "jelly" in item_id:
        return tuple(min(255, round(c * 1.10 + 8)) for c in color)
    if "sauce" in item_id:
        return tuple(round(c * 0.72 + 210 * 0.28) for c in color)
    if "jam" in item_id:
        return tuple(round(c * 0.82) for c in color)
    return color


def make_content_texture(color: tuple[int, int, int], seed: str) -> Image.Image:
    rng = random.Random(seed)
    image = Image.new("RGBA", (16, 16))
    px = image.load()
    for y in range(16):
        for x in range(16):
            wave = math.sin((x + y) * 0.8) * 4
            noise = rng.randint(-5, 5)
            shade = wave + noise + (3 if y < 4 else -2 if y > 12 else 0)
            px[x, y] = tuple(max(0, min(255, round(c + shade))) for c in color) + (255,)
    return image


def cube(from_xyz: Iterable[float], to_xyz: Iterable[float], texture: str) -> dict:
    return {
        "from": list(from_xyz),
        "to": list(to_xyz),
        "faces": {face: {"texture": texture} for face in ("north", "south", "east", "west", "up", "down")},
    }


def vessel_elements(kind: str, bounds: tuple[float, float, float, float]) -> list[dict]:
    x0, z0, x1, z1 = bounds
    if kind == "jug":
        return [
            cube((x0 + 0.5, 0.5, z0 + 0.5), (x1 - 1.0, 13.4, z1 - 0.5), "#glass"),
            cube((x0 + 1.1, 1.0, z0 + 1.1), (x1 - 1.6, 12.5, z1 - 1.1), "#content"),
            cube((x0 + 1.4, 13.0, z0 + 1.4), (x1 - 2.0, 15.4, z1 - 1.4), "#glass"),
            cube((x0 + 1.1, 15.0, z0 + 1.1), (x1 - 1.7, 16.0, z1 - 1.1), "#lid"),
            # Compact handle contained inside its quarter footprint.
            cube((x1 - 1.4, 8.0, z0 + 1.0), (x1 - 0.3, 13.0, z0 + 2.0), "#glass"),
            cube((x1 - 1.4, 8.0, z1 - 2.0), (x1 - 0.3, 13.0, z1 - 1.0), "#glass"),
            cube((x1 - 1.4, 12.0, z0 + 1.0), (x1 - 0.3, 13.0, z1 - 1.0), "#glass"),
        ]
    return [
        cube((x0 + 0.4, 0.5, z0 + 0.4), (x1 - 0.4, 14.0, z1 - 0.4), "#glass"),
        cube((x0 + 1.0, 1.0, z0 + 1.0), (x1 - 1.0, 12.8, z1 - 1.0), "#content"),
        cube((x0 + 1.0, 13.2, z0 + 1.0), (x1 - 1.0, 15.2, z1 - 1.0), "#glass"),
        cube((x0 + 0.7, 14.8, z0 + 0.7), (x1 - 0.7, 16.0, z1 - 0.7), "#lid"),
    ]


def write_vessel_support_textures() -> None:
    tex_dir = ASSETS / "textures/block/vessels"
    tex_dir.mkdir(parents=True, exist_ok=True)

    glass = Image.new("RGBA", (16, 16), (188, 224, 236, 54))
    px = glass.load()
    for i in range(16):
        px[0, i] = (224, 246, 252, 145)
        px[15, i] = (121, 164, 183, 100)
        px[i, 0] = (235, 250, 255, 150)
        px[i, 15] = (112, 152, 170, 100)
    for i in range(2, 14):
        px[3, i] = (240, 253, 255, 75)
    glass.save(tex_dir / "glass.png")

    lid = Image.new("RGBA", (16, 16), (121, 124, 130, 255))
    lp = lid.load()
    for y in range(16):
        for x in range(16):
            v = 24 if (x + y) % 4 == 0 else -12 if (x - y) % 5 == 0 else 0
            lp[x, y] = tuple(max(0, min(255, c + v)) for c in (121, 124, 130)) + (255,)
    lid.save(tex_dir / "lid.png")
    glass.save(tex_dir / "neck.png")


def regenerate_vessels(jars: list[str], jugs: list[str]) -> int:
    write_vessel_support_textures()
    content_dir = ASSETS / "textures/block/vessels"
    model_item_dir = ASSETS / "models/item"
    model_block_dir = ASSETS / "models/block"
    blockstate_dir = ASSETS / "blockstates"
    slot_bounds = [(1, 1, 7, 7), (9, 1, 15, 7), (1, 9, 7, 15), (9, 9, 15, 15)]

    for item_id in [*jars, *jugs]:
        kind = "jug" if item_id in jugs else "jar"
        color = adjusted_vessel_color(item_id)
        make_content_texture(color, item_id).save(content_dir / f"{item_id}_content.png")

        textures = {
            "particle": f"richstuff:block/vessels/{item_id}_content",
            "glass": "richstuff:block/vessels/glass",
            "content": f"richstuff:block/vessels/{item_id}_content",
            "lid": "richstuff:block/vessels/lid",
        }

        # Full-height 3D inventory/item model.
        item_bounds = (2.0, 2.0, 14.0, 14.0)
        item_model = {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:translucent",
            "textures": textures,
            "elements": vessel_elements(kind, item_bounds),
            "display": {
                "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.72, 0.72, 0.72]},
                "ground": {"translation": [0, 3, 0], "scale": [0.42, 0.42, 0.42]},
                "fixed": {"rotation": [0, 180, 0], "scale": [1.0, 1.0, 1.0]},
                "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.45, 0.45, 0.45]},
                "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.55, 0.55, 0.55]},
            },
        }
        model_item_dir.joinpath(f"{item_id}.json").write_text(json.dumps(item_model, indent=2) + "\n")

        # Full-height legacy homogeneous block models, retained for old worlds.
        for count in range(1, 5):
            elements: list[dict] = []
            for bounds in slot_bounds[:count]:
                elements.extend(vessel_elements(kind, bounds))
            block_model = {
                "parent": "minecraft:block/block",
                "render_type": "minecraft:translucent",
                "textures": textures,
                "elements": elements,
            }
            model_block_dir.joinpath(f"{item_id}_{count}.json").write_text(json.dumps(block_model, indent=2) + "\n")
        blockstate = {
            "variants": {f"count={count}": {"model": f"richstuff:block/{item_id}_{count}"} for count in range(1, 5)}
        }
        blockstate_dir.joinpath(f"{item_id}.json").write_text(json.dumps(blockstate, indent=2) + "\n")

    # Invisible host model used only for particles/model baking; BER renders actual items.
    blockstate_dir.joinpath("vessel_cluster.json").write_text(
        json.dumps({"variants": {"": {"model": "richstuff:block/vessel_cluster"}}}, indent=2) + "\n"
    )
    model_block_dir.joinpath("vessel_cluster.json").write_text(
        json.dumps({
            "parent": "minecraft:block/block",
            "textures": {"particle": "richstuff:block/vessels/glass"},
            "elements": [],
        }, indent=2) + "\n"
    )
    return len(jars) + len(jugs)


def main() -> None:
    source = JAVA_CATALOG.read_text(encoding="utf-8")
    jars = extract_array(source, "STACKABLE_JARS")
    jugs = extract_array(source, "STACKABLE_JUGS")
    colors = material_colors(source)
    ingots = recolor_ingots(colors)
    budding = regenerate_budding_blocks()
    vessels = regenerate_vessels(jars, jugs)
    print(f"Recolored {ingots} ingots")
    print(f"Regenerated {budding} budding blocks")
    print(f"Regenerated {vessels} full-height vessel item/block models")


if __name__ == "__main__":
    main()
