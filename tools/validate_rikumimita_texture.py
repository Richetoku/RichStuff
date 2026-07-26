#!/usr/bin/env python3
"""Validate a Bedrock/Blockbench-style geometry JSON against a texture and print results to stdout."""
from __future__ import annotations
import json, sys
from pathlib import Path
from PIL import Image

def visit_cubes(value):
    if isinstance(value, dict):
        if isinstance(value.get("cubes"), list):
            for cube in value["cubes"]:
                yield cube
        for child in value.values():
            yield from visit_cubes(child)
    elif isinstance(value, list):
        for child in value:
            yield from visit_cubes(child)

def rects_from_cube(cube):
    uv=cube.get("uv")
    size=cube.get("size", [0,0,0])
    if not isinstance(uv,list) or len(uv)<2 or len(size)<3: return []
    u,v=uv[:2]; x,y,z=[max(0,float(n)) for n in size[:3]]
    # Standard box-unwrapped footprint used by common Bedrock geometry exports.
    # Bedrock box UV origin is the upper-left of the complete unfolded box.
    # Side faces begin below the top/bottom strip, so all coordinates remain
    # positive even when the declared V origin is zero.
    return [
      (u + z, v + z, u + z + x, v + z + y, "north"),
      (u, v + z, u + z, v + z + y, "west"),
      (u + z + x, v + z, u + z + x + z, v + z + y, "east"),
      (u + z + x + z, v + z, u + z + x + z + x, v + z + y, "south"),
      (u + z, v, u + z + x, v + z, "up"),
      (u + z + x, v, u + z + x + x, v + z, "down")
    ]

def main():
    if len(sys.argv) not in (3, 4):
        raise SystemExit("usage: validate_rikumimita_texture.py <model.json> <texture.png> [legacy-output-directory]")
    model_path, texture_path = map(Path, sys.argv[1:3])
    model=json.loads(model_path.read_text(encoding="utf-8"))
    image=Image.open(texture_path).convert("RGBA")
    width,height=image.size
    declared=[]
    def find_desc(v):
        if isinstance(v,dict):
            if "texture_width" in v and "texture_height" in v: declared.append((v["texture_width"],v["texture_height"]))
            for c in v.values(): find_desc(c)
        elif isinstance(v,list):
            for c in v: find_desc(c)
    find_desc(model)
    errors=[]; rect_count=0
    for ci,cube in enumerate(visit_cubes(model)):
        for x1,y1,x2,y2,face in rects_from_cube(cube):
            rect_count+=1
            if x1<0 or y1<0 or x2>width or y2>height:
                errors.append(f"cube {ci} {face}: UV rectangle {(x1,y1,x2,y2)} outside {width}x{height}")
    if declared and any(tuple(map(int,d))!=(width,height) for d in declared):
        errors.append(f"declared dimensions {declared} do not match texture {width}x{height}")
    report={"model":str(model_path),"texture":str(texture_path),"texture_size":[width,height],"declared_sizes":declared,"uv_rectangles":rect_count,"errors":errors,"valid":not errors}
    print(json.dumps(report,indent=2))
    raise SystemExit(0 if not errors else 1)
if __name__=='__main__': main()
