package com.gugas749.abysscore.Client;

import com.gugas749.abysscore.Network.SubmitBulkCommandPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GUI screen for creating a new bulk command.
 *
 * Fields:
 *   - Command name (alias)
 *   - Permission level (0 = Everyone, 2 = OP only)
 *   - Commands list (one per line, without the leading /)
 */
public class BulkCommandScreen extends Screen {

    // Layout constants
    private static final int PANEL_WIDTH  = 320;
    private static final int PANEL_HEIGHT = 240;

    private EditBox nameBox;
    private EditBox commandsBox;   // multiline simulated with a tall EditBox
    private CycleButton<Integer> permButton;
    private Button saveButton;
    private Button cancelButton;

    private int panelX, panelY;

    public BulkCommandScreen() {
        super(Component.translatable("screen.abysscore.bulk.title"));
    }

    @Override
    protected void init() {
        panelX = (width  - PANEL_WIDTH)  / 2;
        panelY = (height - PANEL_HEIGHT) / 2;

        // ── Command Name ────────────────────────────────────────────────────
        nameBox = new EditBox(
            font,
            panelX + 10, panelY + 30,
            PANEL_WIDTH - 20, 20,
            Component.translatable("screen.abysscore.bulk.name_hint")
        );
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.translatable("screen.abysscore.bulk.name_hint"));
        addRenderableWidget(nameBox);

        // ── Permission Level cycle button ───────────────────────────────────
        permButton = CycleButton.<Integer>builder(level -> switch (level) {
                case 0  -> Component.translatable("screen.abysscore.bulk.perm_everyone");
                case 2  -> Component.translatable("screen.abysscore.bulk.perm_op");
                default -> Component.literal("Level " + level);
            })
            .withValues(0, 2)
            .create(
                panelX + 10, panelY + 65,
                PANEL_WIDTH - 20, 20,
                Component.translatable("screen.abysscore.bulk.perm_label")
            );
        addRenderableWidget(permButton);

        // ── Commands box ────────────────────────────────────────────────────
        // Each command on a new line. EditBox doesn't support real multiline,
        // so we use a wide single-line box and instruct the user to separate with |
        commandsBox = new EditBox(
            font,
            panelX + 10, panelY + 100,
            PANEL_WIDTH - 20, 80,
            Component.translatable("screen.abysscore.bulk.commands_hint")
        );
        commandsBox.setMaxLength(2048);
        commandsBox.setHint(Component.translatable("screen.abysscore.bulk.commands_hint"));
        addRenderableWidget(commandsBox);

        // ── Buttons ─────────────────────────────────────────────────────────
        saveButton = Button.builder(
            Component.translatable("screen.abysscore.bulk.save"),
            btn -> onSave()
        ).bounds(panelX + 10, panelY + PANEL_HEIGHT - 30, 100, 20).build();
        addRenderableWidget(saveButton);

        cancelButton = Button.builder(
            Component.translatable("gui.cancel"),
            btn -> onClose()
        ).bounds(panelX + PANEL_WIDTH - 110, panelY + PANEL_HEIGHT - 30, 100, 20).build();
        addRenderableWidget(cancelButton);
    }

    private void onSave() {
        String name = nameBox.getValue().trim();
        String rawCmds = commandsBox.getValue().trim();
        int perm = permButton.getValue();

        if (name.isEmpty() || rawCmds.isEmpty()) return;

        // Commands are separated by | so we can fit them in one EditBox
        List<String> cmds = Arrays.stream(rawCmds.split("\\|"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());

        if (cmds.isEmpty()) return;

        // Send to server
        PacketDistributor.sendToServer(new SubmitBulkCommandPacket(name, perm, cmds));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Dark background overlay
        renderBackground(graphics, mouseX, mouseY, delta);

        // Panel background
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xCC222222);
        graphics.renderOutline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFFAAAAAA);

        // Title
        graphics.drawCenteredString(font, title, width / 2, panelY + 10, 0xFFFFFF);

        // Labels
        graphics.drawString(font,
            Component.translatable("screen.abysscore.bulk.name_label"),
            panelX + 10, panelY + 20, 0xAAAAAA);

        graphics.drawString(font,
            Component.translatable("screen.abysscore.bulk.commands_label"),
            panelX + 10, panelY + 90, 0xAAAAAA);

        graphics.drawString(font,
            Component.translatable("screen.abysscore.bulk.commands_info"),
            panelX + 10, panelY + 185, 0x888888);

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
