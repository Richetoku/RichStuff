package com.richetoku.richstuff.rikumimita.client;

import com.richetoku.richstuff.rikumimita.OutfitRegistry;
import com.richetoku.richstuff.rikumimita.RikumiMitaMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Owner-only native RichStuff companion inventory and settings screen. */
public final class RikumiMitaScreen extends AbstractContainerScreen<RikumiMitaMenu> {
    private Button modeButton;
    private Button homeButton;
    private Button confirmHomeButton;
    private Button cancelHomeButton;
    private boolean confirmingHomeReplacement;
    private static final int GUI_WIDTH = 344;
    private static final int GUI_HEIGHT = 282;

    private static final int LEFT_X = 8;
    private static final int LEFT_WIDTH = 110;
    private static final int RIGHT_X = 124;
    private static final int RIGHT_WIDTH = 212;

    private static final int PORTRAIT_X1 = 14;
    private static final int PORTRAIT_Y1 = 22;
    private static final int PORTRAIT_X2 = 112;
    private static final int PORTRAIT_Y2 = 116;

    private static final int OUTFIT_CONTROL_Y = 124;
    private static final int OUTFIT_ARROW_WIDTH = 24;
    private static final int ACTION_BUTTON_X = 14;
    private static final int ACTION_BUTTON_WIDTH = 98;

