package com.gugas749.abysscore.Client.screens;

import com.gugas749.abysscore.Network.Bulk.SubmitBulkCommandPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
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
    private static final int ROW_HEIGHT    = 24;
    private static final int MAX_COMMANDS  = 10;

    private int panelX, panelY;

    // Widgets
    private EditBox nameBox;
    private CycleButton<Integer> permButton;
    private final List<EditBox> commandBoxes = new ArrayList<>();
    private Button addRowButton;
    private Button saveButton;
    private Button cancelButton;

    // Scroll offset for the command list
    private int scrollOffset = 0;
    // How many rows fit in the visible command area
    private static final int VISIBLE_ROWS = 5;
    public BulkCommandScreen() {
        super(Component.translatable("screen.abysscore.bulk.title"));
    }

    @Override
    protected void init() {
        panelX = (width  - PANEL_WIDTH)  / 2;
        panelY = (height - PANEL_HEIGHT) / 2;

        // Command Name
        nameBox = new EditBox(
                font, panelX + 10, panelY + 28, PANEL_WIDTH - 20, 18,
                Component.translatable("screen.abysscore.bulk.name_hint")
        );
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.translatable("screen.abysscore.bulk.name_hint"));
        addRenderableWidget(nameBox);

        // Permission Level
        permButton = CycleButton.<Integer>builder(level -> switch (level) {
                    case 0  -> Component.translatable("screen.abysscore.bulk.perm_everyone");
                    case 2  -> Component.translatable("screen.abysscore.bulk.perm_op");
                    default -> Component.literal("Level " + level);
                })
                .withValues(0, 2)
                .create(
                        panelX + 10, panelY + 62,
                        PANEL_WIDTH - 20, 18,
                        Component.translatable("screen.abysscore.bulk.perm_label")
                );
        addRenderableWidget(permButton);

        // Command rows
        // Start with one empty row
        if (commandBoxes.isEmpty()) {
            commandBoxes.add(makeCommandBox(0));
        }
        rebuildCommandWidgets();

        // Save / Cancel
        int btnY = panelY + PANEL_HEIGHT - 24;

        addRowButton = Button.builder(
                Component.literal("+ " + Component.translatable("screen.abysscore.bulk.add_row").getString()),
                btn -> onAddRow()
        ).bounds(panelX + 10, btnY, 80, 18).build();
        addRenderableWidget(addRowButton);

        saveButton = Button.builder(
                Component.translatable("screen.abysscore.bulk.save"),
                btn -> onSave()
        ).bounds(panelX + PANEL_WIDTH - 220, btnY, 100, 18).build();
        addRenderableWidget(saveButton);

        cancelButton = Button.builder(
                Component.translatable("gui.cancel"),
                btn -> onClose()
        ).bounds(panelX + PANEL_WIDTH - 110, btnY, 100, 18).build();
        addRenderableWidget(cancelButton);
    }

    // Builds / rebuilds the visible command EditBox rows

    private void rebuildCommandWidgets() {
        // Remove all existing command boxes from the widget list
        commandBoxes.forEach(this::removeWidget);

        int listStartY = panelY + 96;
        int visibleCount = Math.min(VISIBLE_ROWS, commandBoxes.size());
        int startIdx = Math.min(scrollOffset, Math.max(0, commandBoxes.size() - VISIBLE_ROWS));

        for (int i = 0; i < visibleCount; i++) {
            int idx = startIdx + i;
            if (idx >= commandBoxes.size()) break;

            EditBox box = commandBoxes.get(idx);
            int rowY = listStartY + (i * ROW_HEIGHT);

            // Reposition the box (leave room for remove button on the right)
            box.setX(panelX + 10);
            box.setY(rowY + 2);
            box.setWidth(PANEL_WIDTH - 40);

            addRenderableWidget(box);

            // "X" remove button per row
            final int removeIdx = idx;
            int xBtnFinalY = rowY + 2;
            Button removeBtn = Button.builder(
                    Component.literal("X"),
                    btn -> onRemoveRow(removeIdx)
            ).bounds(panelX + PANEL_WIDTH - 26, xBtnFinalY, 16, 16).build();
            addRenderableWidget(removeBtn);
        }
    }

    private EditBox makeCommandBox(int index) {
        EditBox box = new EditBox(
                font, panelX + 10, 0, PANEL_WIDTH - 40, 16,
                Component.literal("Command " + (index + 1))
        );
        box.setMaxLength(256);
        box.setHint(Component.translatable("screen.abysscore.bulk.command_hint"));
        return box;
    }

    // Add / Remove row

    private void onAddRow() {
        if (commandBoxes.size() >= MAX_COMMANDS) return;
        commandBoxes.add(makeCommandBox(commandBoxes.size()));

        if (commandBoxes.size() > VISIBLE_ROWS) {
            scrollOffset = commandBoxes.size() - VISIBLE_ROWS;
        }
        rebuildAll();
    }

    private void onRemoveRow(int idx) {
        if (commandBoxes.size() <= 1) return; // keep at least one row
        removeWidget(commandBoxes.remove(idx));
        if (scrollOffset > 0 && scrollOffset >= commandBoxes.size() - VISIBLE_ROWS + 1) {
            scrollOffset = Math.max(0, commandBoxes.size() - VISIBLE_ROWS);
        }
        rebuildAll();
    }

    // Scroll support

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listStartY = panelY + 96;
        int listEndY   = listStartY + (VISIBLE_ROWS * ROW_HEIGHT);

        if (mouseX >= panelX + 10 && mouseX <= panelX + PANEL_WIDTH - 10
                && mouseY >= listStartY && mouseY <= listEndY) {
            scrollOffset -= (int) Math.signum(scrollY);
            scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, commandBoxes.size() - VISIBLE_ROWS)));
            rebuildAll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // Full rebuild (clears all)

    private void rebuildAll() {
        // Save current values
        String savedName = nameBox.getValue();
        int savedPerm    = permButton.getValue();
        List<String> savedCmds = commandBoxes.stream()
                .map(EditBox::getValue)
                .collect(Collectors.toList());

        clearWidgets();
        commandBoxes.clear();
        init();

        // Restore values
        nameBox.setValue(savedName);
        // Cycle button to correct perm
        while (!permButton.getValue().equals(savedPerm)) {
            permButton.onPress();
        }
        // Restore command text
        for (int i = 0; i < savedCmds.size(); i++) {
            if (i < commandBoxes.size()) {
                commandBoxes.get(i).setValue(savedCmds.get(i));
            } else {
                EditBox box = makeCommandBox(i);
                box.setValue(savedCmds.get(i));
                commandBoxes.add(box);
            }
        }
        rebuildCommandWidgets();
    }

    // Save

    private void onSave() {
        String name = nameBox.getValue().trim();
        int perm    = permButton.getValue();

        if (name.isEmpty()) return;

        List<String> cmds = commandBoxes.stream()
                .map(box -> box.getValue().trim())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (cmds.isEmpty()) return;

        PacketDistributor.sendToServer(new SubmitBulkCommandPacket(name, perm, cmds));
        onClose();
    }

    // Render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x80000000);

        // Panel
        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xF21A1A1A);
        g.renderOutline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFFAAAAAA);

        // Title
        g.drawCenteredString(font, title, width / 2, panelY + 8, 0xFFFFFF);

        // Labels
        g.drawString(font, Component.translatable("screen.abysscore.bulk.name_label"),
                panelX + 10, panelY + 18, 0xAAAAAA);

        g.drawString(font, Component.translatable("screen.abysscore.bulk.commands_label"),
                panelX + 10, panelY + 86, 0xAAAAAA);

        // Scroll indicator
        if (commandBoxes.size() > VISIBLE_ROWS) {
            String scrollInfo = (scrollOffset + 1) + "-"
                    + Math.min(scrollOffset + VISIBLE_ROWS, commandBoxes.size())
                    + " / " + commandBoxes.size();
            g.drawString(font, scrollInfo,
                    panelX + PANEL_WIDTH - 10 - font.width(scrollInfo),
                    panelY + 86, 0x888888);
        }

        // Row count warning
        if (commandBoxes.size() >= MAX_COMMANDS) {
            g.drawString(font, Component.translatable("screen.abysscore.bulk.max_commands").getString(),
                    panelX + 100, panelY + 86, 0xFF5555);
        }

        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
