package com.gugas749.abysscore.Client.screens;

import com.gugas749.abysscore.Network.Dimen.SubmitDimenCreatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Random;

public class DimenCreateScreen extends Screen {

    private static final int W = 300;
    private static final int H = 200;
    private int px, py;

    private EditBox nameBox;
    private EditBox displayNameBox;
    private EditBox seedBox;
    private CycleButton<String> styleButton;
    private Button randomSeedButton;
    private Button createButton;
    private Button cancelButton;

    public DimenCreateScreen() {
        super(Component.translatable("screen.abysscore.dimen.create.title"));
    }

    @Override
    protected void init() {
        px = (width  - W) / 2;
        py = (height - H) / 2;

        int x  = px + 10;
        int fw = W - 20;
        int hw = (fw - 4) / 2;

        // ── Internal name ─────────────────────────────────────────────────────
        nameBox = new EditBox(font, x, py + 28, fw, 18,
                Component.translatable("screen.abysscore.dimen.create.name_hint"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.translatable("screen.abysscore.dimen.create.name_hint"));
        addRenderableWidget(nameBox);

        // ── Display name ──────────────────────────────────────────────────────
        displayNameBox = new EditBox(font, x, py + 62, fw, 18,
                Component.translatable("screen.abysscore.dimen.create.display_hint"));
        displayNameBox.setMaxLength(64);
        displayNameBox.setHint(Component.translatable("screen.abysscore.dimen.create.display_hint"));
        addRenderableWidget(displayNameBox);

        // ── Style ─────────────────────────────────────────────────────────────
        styleButton = CycleButton.<String>builder(s -> Component.literal(
                        s.charAt(0) + s.substring(1).toLowerCase()))
                .withValues("NORMAL", "SUPERFLAT", "VOID")
                .create(x, py + 96, fw, 18,
                        Component.translatable("screen.abysscore.dimen.create.style"));
        addRenderableWidget(styleButton);

        // ── Seed (half-width) + Random button ─────────────────────────────────
        seedBox = new EditBox(font, x, py + 130, hw, 18,
                Component.literal("0"));
        seedBox.setMaxLength(20);
        seedBox.setHint(Component.literal("0 = random"));
        seedBox.setValue("0");
        addRenderableWidget(seedBox);

        randomSeedButton = Button.builder(
                Component.translatable("screen.abysscore.dimen.create.random_seed"),
                btn -> seedBox.setValue(String.valueOf(new Random().nextLong()))
        ).bounds(x + hw + 4, py + 130, hw, 18).build();
        addRenderableWidget(randomSeedButton);

        // ── Create / Cancel ───────────────────────────────────────────────────
        createButton = Button.builder(
                Component.translatable("screen.abysscore.dimen.create.create"),
                btn -> onCreate()
        ).bounds(x, py + H - 24, 100, 18).build();
        addRenderableWidget(createButton);

        cancelButton = Button.builder(
                Component.translatable("gui.cancel"),
                btn -> onClose()
        ).bounds(px + W - 110, py + H - 24, 100, 18).build();
        addRenderableWidget(cancelButton);
    }

    @Override
    public void tick() {
        // Seed row is only relevant for NORMAL style
        boolean isNormal = "NORMAL".equals(styleButton.getValue());
        seedBox.setVisible(isNormal);
        randomSeedButton.visible = isNormal;
    }

    private void onCreate() {
        String rawName = nameBox.getValue().trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
        if (rawName.isEmpty()) return;

        long seed = 0;
        try { seed = Long.parseLong(seedBox.getValue().trim()); }
        catch (NumberFormatException ignored) {}

        PacketDistributor.sendToServer(new SubmitDimenCreatePacket(
                rawName,
                displayNameBox.getValue().trim(),
                styleButton.getValue(),
                seed
        ));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        renderBackground(g, mx, my, delta);
        g.fill(px, py, px + W, py + H, 0xCC1A1A1A);
        g.renderOutline(px, py, W, H, 0xFFAAAAAA);
        g.drawCenteredString(font, title, width / 2, py + 8, 0xFFFFFF);

        g.drawString(font,
                Component.translatable("screen.abysscore.dimen.create.name_label"),
                px + 10, py + 18, 0xAAAAAA);
        g.drawString(font,
                Component.translatable("screen.abysscore.dimen.create.display_label"),
                px + 10, py + 52, 0xAAAAAA);
        g.drawString(font,
                Component.translatable("screen.abysscore.dimen.create.style"),
                px + 10, py + 86, 0xAAAAAA);

        // Only show seed label when NORMAL is selected
        if ("NORMAL".equals(styleButton.getValue())) {
            g.drawString(font,
                    Component.translatable("screen.abysscore.dimen.create.seed_label"),
                    px + 10, py + 120, 0xAAAAAA);
        }

        super.render(g, mx, my, delta);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
