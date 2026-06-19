package com.gugas749.abysscore.Client.screens;

import com.gugas749.abysscore.Network.menu.MenuActionPacket;
import com.gugas749.abysscore.Network.menu.OpenMainMenuPacket;
import com.gugas749.abysscore.Network.region.RequestRegionScreenPacket;
import com.gugas749.abysscore.Network.title.SaveTitlePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class AbyssCoreMenuScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int CAT_W = 110;
    private static final int PAD   = 8;
    private static final int ROW_H = 18;
    private static final int BTN_S = 36;

    // ── Data (mutable local copies for instant UI feedback) ───────────────────
    private final List<OpenMainMenuPacket.PlayerState> players;
    private final List<OpenMainMenuPacket.TitleEntry>  titles;
    private final List<OpenMainMenuPacket.DimEntry>    dims;
    private final List<OpenMainMenuPacket.HelpEntry>   helpRequests;
    private final List<OpenMainMenuPacket.BulkEntry>   bulkCommands;
    private final Map<Integer, String>                 bindSlots;
    private boolean teamVisibility;

    // ── Navigation ────────────────────────────────────────────────────────────
    private int selectedCategory = 0;
    private static final String[] CATS = {
            "Players", "Titles", "Vanish", "Regions", "Dimensions", "Help Requests", "Bulk Commands"
    };

    // ── Scroll offsets ────────────────────────────────────────────────────────
    private int playerScroll = 0, titleScroll = 0, dimScroll = 0;
    private int helpScroll = 0, bulkScroll = 0;

    // ── Title editor state ────────────────────────────────────────────────────
    private int editingTitle = -1; // -1=none, -2=new
    private int runningTitle = -1;
    private EditBox titleNameBox, titleTextBox, titleSubBox;
    private EditBox titleFadeInBox, titleStayBox, titleFadeOutBox;
    private EditBox runTagBox, runTeamBox;

    public AbyssCoreMenuScreen(OpenMainMenuPacket pkt) {
        super(Component.literal("AbyssCore"));
        this.players       = new ArrayList<>(pkt.players());
        this.titles        = new ArrayList<>(pkt.titles());
        this.dims          = new ArrayList<>(pkt.dims());
        this.helpRequests  = new ArrayList<>(pkt.helpRequests());
        this.bulkCommands  = new ArrayList<>(pkt.bulkCommands());
        this.bindSlots     = new LinkedHashMap<>(pkt.bindSlots());
        this.teamVisibility = pkt.teamVisibility();
    }

    @Override
    protected void init() {
        clearWidgets();
        initCategoryButtons();
        switch (selectedCategory) {
            case 1 -> initTitlesWidgets();
        }
        // All other panels are pure render + mouseClicked — no persistent widgets needed
    }

    // ── Category sidebar ──────────────────────────────────────────────────────

    private void initCategoryButtons() {
        int y = PAD + 20;
        for (int i = 0; i < CATS.length; i++) {
            final int idx = i;
            var btn = Button.builder(Component.literal(CATS[i]), b -> {
                selectedCategory = idx;
                editingTitle = runningTitle = -1;
                init();
            }).bounds(PAD + 2, y + i * (ROW_H + 2), CAT_W - 4, ROW_H).build();
            btn.active = (i != selectedCategory);
            addRenderableWidget(btn);
        }
        addRenderableWidget(Button.builder(
                Component.literal("X").withStyle(ChatFormatting.RED),
                b -> onClose()
        ).bounds(width - PAD - 28, PAD + 4, 18, 14).build());
    }

    // ── Titles widgets ────────────────────────────────────────────────────────

    private void initTitlesWidgets() {
        int px = panelX(), pw = panelW();
        int x = px + PAD, w = pw - PAD * 2;

        if (editingTitle == -1 && runningTitle == -1) {
            addRenderableWidget(Button.builder(Component.literal("+ New Title"),
                    b -> { editingTitle = -2; init(); }
            ).bounds(px + pw - PAD - 90, PAD + 4, 90, 14).build());
            return;
        }

        if (editingTitle != -1) {
            int y = PAD + 22;
            titleNameBox    = addEditBox(x, y, w, "Name"); y += 20;
            titleTextBox    = addEditBox(x, y, w, "Title text (&a green, &c red...)"); y += 20;
            titleSubBox     = addEditBox(x, y, w, "Subtitle (optional)"); y += 20;
            int third = (w - 8) / 3;
            titleFadeInBox  = addEditBox(x,               y, third, "Fade in (ticks)");
            titleStayBox    = addEditBox(x + third + 4,   y, third, "Stay (ticks)");
            titleFadeOutBox = addEditBox(x + third*2 + 8, y, third, "Fade out (ticks)");
            y += 20;

            if (editingTitle >= 0 && editingTitle < titles.size()) {
                var t = titles.get(editingTitle);
                titleNameBox.setValue(t.name()); titleTextBox.setValue(t.titleText());
                titleSubBox.setValue(t.subtitleText());
                titleFadeInBox.setValue(String.valueOf(t.fadeIn()));
                titleStayBox.setValue(String.valueOf(t.stay()));
                titleFadeOutBox.setValue(String.valueOf(t.fadeOut()));
            } else {
                titleFadeInBox.setValue("10"); titleStayBox.setValue("70"); titleFadeOutBox.setValue("20");
            }

            final int fy = y + 4;
            addRenderableWidget(Button.builder(Component.literal("Save"),
                    b -> onSaveTitle()).bounds(x, fy, 55, 16).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"),
                    b -> { editingTitle = -1; init(); }).bounds(x + 59, fy, 55, 16).build());
        }

        if (runningTitle != -1) {
            int y = PAD + 40;
            addRenderableWidget(Button.builder(Component.literal("Send to ALL players"), b -> {
                send(MenuActionPacket.Action.SEND_TITLE_ALL, titles.get(runningTitle).id(), "");
                runningTitle = -1; init();
            }).bounds(x, y, pw - PAD * 2, 16).build()); y += 22;

            runTagBox = addEditBox(x, y, pw - PAD * 2 - 80, "Scoreboard tag...");
            addRenderableWidget(Button.builder(Component.literal("By Tag"), b -> {
                send(MenuActionPacket.Action.SEND_TITLE_TAG,
                        titles.get(runningTitle).id(), runTagBox.getValue().trim());
                runningTitle = -1; init();
            }).bounds(x + pw - PAD * 2 - 76, y, 76, 16).build()); y += 22;

            runTeamBox = addEditBox(x, y, pw - PAD * 2 - 80, "Team name...");
            addRenderableWidget(Button.builder(Component.literal("By Team"), b -> {
                send(MenuActionPacket.Action.SEND_TITLE_TEAM,
                        titles.get(runningTitle).id(), runTeamBox.getValue().trim());
                runningTitle = -1; init();
            }).bounds(x + pw - PAD * 2 - 76, y, 76, 16).build()); y += 22;

            addRenderableWidget(Button.builder(Component.literal("Cancel"),
                    b -> { runningTitle = -1; init(); }).bounds(x, y, 60, 16).build());
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        renderBackground(g, mx, my, delta);
        renderSidebar(g);
        renderPanel(g, mx, my);
        super.render(g, mx, my, delta);
    }

    private void renderSidebar(GuiGraphics g) {
        g.fill(PAD, PAD, PAD + CAT_W, height - PAD, 0xCC111111);
        g.renderOutline(PAD, PAD, CAT_W, height - PAD * 2, 0xFF555555);
        g.drawString(font, "\u00a7eAbyssCore", PAD + 4, PAD + 4, 0xFFFFFF);
        g.fill(PAD, PAD + 14, PAD + CAT_W, PAD + 15, 0xFF555555);
    }

    private void renderPanel(GuiGraphics g, int mx, int my) {
        int px = panelX(), pw = panelW();
        g.fill(px, PAD, px + pw, height - PAD, 0xCC111111);
        g.renderOutline(px, PAD, pw, height - PAD * 2, 0xFF555555);

        switch (selectedCategory) {
            case 0 -> renderPlayers(g, px, pw, mx, my);
            case 1 -> renderTitles(g, px, pw, mx, my);
            case 2 -> renderVanish(g, px, pw, mx, my);
            case 3 -> renderRegions(g, px, pw);
            case 4 -> renderDims(g, px, pw, mx, my);
            case 5 -> renderHelp(g, px, pw, mx, my);
            case 6 -> renderBulk(g, px, pw, mx, my);
        }
    }

    // ── Panel: Players ────────────────────────────────────────────────────────

    private void renderPlayers(GuiGraphics g, int px, int pw, int mx, int my) {
        int x = px + PAD, y = header(g, px, "Players");
        g.drawString(font, "\u00a77Name",   x,       y, 0xFFFFFF, false);
        g.drawString(font, "\u00a77Staff",  x + 130, y, 0xFFFFFF, false);
        g.drawString(font, "\u00a77Vanish", x + 170, y, 0xFFFFFF, false);
        g.drawString(font, "\u00a77God",    x + 212, y, 0xFFFFFF, false);
        g.drawString(font, "\u00a77Blind",  x + 246, y, 0xFFFFFF, false);
        y += 12;

        scissor(g, px, y, pw);
        for (int i = playerScroll; i < players.size(); i++) {
            var p = players.get(i); int ry = y + (i - playerScroll) * ROW_H;
            if (ry > height - PAD - 10) break;
            if (isHovered(mx, my, px + 1, ry, pw - 2, ROW_H)) g.fill(px+1, ry, px+pw-1, ry+ROW_H, 0xFF1A1A2E);
            g.drawString(font, p.name(), x, ry + 4, 0xFFFFFF, false);
            toggle(g, x+130, ry, p.staffMode());
            toggle(g, x+170, ry, p.vanished());
            toggle(g, x+212, ry, p.godMode());
            toggle(g, x+246, ry, p.blinded());
        }
        g.disableScissor();
    }

    // ── Panel: Titles ─────────────────────────────────────────────────────────

    private void renderTitles(GuiGraphics g, int px, int pw, int mx, int my) {
        int x = px + PAD, y = header(g, px, "Titles");
        if (editingTitle != -1) {
            g.drawString(font, editingTitle == -2 ? "\u00a7eNew Title" : "\u00a7eEdit Title", x, y, 0xFFFFFF);
            return;
        }
        if (runningTitle != -1) {
            g.drawString(font, "\u00a7eSend: \u00a7f" + (runningTitle < titles.size() ? titles.get(runningTitle).name() : ""), x, y, 0xFFFFFF);
            return;
        }
        y += 22;
        if (titles.isEmpty()) {
            g.drawCenteredString(font, "No titles. Click + New Title.", (px+px+pw)/2, height/2, 0x888888);
            return;
        }
        scissor(g, px, y, pw);
        for (int i = titleScroll; i < titles.size(); i++) {
            var t = titles.get(i); int ry = y + (i - titleScroll) * ROW_H;
            if (ry > height - PAD - 10) break;
            if (isHovered(mx, my, px+1, ry, pw-2, ROW_H)) g.fill(px+1, ry, px+pw-1, ry+ROW_H, 0xFF1A1A2E);
            g.drawString(font, t.name(), x, ry+4, 0xFFFFFF, false);
            g.drawString(font, "\u00a78" + shorten(t.titleText(), 28), x+100, ry+4, 0xFFFFFF, false);
            inlineBtn(g, pw, ry, 0, BTN_S, "\u00a7aRun", 0xFF1A3A1A);
            inlineBtn(g, pw, ry, BTN_S + 4, BTN_S, "\u00a79Edit", 0xFF1A1A3A);
        }
        g.disableScissor();
    }

    // ── Panel: Vanish ─────────────────────────────────────────────────────────

    private void renderVanish(GuiGraphics g, int px, int pw, int mx, int my) {
        int x = px + PAD, y = header(g, px, "Vanish");
        // Team visibility toggle button (rendered as inline label + box)
        String tvLabel = "Team Visibility: " + (teamVisibility ? "\u00a7aON" : "\u00a7cOFF");
        g.drawString(font, tvLabel, x, y, 0xFFFFFF, false);
        // Box to click
        int bx = px + pw - PAD - 60;
        g.fill(bx, y - 2, bx + 60, y + 10, teamVisibility ? 0xFF1A3A1A : 0xFF3A1A1A);
        g.drawCenteredString(font, teamVisibility ? "\u00a7aEnabled" : "\u00a7cDisabled", bx + 30, y, 0xFFFFFF);
        y += 16;
        g.fill(px, y, px + pw, y + 1, 0xFF555555); y += 4;

        g.drawString(font, "\u00a77Vanished players & their exceptions:", x, y, 0xFFFFFF, false); y += 12;

        scissor(g, px, y, pw);
        List<OpenMainMenuPacket.PlayerState> vanished = players.stream().filter(OpenMainMenuPacket.PlayerState::vanished).toList();
        if (vanished.isEmpty()) {
            g.drawCenteredString(font, "No players are vanished.", (px+px+pw)/2, y + 20, 0x888888);
        }
        for (int i = 0; i < vanished.size(); i++) {
            var p = vanished.get(i); int ry = y + i * ROW_H;
            if (ry > height - PAD - 10) break;
            if (isHovered(mx, my, px+1, ry, pw-2, ROW_H)) g.fill(px+1, ry, px+pw-1, ry+ROW_H, 0xFF1A1A2E);
            g.drawString(font, p.name(), x, ry+4, 0xFFFFFF, false);
            inlineBtn(g, pw, ry, 0, 44, "\u00a7aShow", 0xFF1A3A1A);
            inlineBtn(g, pw, ry, 48, 44, "\u00a7cHide", 0xFF3A1A1A);
            inlineBtn(g, pw, ry, 96, 44, "\u00a77Clear", 0xFF2A2A2A);
        }
        g.disableScissor();
    }

    // ── Panel: Regions ────────────────────────────────────────────────────────

    private void renderRegions(GuiGraphics g, int px, int pw) {
        int x = px + PAD, y = header(g, px, "Regions");
        g.drawString(font, "Open the Region Manager to edit regions, tags and restrictions.", x, y, 0xAAAAAA, false);
        y += 20;
        // Button is rendered via super.render from init — but since we don't add it in init for this panel,
        // we draw it manually and handle in mouseClicked
        g.fill(x, y, x + 160, y + 20, 0xFF225522);
        g.renderOutline(x, y, 160, 20, 0xFF55AA55);
        g.drawCenteredString(font, "\u00a7aOpen Region Manager", x + 80, y + 6, 0xFFFFFF);
    }

    // ── Panel: Dimensions ─────────────────────────────────────────────────────

    private void renderDims(GuiGraphics g, int px, int pw, int mx, int my) {
        int x = px + PAD, y = header(g, px, "Dimensions");
        // Create button
        g.fill(px + PAD, PAD + 4, px + PAD + 100, PAD + 18, 0xFF225522);
        g.drawCenteredString(font, "\u00a7a+ Create Dim", px + PAD + 50, PAD + 8, 0xFFFFFF);
        y += 2;

        scissor(g, px, y, pw);
        for (int i = dimScroll; i < dims.size(); i++) {
            var d = dims.get(i); int ry = y + (i - dimScroll) * ROW_H;
            if (ry > height - PAD - 10) break;
            if (isHovered(mx, my, px+1, ry, pw-2, ROW_H)) g.fill(px+1, ry, px+pw-1, ry+ROW_H, 0xFF1A1A2E);
            String stateColor = switch (d.state()) {
                case "LOADED" -> "\u00a7a";
                case "UNLOADED" -> "\u00a7c";
                default -> "\u00a7e";
            };
            g.drawString(font, d.name(), x, ry+4, 0xFFFFFF, false);
            g.drawString(font, stateColor + d.state().toLowerCase(), x + 160, ry+4, 0xFFFFFF, false);
            inlineBtn(g, pw, ry, 0, BTN_S, "\u00a7bTP", 0xFF1A2A3A);
        }
        g.disableScissor();
    }

    // ── Panel: Help Requests ──────────────────────────────────────────────────

    private void renderHelp(GuiGraphics g, int px, int pw, int mx, int my) {
        int x = px + PAD, y = header(g, px, "Help Requests");
        if (helpRequests.isEmpty()) {
            g.drawCenteredString(font, "No active help requests.", (px+px+pw)/2, height/2, 0x888888);
            return;
        }
        scissor(g, px, y, pw);
        for (int i = helpScroll; i < helpRequests.size(); i++) {
            var h = helpRequests.get(i); int ry = y + (i - helpScroll) * ROW_H;
            if (ry > height - PAD - 10) break;
            if (isHovered(mx, my, px+1, ry, pw-2, ROW_H)) g.fill(px+1, ry, px+pw-1, ry+ROW_H, 0xFF1A1A2E);
            g.drawString(font, "\u00a7e" + h.playerName() + "\u00a77: \u00a7f" + shorten(h.reason(), 40), x, ry+4, 0xFFFFFF, false);
            inlineBtn(g, pw, ry, 0, BTN_S, "\u00a7a[TP]", 0xFF1A3A1A);
        }
        g.disableScissor();
    }

    // ── Panel: Bulk Commands ──────────────────────────────────────────────────

    private void renderBulk(GuiGraphics g, int px, int pw, int mx, int my) {
        int x = px + PAD, y = header(g, px, "Bulk Commands");
        if (bulkCommands.isEmpty()) {
            g.drawCenteredString(font, "No bulk commands defined.", (px+px+pw)/2, height/2, 0x888888);
            return;
        }

        g.drawString(font, "\u00a77Name", x, y, 0xFFFFFF, false);
        g.drawString(font, "\u00a77Bound Slot", x + 150, y, 0xFFFFFF, false);
        y += 12;

        scissor(g, px, y, pw);
        for (int i = bulkScroll; i < bulkCommands.size(); i++) {
            var b = bulkCommands.get(i); int ry = y + (i - bulkScroll) * ROW_H;
            if (ry > height - PAD - 10) break;
            if (isHovered(mx, my, px+1, ry, pw-2, ROW_H)) g.fill(px+1, ry, px+pw-1, ry+ROW_H, 0xFF1A1A2E);
            g.drawString(font, b.name(), x, ry+4, 0xFFFFFF, false);

            // Find if this bulk is bound to any slot
            String boundSlot = bindSlots.entrySet().stream()
                    .filter(e -> e.getValue().equals(b.name()))
                    .map(e -> "Slot " + e.getKey())
                    .findFirst().orElse("\u00a78none");
            g.drawString(font, boundSlot, x + 150, ry+4, 0xFFFFFF, false);

            inlineBtn(g, pw, ry, 0, BTN_S, "\u00a7aRun", 0xFF1A3A1A);
            // Bind slot mini-buttons 1-9
            for (int slot = 1; slot <= 9; slot++) {
                int bx = pw - PAD - (10 - slot) * 14;
                boolean isBound = b.name().equals(bindSlots.get(slot));
                g.fill(px + bx, ry+2, px + bx + 12, ry+ROW_H-2, isBound ? 0xFF334433 : 0xFF222222);
                g.drawCenteredString(font, isBound ? "\u00a7a"+slot : "\u00a78"+slot, px+bx+6, ry+5, 0xFFFFFF);
            }
        }
        g.disableScissor();
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int px = panelX(), pw = panelW();

        switch (selectedCategory) {
            case 0 -> { // Players
                int y = PAD + 6 + 12 + 4 + 12;
                for (int i = playerScroll; i < players.size(); i++) {
                    int ry = y + (i - playerScroll) * ROW_H;
                    if (my < ry || my >= ry + ROW_H) continue;
                    var p = players.get(i);
                    int x = panelX() + PAD;
                    if (clickCol(mx, x+120, x+165)) { togglePlayer(i, 0); return true; }
                    if (clickCol(mx, x+160, x+205)) { togglePlayer(i, 1); return true; }
                    if (clickCol(mx, x+202, x+240)) { togglePlayer(i, 2); return true; }
                    if (clickCol(mx, x+236, x+280)) { togglePlayer(i, 3); return true; }
                }
            }
            case 1 -> { // Titles — Run/Edit buttons
                if (editingTitle == -1 && runningTitle == -1) {
                    int y = PAD + 6 + 12 + 4 + 22;
                    for (int i = titleScroll; i < titles.size(); i++) {
                        int ry = y + (i - titleScroll) * ROW_H;
                        if (my < ry+2 || my >= ry+ROW_H-2) continue;
                        int bx1 = px + pw - PAD - BTN_S * 2 - 4;
                        int bx2 = bx1 + BTN_S + 4;
                        if (clickCol(mx, bx1, bx1+BTN_S)) { runningTitle = i; init(); return true; }
                        if (clickCol(mx, bx2, bx2+BTN_S)) { editingTitle = i; init(); return true; }
                    }
                }
            }
            case 2 -> { // Vanish
                int headerH = PAD + 6 + 12 + 4 + 16 + 4 + 12;
                // Team visibility toggle click
                int bx = px + pw - PAD - 60;
                if (mx >= bx && mx < bx + 60 && my >= PAD + 6 + 12 + 4 - 2 && my < PAD + 6 + 12 + 4 + 10) {
                    PacketDistributor.sendToServer(MenuActionPacket.of(MenuActionPacket.Action.TOGGLE_TEAM_VISIBILITY));
                    teamVisibility = !teamVisibility;
                    return true;
                }
                List<OpenMainMenuPacket.PlayerState> vanished = players.stream().filter(p -> p.vanished()).toList();
                int y = headerH;
                for (int i = 0; i < vanished.size(); i++) {
                    int ry = y + i * ROW_H;
                    if (my < ry+2 || my >= ry+ROW_H-2) continue;
                    var p = vanished.get(i);
                    int b1 = px + pw - PAD - 44*3 - 8;
                    int b2 = b1 + 48, b3 = b2 + 48;
                    if (clickCol(mx, b1, b1+44)) {
                        PacketDistributor.sendToServer(new MenuActionPacket(
                                MenuActionPacket.Action.VANISH_SHOW_TO, p.uuid(), p.uuid().toString(), "", 0));
                        return true;
                    }
                    if (clickCol(mx, b2, b2+44)) {
                        PacketDistributor.sendToServer(new MenuActionPacket(
                                MenuActionPacket.Action.VANISH_HIDE_FROM, p.uuid(), p.uuid().toString(), "", 0));
                        return true;
                    }
                    if (clickCol(mx, b3, b3+44)) {
                        PacketDistributor.sendToServer(new MenuActionPacket(
                                MenuActionPacket.Action.VANISH_CLEAR, null, p.uuid().toString(), "", 0));
                        return true;
                    }
                }
            }
            case 3 -> { // Regions — open button
                int y = PAD + 6 + 12 + 4 + 20;
                int x = panelX() + PAD;
                if (mx >= x && mx < x + 160 && my >= y && my < y + 20) {
                    PacketDistributor.sendToServer(new RequestRegionScreenPacket());
                    return true;
                }
            }
            case 4 -> { // Dims
                int y = PAD + 6 + 12 + 4 + 2;
                // Create button
                if (mx >= px + PAD && mx < px + PAD + 100 && my >= PAD+4 && my < PAD+18) {
                    // Open dimen create screen — close this first
                    PacketDistributor.sendToServer(MenuActionPacket.of(MenuActionPacket.Action.DIMEN_TP)); // reuse as signal
                    return true;
                }
                for (int i = dimScroll; i < dims.size(); i++) {
                    int ry = y + (i - dimScroll) * ROW_H;
                    if (my < ry+2 || my >= ry+ROW_H-2) continue;
                    int bx = px + pw - PAD - BTN_S;
                    if (clickCol(mx, bx, bx+BTN_S)) {
                        PacketDistributor.sendToServer(MenuActionPacket.stringAction(
                                MenuActionPacket.Action.DIMEN_TP, dims.get(i).name(), ""));
                        return true;
                    }
                }
            }
            case 5 -> { // Help
                int y = PAD + 6 + 12 + 4;
                for (int i = helpScroll; i < helpRequests.size(); i++) {
                    int ry = y + (i - helpScroll) * ROW_H;
                    if (my < ry+2 || my >= ry+ROW_H-2) continue;
                    int bx = px + pw - PAD - BTN_S;
                    if (clickCol(mx, bx, bx+BTN_S)) {
                        PacketDistributor.sendToServer(MenuActionPacket.playerAction(
                                MenuActionPacket.Action.HELP_ACCEPT, helpRequests.get(i).playerUUID()));
                        helpRequests.remove(i);
                        return true;
                    }
                }
            }
            case 6 -> { // Bulk
                int y = PAD + 6 + 12 + 4 + 12;
                for (int i = bulkScroll; i < bulkCommands.size(); i++) {
                    int ry = y + (i - bulkScroll) * ROW_H;
                    if (my < ry+2 || my >= ry+ROW_H-2) continue;
                    var b = bulkCommands.get(i);
                    // Run button
                    int bx = px + pw - PAD - BTN_S - 9*14;
                    if (clickCol(mx, bx, bx+BTN_S)) {
                        PacketDistributor.sendToServer(MenuActionPacket.stringAction(
                                MenuActionPacket.Action.BULK_RUN, b.name(), ""));
                        return true;
                    }
                    // Bind slot buttons 1-9
                    for (int slot = 1; slot <= 9; slot++) {
                        int sx = pw - PAD - (10 - slot) * 14;
                        if (clickCol(mx, px+sx, px+sx+12)) {
                            boolean isBound = b.name().equals(bindSlots.get(slot));
                            if (isBound) {
                                PacketDistributor.sendToServer(MenuActionPacket.intAction(
                                        MenuActionPacket.Action.BULK_UNBIND, "", slot));
                                bindSlots.remove(slot);
                            } else {
                                PacketDistributor.sendToServer(MenuActionPacket.intAction(
                                        MenuActionPacket.Action.BULK_BIND, b.name(), slot));
                                bindSlots.put(slot, b.name());
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int d = -(int) dy;
        switch (selectedCategory) {
            case 0 -> playerScroll = clampScroll(playerScroll + d, players.size());
            case 1 -> titleScroll  = clampScroll(titleScroll  + d, titles.size());
            case 4 -> dimScroll    = clampScroll(dimScroll    + d, dims.size());
            case 5 -> helpScroll   = clampScroll(helpScroll   + d, helpRequests.size());
            case 6 -> bulkScroll   = clampScroll(bulkScroll   + d, bulkCommands.size());
        }
        return true;
    }

    // ── Toggle helpers ────────────────────────────────────────────────────────

    private void togglePlayer(int idx, int col) {
        var p = players.get(idx);
        MenuActionPacket.Action action = switch (col) {
            case 0 -> MenuActionPacket.Action.TOGGLE_STAFF;
            case 1 -> MenuActionPacket.Action.TOGGLE_VANISH;
            case 2 -> MenuActionPacket.Action.TOGGLE_GOD;
            default -> MenuActionPacket.Action.TOGGLE_BLIND;
        };
        PacketDistributor.sendToServer(MenuActionPacket.playerAction(action, p.uuid()));
        players.set(idx, new OpenMainMenuPacket.PlayerState(
                p.uuid(), p.name(),
                col == 0 ? !p.staffMode() : p.staffMode(),
                col == 1 ? !p.vanished()  : p.vanished(),
                col == 2 ? !p.godMode()   : p.godMode(),
                col == 3 ? !p.blinded()   : p.blinded()
        ));
    }

    private void onSaveTitle() {
        if (titleNameBox == null || titleTextBox == null) return;
        String name = titleNameBox.getValue().trim();
        String text = titleTextBox.getValue().trim();
        if (name.isEmpty() || text.isEmpty()) return;
        String id = (editingTitle >= 0 && editingTitle < titles.size()) ? titles.get(editingTitle).id() : "";
        PacketDistributor.sendToServer(new SaveTitlePacket(id, name, text,
                titleSubBox.getValue().trim(),
                parseInt(titleFadeInBox.getValue(), 10),
                parseInt(titleStayBox.getValue(), 70),
                parseInt(titleFadeOutBox.getValue(), 20)));
        editingTitle = -1;
        onClose();
    }

    private void send(MenuActionPacket.Action action, String a1, String a2) {
        PacketDistributor.sendToServer(MenuActionPacket.stringAction(action, a1, a2));
    }

    // ── Draw helpers ──────────────────────────────────────────────────────────

    private int header(GuiGraphics g, int px, String title) {
        int y = PAD + 6;
        g.drawString(font, "\u00a7e" + title, px + PAD, y, 0xFFFFFF);
        y += 12;
        g.fill(px, y, px + panelW(), y + 1, 0xFF555555);
        return y + 4;
    }

    private void toggle(GuiGraphics g, int x, int ry, boolean on) {
        g.fill(x, ry+3, x+28, ry+ROW_H-3, on ? 0xFF225522 : 0xFF332222);
        g.drawCenteredString(font, on ? "\u00a7aON" : "\u00a7cOFF", x+14, ry+5, 0xFFFFFF);
    }

    private void inlineBtn(GuiGraphics g, int pw, int ry, int offsetFromRight, int w, String label, int bg) {
        int bx = panelX() + pw - PAD - offsetFromRight - w;
        g.fill(bx, ry+2, bx+w, ry+ROW_H-2, bg);
        g.drawCenteredString(font, label, bx+w/2, ry+5, 0xFFFFFF);
    }

    private void scissor(GuiGraphics g, int px, int y, int pw) {
        g.enableScissor(px+1, y, px+pw-1, height-PAD-1);
    }

    private boolean isHovered(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x+w && my >= y && my < y+h;
    }

    private boolean clickCol(double mx, int x1, int x2) {
        return mx >= x1 && mx < x2;
    }

    private int clampScroll(int val, int size) {
        return Math.max(0, Math.min(val, Math.max(0, size - 10)));
    }

    private EditBox addEditBox(int x, int y, int w, String hint) {
        EditBox box = new EditBox(font, x, y, w, 16, Component.literal(hint));
        box.setHint(Component.literal(hint));
        box.setMaxLength(256);
        addRenderableWidget(box);
        return box;
    }

    private int panelX() { return PAD + CAT_W + PAD; }
    private int panelW() { return width - panelX() - PAD; }
    private int parseInt(String s, int def) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; } }
    private String shorten(String s, int max) { return s.length() > max ? s.substring(0, max) + "..." : s; }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float delta) {}
}
