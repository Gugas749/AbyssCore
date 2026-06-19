package com.gugas749.abysscore.Client.screens;

import com.gugas749.abysscore.Features.Regions.ACBlockProtectionListener;
import com.gugas749.abysscore.Network.region.OpenRegionScreenPacket;
import com.gugas749.abysscore.Network.region.SubmitRegionUpdatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class RegionManagerScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int LIST_W     = 140;
    private static final int PANEL_PAD  = 8;
    private static final int ROW_H      = 16;
    private final List<Integer> renderedTagYs = new ArrayList<>();

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<OpenRegionScreenPacket.RegionEntry> regions;
    private int selectedIndex = -1;

    // Right panel state — checkboxes for each tag
    private final List<String> ALL_TAGS = List.of(
        ACBlockProtectionListener.NO_BUILD_TAG,
        ACBlockProtectionListener.NO_INTERACT_TAG,
        ACBlockProtectionListener.NO_FLY_TAG,
        ACBlockProtectionListener.NO_FRIENDLYFIRE_TAG,
        ACBlockProtectionListener.NO_HUNGER_TAG,
        ACBlockProtectionListener.NO_TP_TAG,
        ACBlockProtectionListener.NO_MOBSPAWNING_HOSTILE_TAG,
        ACBlockProtectionListener.NO_MOBSPAWNING_PACIFIC_TAG,
        ACBlockProtectionListener.NO_ENTRY_TAG
    );
    private final Map<String, Boolean> tagState = new LinkedHashMap<>();

    // Buttons
    private Button saveButton;
    private Button deleteButton;
    private Button closeButton;

    // List scroll
    private int listScrollOffset = 0;

    private EditBox filterTagBox;

    public RegionManagerScreen(List<OpenRegionScreenPacket.RegionEntry> regions) {
        super(Component.translatable("screen.abysscore.region.title"));
        this.regions = new ArrayList<>(regions);
    }

    @Override
    protected void init() {
        int W = width, H = height;
        int detailX = LIST_W + PANEL_PAD * 2;
        int detailW = W - detailX - PANEL_PAD;
        int bottomY = H - 28;

        // Save button (right panel)
        saveButton = Button.builder(
            Component.translatable("screen.abysscore.region.save"),
            btn -> onSave()
        ).bounds(detailX + detailW - 100, bottomY, 98, 18).build();
        saveButton.active = false;
        addRenderableWidget(saveButton);

        // Delete button (right panel)
        deleteButton = Button.builder(
            Component.translatable("screen.abysscore.region.delete")
                .copy().withStyle(net.minecraft.ChatFormatting.RED),
            btn -> onDelete()
        ).bounds(detailX, bottomY, 80, 18).build();
        deleteButton.active = false;
        addRenderableWidget(deleteButton);

        // Close button
        closeButton = Button.builder(
            Component.translatable("gui.done"),
            btn -> onClose()
        ).bounds(W / 2 - 50, bottomY, 98, 18).build();
        addRenderableWidget(closeButton);

        // Reset tag state
        ALL_TAGS.forEach(tag -> tagState.put(tag, false));

        // filter tag - no entry
        int detailX2 = LIST_W + PANEL_PAD * 2;
        int detailW2 = width - detailX2 - PANEL_PAD;

        filterTagBox = new net.minecraft.client.gui.components.EditBox(
                font,
                detailX2 + PANEL_PAD + 14 + 2,  // aligned with checkbox labels
                0,   // Y set dynamically in renderDetailPanel
                detailW2 - PANEL_PAD - 14 - 4, 14,
                net.minecraft.network.chat.Component.literal("filter tag")
        );
        filterTagBox.setMaxLength(64);
        filterTagBox.setHint(net.minecraft.network.chat.Component.literal("Scoreboard tag (empty = block all)"));
        filterTagBox.active = false;
        filterTagBox.visible = false;
        addRenderableWidget(filterTagBox);
    }

    // ── Select region ─────────────────────────────────────────────────────────

    private void selectRegion(int index) {
        selectedIndex = index;
        if (index < 0 || index >= regions.size()) {
            saveButton.active = false;
            deleteButton.active = false;
            ALL_TAGS.forEach(tag -> tagState.put(tag, false));
            return;
        }

        if (filterTagBox != null) {
            String filter = index >= 0 ? regions.get(index).entryFilterTag() : "";
            filterTagBox.setValue(filter != null ? filter : "");
            filterTagBox.active = index >= 0;
        }

        OpenRegionScreenPacket.RegionEntry region = regions.get(index);
        ALL_TAGS.forEach(tag -> tagState.put(tag, region.tags().contains(tag)));
        saveButton.active = true;
        deleteButton.active = true;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void onSave() {
        if (selectedIndex < 0 || selectedIndex >= regions.size()) return;
        String name = regions.get(selectedIndex).name();

        Set<String> activeTags = new LinkedHashSet<>();
        ALL_TAGS.forEach(tag -> { if (tagState.getOrDefault(tag, false)) activeTags.add(tag); });

        String filterTag = filterTagBox != null ? filterTagBox.getValue().trim() : "";
        PacketDistributor.sendToServer(new SubmitRegionUpdatePacket(name, false, activeTags, filterTag));

        // Update local copy so the UI reflects the change immediately
        OpenRegionScreenPacket.RegionEntry old = regions.get(selectedIndex);
        regions.set(selectedIndex, new OpenRegionScreenPacket.RegionEntry(
                old.name(), old.dimension(),
                old.minX(), old.minY(), old.minZ(),
                old.maxX(), old.maxY(), old.maxZ(),
                new java.util.LinkedHashSet<>(activeTags),
                filterTag
        ));
    }

    private void onDelete() {
        if (selectedIndex < 0 || selectedIndex >= regions.size()) return;
        if (filterTagBox != null) filterTagBox.setValue("");
        String name = regions.get(selectedIndex).name();
        PacketDistributor.sendToServer(new SubmitRegionUpdatePacket(name, true, Set.of(), ""));
        regions.remove(selectedIndex);
        selectRegion(-1);
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // List panel clicks
        int listX = PANEL_PAD;
        int listY = PANEL_PAD + 14; // below header
        int visibleRows = (height - listY - 30) / ROW_H;

        if (mx >= listX && mx < listX + LIST_W && my >= listY) {
            int clicked = (int)(my - listY) / ROW_H + listScrollOffset;
            if (clicked >= 0 && clicked < regions.size()) {
                selectRegion(clicked);
                return true;
            }
        }

        // Tag checkbox clicks (right panel)
        if (selectedIndex >= 0) {
            int detailX = LIST_W + PANEL_PAD * 2;
            int tx = detailX + PANEL_PAD;

            for (int i = 0; i < ALL_TAGS.size() && i < renderedTagYs.size(); i++) {
                int rowY = renderedTagYs.get(i);

                if (mx >= tx && mx <= tx + 10
                        && my >= rowY + 3 && my <= rowY + 13) {

                    String tag = ALL_TAGS.get(i);
                    tagState.put(tag, !tagState.getOrDefault(tag, false));
                    return true;
                }
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        // Scroll the list panel
        if (mx < LIST_W + PANEL_PAD) {
            int maxScroll = Math.max(0, regions.size() - ((height - PANEL_PAD * 2 - 44) / ROW_H));
            listScrollOffset = Math.max(0, Math.min(listScrollOffset - (int) dy, maxScroll));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        renderBackground(g, mx, my, delta);
        renderListPanel(g, mx, my);
        renderDetailPanel(g);
        super.render(g, mx, my, delta);
    }

    private void renderListPanel(GuiGraphics g, int mx, int my) {
        int x = PANEL_PAD, y = PANEL_PAD;
        int h = height - PANEL_PAD * 2;

        // Panel background
        g.fill(x, y, x + LIST_W, y + h, 0xCC111111);
        g.renderOutline(x, y, LIST_W, h, 0xFF555555);

        // Header
        g.drawString(font, Component.translatable("screen.abysscore.region.list_header"),
            x + 4, y + 4, 0xFFFFFF);
        g.fill(x, y + 13, x + LIST_W, y + 14, 0xFF555555); // divider

        // Region rows
        int rowY = y + 16;
        int visibleH = h - 20 - 28;
        int visibleRows = visibleH / ROW_H;
        g.enableScissor(x, y + 14, x + LIST_W, y + 14 + visibleH);

        for (int i = listScrollOffset; i < regions.size() && i < listScrollOffset + visibleRows + 1; i++) {
            OpenRegionScreenPacket.RegionEntry region = regions.get(i);
            int rY = rowY + (i - listScrollOffset) * ROW_H;
            boolean hovered = mx >= x && mx < x + LIST_W && my >= rY && my < rY + ROW_H;
            boolean selected = i == selectedIndex;

            if (selected)      g.fill(x + 1, rY, x + LIST_W - 1, rY + ROW_H, 0xFF2255AA);
            else if (hovered)  g.fill(x + 1, rY, x + LIST_W - 1, rY + ROW_H, 0xFF333333);

            // Tag count badge
            int tagCount = region.tags().size();
            String badge = tagCount > 0 ? "[" + tagCount + "]" : "";
            g.drawString(font, region.name() + " " + badge, x + 5, rY + 4, 0xFFFFFF, false);
        }

        g.disableScissor();
    }

    private void renderDetailPanel(GuiGraphics g) {
        int detailX = LIST_W + PANEL_PAD * 2;
        int detailW = width - detailX - PANEL_PAD;
        int y = PANEL_PAD;
        int h = height - PANEL_PAD * 2;

        // Panel background
        g.fill(detailX, y, detailX + detailW, y + h, 0xCC111111);
        g.renderOutline(detailX, y, detailW, h, 0xFF555555);

        if (selectedIndex < 0 || selectedIndex >= regions.size()) {
            // Empty state
            String hint = "<- Select a region";
            g.drawCenteredString(font, hint, detailX + detailW / 2, y + h / 2, 0x888888);
            return;
        }

        OpenRegionScreenPacket.RegionEntry region = regions.get(selectedIndex);
        int tx = detailX + PANEL_PAD;
        int ty = y + 6;

        // Region name header
        g.drawString(font, region.name(), tx, ty, 0xFFFFFF);
        g.fill(detailX, ty + 10, detailX + detailW, ty + 11, 0xFF555555);
        ty += 14;

        // Dimension
        g.drawString(font, "Dim: " + shortenDim(region.dimension()), tx, ty, 0xFFFFFF, false);
        ty += 10;

        // Coords
        g.drawString(font,
            String.format("From: %d %d %d", region.minX(), region.minY(), region.minZ()),
            tx, ty, 0xFFFFFF, false);
        ty += 10;
        g.drawString(font,
            String.format("To:   %d %d %d", region.maxX(), region.maxY(), region.maxZ()),
            tx, ty, 0xFFFFFF, false);
        ty += 14;

        // Tags header
        g.drawString(font, "Restrictions:", tx, ty, 0xFFFFFF, false);
        ty += 12;

        // Tag checkboxes
        renderedTagYs.clear();
        for (String tag : ALL_TAGS) {
            renderedTagYs.add(ty);
            boolean checked = tagState.getOrDefault(tag, false);

            // Checkbox box
            g.fill(tx, ty + 3, tx + 10, ty + 13, 0xFF222222);
            g.renderOutline(tx, ty + 3, 10, 10, checked ? 0xFF55AA55 : 0xFF666666);

            // Checkmark
            if (checked) {
                g.fill(tx + 2, ty + 6, tx + 5, ty + 11, 0xFF55AA55);
                g.fill(tx + 4, ty + 4, tx + 9, ty + 9,  0xFF55AA55);
            }

            // Tag label — friendly name
            g.drawString(font, formatTagName(tag), tx + 14, ty + 4, 0xDDDDDD, false);
            ty += 18;
        }

        // no entry related
        boolean noEntryChecked = tagState.getOrDefault(ACBlockProtectionListener.NO_ENTRY_TAG, false);

        if (noEntryChecked && filterTagBox != null) {
            int ftx = tx + 14 + 2;
            int fty = ty + 2; // ty is the current y after the last checkbox

            g.drawString(font, "Entry filter tag:", tx + 14, fty - 10, 0xAAAAAA, false);

            filterTagBox.setX(ftx);
            filterTagBox.setY(fty);
            filterTagBox.visible = true;
            filterTagBox.active = true;
        } else if (filterTagBox != null) {
            filterTagBox.visible = false;
            filterTagBox.active = false;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String shortenDim(String dim) {
        // "minecraft:overworld" → "overworld"
        int colon = dim.indexOf(':');
        return colon >= 0 ? dim.substring(colon + 1) : dim;
    }

    private String formatTagName(String tag) {
        return switch (tag) {
            case "no_build"               -> "No Build";
            case "no_interact"            -> "No Interact";
            case "no_fly"                 -> "No Fly";
            case "no_friendlyfire"        -> "No Friendly Fire";
            case "no_hunger"              -> "No Hunger";
            case "no_tp"                  -> "No Teleport";
            case "no_mobspawning_hostile" -> "No Hostile Spawning";
            case "no_mobspawning_pacific" -> "No Passive Spawning";
            case "no_entry"               -> "No Entry";
            default -> tag;
        };
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
    }
}
