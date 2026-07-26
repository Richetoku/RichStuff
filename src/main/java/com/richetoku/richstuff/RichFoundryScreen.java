package com.richetoku.richstuff;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/** Non-overlapping full Foundry storage, validation and fluid-selection interface. */
public final class RichFoundryScreen extends AbstractContainerScreen<RichFoundryMenu> {
    private final Component inventoryTitle;
    private Button cycle;
    public RichFoundryScreen(RichFoundryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        inventoryTitle = inventory.getDisplayName();
        imageWidth=RichFoundryMenu.WIDTH; imageHeight=RichFoundryMenu.HEIGHT;
        titleLabelX=10; titleLabelY=8; inventoryLabelX=49; inventoryLabelY=138;
    }
    @Override protected void init() {
        super.init();
        cycle=addRenderableWidget(Button.builder(Component.translatable("gui.richstuff.foundry.cycle"), b->{
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);
        }).bounds(leftPos+174,topPos+111,74,20).build());
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF171717);
        g.fill(leftPos+7,topPos+25,leftPos+82,topPos+116,0xFF2B2B2B);
        g.fill(leftPos+90,topPos+25,leftPos+252,topPos+136,0xFF242424);
        g.fill(leftPos+47,topPos+147,leftPos+213,topPos+229,0xFF303030);
        for(int row=0;row<3;row++)for(int col=0;col<3;col++)slot(g,leftPos+23+col*18,topPos+51+row*18);
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)slot(g,leftPos+48+col*18,topPos+149+row*18);
        for(int col=0;col<9;col++)slot(g,leftPos+48+col*18,topPos+207);
        int barH=Math.round(92.0F*Math.min(1.0F,menu.totalAmount()/(float)Math.max(1,menu.totalCapacity())));
        g.fill(leftPos+94,topPos+130-barH,leftPos+108,topPos+130,0xFF9B2020);
        g.renderOutline(leftPos+93,topPos+37,16,94,0xFFBDBDBD);
    }
    private static void slot(GuiGraphics g,int x,int y){g.fill(x,y,x+18,y+18,0xFF111111);g.renderOutline(x,y,18,18,0xFF707070);}
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        g.drawString(font,title,titleLabelX,titleLabelY,0xFFFFFF,false);
        Component validation=menu.formed()?Component.translatable("gui.richstuff.foundry.valid",menu.width(),menu.depth(),menu.height()).withStyle(ChatFormatting.GREEN)
                :Component.translatable("gui.richstuff.foundry.invalid").withStyle(ChatFormatting.RED);
        g.drawString(font,validation,10,28,0xFFFFFF,false);
        g.drawString(font,Component.translatable("gui.richstuff.foundry.inputs"),10,40,0xD8D8D8,false);
        g.drawString(font,Component.translatable("gui.richstuff.foundry.storage"),116,28,0xD8D8D8,false);
        FluidStack selected=menu.foundry().selectedFluidStack();
        Component fluid=selected.isEmpty()?Component.translatable("gui.richstuff.foundry.no_fluid"):
                Component.translatable("gui.richstuff.foundry.selected",selected.getHoverName(),menu.selectedAmount());
        g.drawString(font,fluid,116,42,0xFFFFFF,false);
        g.drawString(font,Component.translatable("gui.richstuff.foundry.base_capacity",menu.baseCapacity()),116,57,0xCFCFCF,false);
        g.drawString(font,Component.translatable("gui.richstuff.foundry.tank_capacity",menu.tankCapacity(),menu.tankCount()),116,70,0xCFCFCF,false);
        g.drawString(font,Component.translatable("gui.richstuff.foundry.total_capacity",menu.totalAmount(),menu.totalCapacity()),116,83,0xCFCFCF,false);
        g.drawString(font,Component.translatable("gui.richstuff.foundry.fluids",menu.fluidCount(),menu.drains()),116,96,0xCFCFCF,false);
        g.drawString(font,inventoryTitle,inventoryLabelX,inventoryLabelY,0xD8D8D8,false);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){renderBackground(g,mouseX,mouseY,partial);super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);}
}
