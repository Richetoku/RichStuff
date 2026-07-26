#!/usr/bin/env python3
from pathlib import Path
from PIL import Image, ImageDraw
import json, re, colorsys, hashlib

ROOT=Path(__file__).resolve().parents[1]
CAT=ROOT/'src/main/java/com/richetoku/richstuff/RichStuffCatalog.java'
RES=ROOT/'src/main/resources'
TEX=RES/'assets/richstuff/textures/item'
MODELI=RES/'assets/richstuff/models/item'
MODELB=RES/'assets/richstuff/models/block'
KUBE=Path('/mnt/data/_kube/kubejs/assets/richstuff/textures/item')
# During normal local regeneration, fall back to bundled templates copied below.
TEMPL=ROOT/'tools/templates'
TEMPL.mkdir(parents=True, exist_ok=True)

text=CAT.read_text(encoding='utf-8')
materials={m[0]:(m[1],m[2]) for m in re.findall(r'new MaterialDef\("([^"]+)", "([^"]+)", "(#[0-9A-Fa-f]{6})"',text)}
ids=set(re.findall(r'^\s*"([a-z0-9_]+)",?$',text,re.M))

def source_or_template(rel, fallback):
    a=KUBE/rel
    b=TEMPL/fallback
    if a.exists():
        if not b.exists(): b.write_bytes(a.read_bytes())
        return a
    return b

ingot_t=Image.open(source_or_template(Path('ingot/iron.png'),'iron_ingot_template.png')).convert('RGBA')
gear_t=Image.open(source_or_template(Path('gear/iron.png'),'iron_gear_template.png')).convert('RGBA')
stack_src=TEX/'vanadium_coin_stack.png'
if stack_src.exists() and not (TEMPL/'coin_stack_template.png').exists():
    (TEMPL/'coin_stack_template.png').write_bytes(stack_src.read_bytes())
stack_t=Image.open(TEMPL/'coin_stack_template.png').convert('RGBA')

def rgb(hexv):
    return tuple(int(hexv[i:i+2],16) for i in (1,3,5))

def recolor(template, target):
    tr,tg,tb=target
    out=Image.new('RGBA',template.size)
    pix=[]
    for r,g,b,a in template.getdata():
        if a==0: pix.append((0,0,0,0)); continue
        lum=(r*0.2126+g*0.7152+b*0.0722)/255
        shade=0.38+lum*0.82
        nr=min(255,int(tr*shade)); ng=min(255,int(tg*shade)); nb=min(255,int(tb*shade))
        if lum>0.82:
            f=(lum-0.82)/0.18*0.40
            nr=int(nr+(255-nr)*f); ng=int(ng+(255-ng)*f); nb=int(nb+(255-nb)*f)
        pix.append((nr,ng,nb,a))
    out.putdata(pix)
    return out

# True ingot and common gear silhouettes.
for name,(kind,color) in materials.items():
    c=rgb(color)
    if f'{name}_ingot' in ids: recolor(ingot_t,c).save(TEX/f'{name}_ingot.png')
    if f'{name}_gear' in ids: recolor(gear_t,c).save(TEX/f'{name}_gear.png')
    if f'{name}_coin_stack' in ids: recolor(stack_t,c).save(TEX/f'{name}_coin_stack.png')

