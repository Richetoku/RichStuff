#!/usr/bin/env python3
from __future__ import annotations
import json, math, re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
MODEL=ROOT/'src/main/resources/assets/richstuff/models/entity/rikumi_mita.json'
OUT=ROOT/'src/main/java/com/richetoku/richstuff/rikumimita/client/RikumiMitaModel.java'

def safe(name:str)->str:
    s=re.sub(r'[^A-Za-z0-9_]', '_', name)
    if s and s[0].isdigit(): s='bone_'+s
    return s

def f(v:float)->str:
    if abs(v)<1e-8: v=0.0
    s=f'{v:.5f}'.rstrip('0').rstrip('.')
    if '.' not in s: s += '.0'
    return s+'F'

def rad(v:float)->float:
    return math.radians(v)

data=json.loads(MODEL.read_text(encoding='utf-8'))['minecraft:geometry'][0]
desc=data['description']
bones=data['bones']
by={b['name']:b for b in bones}
# Ensure parent before child (source already is, but make deterministic)
ordered=[]; pending=bones[:]
while pending:
    progressed=False
    for b in pending[:]:
        if not b.get('parent') or b['parent'] in {x['name'] for x in ordered}:
            ordered.append(b); pending.remove(b); progressed=True
    if not progressed:
        ordered += pending; break

lines=[]
lines += [
'package com.richetoku.richstuff.rikumimita.client;',
'',
'import com.mojang.blaze3d.vertex.PoseStack;',
'import com.mojang.blaze3d.vertex.VertexConsumer;',
'import com.richetoku.richstuff.RichStuff;',
'import com.richetoku.richstuff.rikumimita.RikumiMitaEntity;',
'import net.minecraft.client.model.EntityModel;',
'import net.minecraft.client.model.geom.ModelLayerLocation;',
'import net.minecraft.client.model.geom.ModelPart;',
'import net.minecraft.client.model.geom.PartPose;',
'import net.minecraft.client.model.geom.builders.CubeDeformation;',
'import net.minecraft.client.model.geom.builders.CubeListBuilder;',
'import net.minecraft.client.model.geom.builders.LayerDefinition;',
'import net.minecraft.client.model.geom.builders.MeshDefinition;',
'import net.minecraft.client.model.geom.builders.PartDefinition;',
'import net.minecraft.resources.ResourceLocation;',
'import net.minecraft.util.Mth;',
'',
'/** Native RichStuff model generated from the supplied Bedrock model JSON. */',
'public final class RikumiMitaModel extends EntityModel<RikumiMitaEntity> {',
'    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(',
'            ResourceLocation.fromNamespaceAndPath(RichStuff.MODID, "rikumi_mita"), "main");',
'    private final ModelPart root;',
'    private final ModelPart allHead;',
'    private final ModelPart leftArm;',
'    private final ModelPart rightArm;',
'    private final ModelPart leftLeg;',
'    private final ModelPart rightLeg;',
'',
'    public RikumiMitaModel(ModelPart root) {',
'        this.root = root.getChild("Root");',
'        this.allHead = find(this.root, "AllHead");',
'        this.leftArm = find(this.root, "LeftArm");',
'        this.rightArm = find(this.root, "RightArm");',
'        this.leftLeg = find(this.root, "LeftLeg");',
'        this.rightLeg = find(this.root, "RightLeg");',
'    }',
'',
'    private static ModelPart find(ModelPart part, String name) {',
'        if (part.hasChild(name)) return part.getChild(name);',
'        for (ModelPart child : part.getAllParts().toList()) {',
'            if (child != part && child.hasChild(name)) return child.getChild(name);',
'        }',
'        return part;',
'    }',
'',
'    public static LayerDefinition createBodyLayer() {',
'        MeshDefinition mesh = new MeshDefinition();',
'        PartDefinition meshRoot = mesh.getRoot();',
]

var_for={}
for idx,b in enumerate(ordered):
    name=b['name']; var='p_'+safe(name)+'_'+str(idx); var_for[name]=var
    parent=b.get('parent')
    pivot=b.get('pivot',[0,0,0]); rot=b.get('rotation',[0,0,0])
    if parent:
        pp=by[parent].get('pivot',[0,0,0])
        ox=pivot[0]-pp[0]; oy=-(pivot[1]-pp[1]); oz=pivot[2]-pp[2]
        pvar=var_for[parent]
    else:
        ox=pivot[0]; oy=24.0-pivot[1]; oz=pivot[2]; pvar='meshRoot'
    rx,ry,rz=rot
    lines.append(f'        PartDefinition {var} = {pvar}.addOrReplaceChild("{name}", CubeListBuilder.create(),')
    lines.append(f'                PartPose.offsetAndRotation({f(ox)}, {f(oy)}, {f(oz)}, {f(-rad(rx))}, {f(-rad(ry))}, {f(rad(rz))}));')
    for ci,c in enumerate(b.get('cubes',[])):
        origin=c.get('origin',[0,0,0]); size=c.get('size',[0,0,0]); uv=c.get('uv',[0,0])
        cp=c.get('pivot',pivot); cr=c.get('rotation',[0,0,0]); inflate=float(c.get('inflate',0.0)); mirror=bool(c.get('mirror',False))
        cx=cp[0]-pivot[0]; cy=-(cp[1]-pivot[1]); cz=cp[2]-pivot[2]
        lx=origin[0]-cp[0]; ly=cp[1]-origin[1]-size[1]; lz=origin[2]-cp[2]
        builder='CubeListBuilder.create()'
        if mirror: builder += '.mirror()'
        builder += f'.texOffs({int(uv[0])}, {int(uv[1])}).addBox({f(lx)}, {f(ly)}, {f(lz)}, {f(size[0])}, {f(size[1])}, {f(size[2])}, new CubeDeformation({f(inflate)}))'
        child=f'cube_{idx}_{ci}'
        lines.append(f'        {var}.addOrReplaceChild("{child}", {builder},')
        lines.append(f'                PartPose.offsetAndRotation({f(cx)}, {f(cy)}, {f(cz)}, {f(-rad(cr[0]))}, {f(-rad(cr[1]))}, {f(rad(cr[2]))}));')

lines += [
 f'        return LayerDefinition.create(mesh, {int(desc.get("texture_width",256))}, {int(desc.get("texture_height",256))});',
'    }',
'',
'    @Override',
'    public void setupAnim(RikumiMitaEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {',
'        this.root.getAllParts().forEach(ModelPart::resetPose);',
'        this.allHead.yRot += netHeadYaw * Mth.DEG_TO_RAD;',
'        this.allHead.xRot += headPitch * Mth.DEG_TO_RAD;',
'        float walk = Mth.cos(limbSwing * 0.6662F) * 1.15F * limbSwingAmount;',
'        this.rightArm.xRot += walk;',
'        this.leftArm.xRot -= walk;',
'        this.rightLeg.xRot -= walk;',
'        this.leftLeg.xRot += walk;',
'        if (entity.isOrderedToSit()) {',
'            this.rightLeg.xRot = -1.15F;',
'            this.leftLeg.xRot = -1.15F;',
'            this.rightArm.xRot *= 0.25F;',
'            this.leftArm.xRot *= 0.25F;',
'        }',
'    }',
'',
'    @Override',
'    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {',
'        this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);',
'    }',
'}',
]
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text('\n'.join(lines)+'\n',encoding='utf-8')
print(f'Generated {OUT} with {sum(len(b.get("cubes",[])) for b in ordered)} cubes and {len(ordered)} bones')