    public RikumiMitaScreen(RikumiMitaMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        // One compact outfit selector: previous icon, centered label, next icon.
        addRenderableWidget(Button.builder(Component.literal("◀"), button -> send(0))
                .bounds(leftPos + 14, topPos + OUTFIT_CONTROL_Y, OUTFIT_ARROW_WIDTH, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("▶"), button -> send(1))
                .bounds(leftPos + 88, topPos + OUTFIT_CONTROL_Y, OUTFIT_ARROW_WIDTH, 20)
                .build());

        homeButton = addRenderableWidget(Button.builder(Component.translatable("gui.richstuff.rikumi.set_home"), button -> requestSetHome())
                .bounds(leftPos + ACTION_BUTTON_X, topPos + 184, ACTION_BUTTON_WIDTH, 18)
                .build());
        modeButton = addRenderableWidget(Button.builder(Component.literal(menu.rikumi().getMode().displayName()), button -> {
                    send(2);
                    button.setMessage(Component.literal(menu.rikumi().getMode().next().displayName()));
                })
                .bounds(leftPos + ACTION_BUTTON_X, topPos + 206, ACTION_BUTTON_WIDTH, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.richstuff.rikumi.voice"), button -> send(3))
                .bounds(leftPos + ACTION_BUTTON_X, topPos + 228, ACTION_BUTTON_WIDTH, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.richstuff.rikumi.nameplate"), button -> send(4))
                .bounds(leftPos + ACTION_BUTTON_X, topPos + 250, ACTION_BUTTON_WIDTH, 18)
                .build());
        confirmHomeButton = addRenderableWidget(Button.builder(Component.translatable("gui.yes"), button -> {
                    send(5); setHomeConfirmation(false);
                }).bounds(leftPos + 118, topPos + 137, 50, 20).build());
        cancelHomeButton = addRenderableWidget(Button.builder(Component.translatable("gui.no"), button -> setHomeConfirmation(false))
                .bounds(leftPos + 176, topPos + 137, 50, 20).build());
        setHomeConfirmation(false);
    }

    private void requestSetHome() {
        if (!menu.rikumi().hasHome()) send(5);
        else setHomeConfirmation(true);
    }

    private void setHomeConfirmation(boolean value) {
        confirmingHomeReplacement = value;
        if (confirmHomeButton != null) confirmHomeButton.visible = confirmHomeButton.active = value;
        if (cancelHomeButton != null) cancelHomeButton.visible = cancelHomeButton.active = value;
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Draw a single custom window instead of blitting the vanilla inventory
        // texture. The old mixed background caused clipped labels and stray slots.
        panel(graphics, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight);
        insetPanel(graphics,
                leftPos + PORTRAIT_X1,
                topPos + PORTRAIT_Y1,
                leftPos + PORTRAIT_X2,
                topPos + PORTRAIT_Y2);
        insetPanel(graphics,
                leftPos + RIGHT_X,
                topPos + 20,
                leftPos + RIGHT_X + RIGHT_WIDTH,
                topPos + 92);
        insetPanel(graphics,
                leftPos + RIGHT_X,
                topPos + 104,
                leftPos + RIGHT_X + RIGHT_WIDTH,
                topPos + 194);
        insetPanel(graphics,
                leftPos + RIGHT_X,
                topPos + 196,
                leftPos + RIGHT_X + RIGHT_WIDTH,
                topPos + 250);

        drawSlotGrid(graphics,
                leftPos + RikumiMitaMenu.COMPANION_X,
                topPos + RikumiMitaMenu.COMPANION_Y,
                9,
                3);
        drawSlotGrid(graphics, leftPos + RikumiMitaMenu.EQUIPMENT_X,
                topPos + RikumiMitaMenu.MAIN_HAND_Y, 1, 1);
        drawSlotGrid(graphics, leftPos + RikumiMitaMenu.EQUIPMENT_X,
                topPos + RikumiMitaMenu.OFF_HAND_Y, 1, 1);
        drawSlotGrid(graphics,
                leftPos + RikumiMitaMenu.PLAYER_X,
                topPos + RikumiMitaMenu.PLAYER_Y,
                9,
                3);
        drawSlotGrid(graphics,
                leftPos + RikumiMitaMenu.PLAYER_X,
                topPos + RikumiMitaMenu.HOTBAR_Y,
                9,
                1);

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                leftPos + 22,
                topPos + 28,
                leftPos + 104,
                topPos + 112,
                38,
                0.0625F,
                mouseX,
                mouseY,
                menu.rikumi());
        if (confirmingHomeReplacement) {
            graphics.fill(leftPos + 104, topPos + 103, leftPos + 240, topPos + 164, 0xEE111111);
            graphics.drawCenteredString(font, Component.translatable("gui.richstuff.rikumi.replace_home_title"),
                    leftPos + 172, topPos + 111, 0xFFFFFFFF);
            graphics.drawCenteredString(font, Component.translatable("gui.richstuff.rikumi.replace_home_warning"),
                    leftPos + 172, topPos + 123, 0xFFFFC266);
        }
    }

    private static void panel(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        graphics.fill(x1, y1, x2, y2, 0xFFC6C6C6);
        graphics.fill(x1, y1, x2, y1 + 2, 0xFFFFFFFF);
        graphics.fill(x1, y1, x1 + 2, y2, 0xFFFFFFFF);
        graphics.fill(x1, y2 - 2, x2, y2, 0xFF555555);
        graphics.fill(x2 - 2, y1, x2, y2, 0xFF555555);
    }

    private static void insetPanel(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        graphics.fill(x1, y1, x2, y2, 0xFF8B8B8B);
        graphics.fill(x1, y1, x2, y1 + 2, 0xFF555555);
        graphics.fill(x1, y1, x1 + 2, y2, 0xFF555555);
        graphics.fill(x1 + 2, y1 + 2, x2 - 2, y2 - 2, 0xFF171717);
        graphics.fill(x1 + 2, y2 - 2, x2, y2, 0xFFFFFFFF);
        graphics.fill(x2 - 2, y1 + 2, x2, y2, 0xFFFFFFFF);
    }

    private static void drawSlotGrid(GuiGraphics graphics, int x, int y, int columns, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int sx = x + col * 18;
                int sy = y + row * 18;
                graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
                graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("gui.richstuff.rikumi.title"), 14, 8, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.richstuff.rikumi.companion_inventory"), RIGHT_X + 40, 8, 0x404040, false);
        graphics.drawString(font, Component.translatable("container.inventory"), RIGHT_X + 40, 96, 0x404040, false);

        graphics.drawCenteredString(font, Component.translatable("gui.richstuff.rikumi.outfit"), 63, OUTFIT_CONTROL_Y + 6, 0x404040);

        String outfitName = OutfitRegistry.byIndex(menu.rikumi().getOutfitIndex()).label();
        String visibleName = font.plainSubstrByWidth(outfitName, LEFT_WIDTH - 12);
        graphics.drawCenteredString(font, Component.literal(visibleName), 63, 149, 0x404040);
        graphics.drawString(font, Component.translatable("gui.richstuff.rikumi.current_task"), 14, 160, 0x404040, false);
        graphics.drawString(font, Component.literal(font.plainSubstrByWidth(menu.rikumi().getCurrentTask(), 96)),
                14, 170, 0x303030, false);
        graphics.drawString(font, Component.translatable("gui.richstuff.rikumi.current_goal"), RIGHT_X + 4, 199, 0x404040, false);
        graphics.drawString(font, Component.literal(font.plainSubstrByWidth(menu.rikumi().getCurrentGoal(), 204)),
                RIGHT_X + 4, 210, 0x303030, false);
        graphics.drawString(font, Component.translatable("gui.richstuff.rikumi.current_project"), RIGHT_X + 4, 222, 0x404040, false);
        graphics.drawString(font, Component.literal(font.plainSubstrByWidth(menu.rikumi().getCurrentProjectName(), 204)),
                RIGHT_X + 4, 233, 0x303030, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (modeButton != null) modeButton.setMessage(Component.literal(menu.rikumi().getMode().displayName()));
        if (homeButton != null) homeButton.setMessage(Component.translatable(menu.rikumi().hasHome()
                ? "gui.richstuff.rikumi.replace_home" : "gui.richstuff.rikumi.set_home"));
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        if (localX >= 14 && localX < 112 && localY >= 168 && localY < 181) {
            graphics.renderTooltip(font, Component.literal(menu.rikumi().getCurrentTaskDetail()), mouseX, mouseY);
        } else if (localX >= RIGHT_X + 4 && localX < RIGHT_X + RIGHT_WIDTH - 4
                && localY >= 208 && localY < 221) {
            graphics.renderTooltip(font, Component.literal(menu.rikumi().getCurrentGoalDetail()), mouseX, mouseY);
        } else if (localX >= RIGHT_X + 4 && localX < RIGHT_X + RIGHT_WIDTH - 4
                && localY >= 231 && localY < 245) {
            graphics.renderTooltip(font, Component.literal(menu.rikumi().getCurrentProjectDetail()), mouseX, mouseY);
        }
    }
}