# Blisk-shaped coins with deterministic recognizable mob face embossing.
faces=['creeper','zombie','skeleton','pig','slime','enderman','blaze','cat']
def face_overlay(im,face):
    d=ImageDraw.Draw(im)
    # dark stamped metal; tiny highlight underneath gives embossing.
    opaque=[p for p in im.getdata() if p[3]]
    avg=tuple(sum(p[i] for p in opaque)//max(1,len(opaque)) for i in range(3))
    dark=tuple(max(0,int(v*.23)) for v in avg)+(255,)
    light=tuple(min(255,int(v*1.38+24)) for v in avg)+(210,)
    def rect(box):
        x1,y1,x2,y2=box; d.rectangle((x1+1,y1+1,x2+1,y2+1),fill=light); d.rectangle(box,fill=dark)
    if face=='creeper':
        rect((5,5,6,6)); rect((9,5,10,6)); rect((7,7,8,9)); rect((6,9,9,10))
    elif face=='zombie':
        rect((5,5,6,6)); rect((9,5,10,6)); rect((5,9,10,9)); rect((7,8,8,8))
    elif face=='skeleton':
        rect((5,5,6,6)); rect((9,5,10,6)); rect((7,7,8,8)); rect((6,10,6,10)); rect((8,10,8,10)); rect((10,10,10,10))
    elif face=='pig':
        rect((5,5,6,6)); rect((9,5,10,6)); rect((6,8,9,10)); d.point((7,9),fill=light); d.point((8,9),fill=light)
    elif face=='slime':
        rect((5,6,6,6)); rect((9,6,10,6)); rect((6,9,9,9))
    elif face=='enderman':
        rect((4,6,7,6)); rect((9,6,12,6)); rect((6,10,10,10))
    elif face=='blaze':
        rect((5,5,6,6)); rect((9,5,10,6)); rect((6,9,9,9)); d.line((4,3,4,11),fill=dark); d.line((12,3,12,11),fill=dark)
    else: # cat
        d.polygon([(4,6),(4,3),(7,5)],fill=dark); d.polygon([(11,5),(14,3),(14,6)],fill=dark)
        rect((5,6,6,6)); rect((10,6,11,6)); d.point((8,8),fill=dark); d.line((6,9,4,9),fill=dark); d.line((10,9,12,9),fill=dark)

for name,(kind,color) in materials.items():
    coin=TEX/f'{name}_coin.png'
    if f'{name}_coin' not in ids: continue
    blisk=KUBE/f'blisk/{name}.png'
    bundled=TEX/'blisk'/f'{name}.png'
    iron_bundled=TEX/'blisk'/'iron.png'
    if blisk.exists(): base=Image.open(blisk).convert('RGBA')
    elif bundled.exists(): base=Image.open(bundled).convert('RGBA')
    else: base=recolor(Image.open(iron_bundled).convert('RGBA'),rgb(color))
    # Preserve the blisk silhouette; only the stamped face is added.
    face=faces[int(hashlib.sha1(name.encode()).hexdigest(),16)%len(faces)]
    face_overlay(base,face); base.save(coin)

# Present item: closed wrapped gift, distinct from its open block state.
im=Image.new('RGBA',(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
d.rectangle((3,5,12,14),fill=(155,24,126,255)); d.rectangle((4,6,11,13),fill=(226,53,174,255))
d.rectangle((7,5,8,14),fill=(73,181,79,255)); d.rectangle((3,8,12,9),fill=(91,215,96,255))
d.rectangle((2,3,13,6),fill=(127,17,105,255)); d.rectangle((3,3,12,5),fill=(240,67,190,255)); d.rectangle((7,3,8,6),fill=(77,197,80,255))
d.polygon([(7,3),(4,1),(3,2),(6,4)],fill=(95,220,102,255)); d.polygon([(8,3),(11,1),(13,2),(9,4)],fill=(95,220,102,255))
d.point((4,6),fill=(255,153,224,255)); d.line((4,13,11,13),fill=(92,11,73,255))
im.save(TEX/'rikumi_mita_package.png')

# Molten fill textures and 3D filled mold models.
molten_dir=RES/'assets/richstuff/textures/block/molten_mold'; molten_dir.mkdir(parents=True,exist_ok=True)
for name,(kind,color) in materials.items():
    c=rgb(color); h,s,v=colorsys.rgb_to_hsv(*(x/255 for x in c)); hot=colorsys.hsv_to_rgb(h,min(1,s*1.12),min(1,max(v,0.65)))
    hot=tuple(int(x*255) for x in hot)
    t=Image.new('RGBA',(16,16),hot+(255,)); td=ImageDraw.Draw(t)
    for y in range(16):
        for x in range(16):
            if (x*3+y*5+len(name))%11==0: td.point((x,y),fill=tuple(min(255,k+55) for k in hot)+(255,))
            elif (x+y*2)%13==0: td.point((x,y),fill=tuple(max(0,int(k*.62)) for k in hot)+(255,))
    t.save(molten_dir/f'{name}.png')

filled=0
for ip in MODELI.glob('*filled*_mold.json'):
    stem=ip.stem
    base=None; mat=None
    if stem.startswith('filled_') and stem.endswith('_gem_mold'):
        mat=stem[len('filled_'):-len('_gem_mold')]
        cand=MODELB/f'base_{mat}_gem_mold.json'
        if cand.exists(): base=cand
    elif '_filled_' in stem:
        mat,part=stem.split('_filled_',1)
        cand=MODELB/f'silent_{part}.json'
        if not cand.exists(): cand=MODELB/f'base_{part}.json'
        if cand.exists(): base=cand
    elif stem in ('filled_shard_mold','filled_shards_mold'):
        part=stem[len('filled_'):]
        cand=MODELB/f'base_{part}.json'
        if cand.exists(): base=cand
    if base is None: continue
    data=json.loads(base.read_text(encoding='utf-8'))
    if mat in materials:
        data.setdefault('textures',{})['cavity']=f'richstuff:block/molten_mold/{mat}'
    else:
        data.setdefault('textures',{})['cavity']='minecraft:block/orange_concrete'
    # Keep block geometry as an item model; add display transforms for GUI/ground/world.
    data['display']={
      'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[0.78,0.78,0.78]},
      'ground':{'rotation':[0,0,0],'translation':[0,3,0],'scale':[0.45,0.45,0.45]},
      'fixed':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[0.7,0.7,0.7]},
      'firstperson_righthand':{'rotation':[0,45,0],'translation':[0,3,0],'scale':[0.65,0.65,0.65]},
      'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2,0],'scale':[0.55,0.55,0.55]}
    }
    block_out=MODELB/f'{stem}.json'; block_out.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    ip.write_text(json.dumps({'parent':f'richstuff:block/{stem}'},indent=2)+'\n',encoding='utf-8')
    filled+=1
print(f'Regenerated ingots/gears/coins/stacks, present, and {filled} filled 3D mold models.')
