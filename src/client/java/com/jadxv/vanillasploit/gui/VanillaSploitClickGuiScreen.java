package com.jadxv.vanillasploit.gui;

import com.jadxv.vanillasploit.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.client.util.InputUtil;

public class VanillaSploitClickGuiScreen extends Screen {

    private enum Category { MOVEMENT, COMBAT, PLAYER, VISUALS }

    private final ModuleManager manager = ModuleManager.getInstance();

    // Visual state only
    private boolean speedToggled = false;
    private boolean flyToggled = false;
    private int speedValue = 5; // 1-10
    private boolean killAuraToggled = false;
    private boolean criticalsToggled = false;
    private boolean noFallToggled = false;
    private boolean scaffoldToggled = false;

    private Category selected = Category.MOVEMENT;

    // Widgets
    private ButtonWidget closeButton;
    private ButtonWidget catMovementBtn, catCombatBtn, catPlayerBtn, catVisualsBtn;
    private ButtonWidget speedToggleButton, flyToggleButton;
    private ButtonWidget speedKeyButton, flyKeyButton;
    private ButtonWidget killAuraToggleButton, killAuraKeyButton;
    private ButtonWidget criticalsToggleButton, criticalsKeyButton;
    private ButtonWidget noFallToggleButton, noFallKeyButton;
    private ButtonWidget scaffoldToggleButton, scaffoldKeyButton;
    private SliderWidget speedSlider, flySpeedSlider;

    // Layout
    private int panelX, panelY, panelW, panelH;
    private int sidebarW, contentX, contentY, contentW;
    @SuppressWarnings("unused")
    private int contentH;
    private final int padding = 6;

    public VanillaSploitClickGuiScreen() {
        super(Text.translatable("screen.vanillasploit.click_gui.title"));
        // Load state from manager
        speedToggled = manager.speedEnabled;
        speedValue = manager.speedValue;
        flyToggled = manager.flyEnabled;
        killAuraToggled = manager.killAuraEnabled;
        criticalsToggled = manager.criticalsEnabled;
        noFallToggled = manager.noFallEnabled;
        scaffoldToggled = manager.scaffoldEnabled;
    }

    @Override
    protected void init() {
        computeLayout();

        // Sidebar category buttons
        catMovementBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Movement"), b -> {
            selected = Category.MOVEMENT;
            updateVisibility();
        }).dimensions(panelX + padding, panelY + 30, sidebarW - padding * 2, 18).build());

        catCombatBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Combat"), b -> {
            selected = Category.COMBAT;
            updateVisibility();
        }).dimensions(panelX + padding, panelY + 30 + 20, sidebarW - padding * 2, 18).build());

        catPlayerBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Player"), b -> {
            selected = Category.PLAYER;
            updateVisibility();
        }).dimensions(panelX + padding, panelY + 30 + 40, sidebarW - padding * 2, 18).build());

        catVisualsBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Visuals"), b -> {
            selected = Category.VISUALS;
            updateVisibility();
        }).dimensions(panelX + padding, panelY + 30 + 60, sidebarW - padding * 2, 18).build());

        // Content widgets for Movement

        // Speed toggle and keybind
        speedToggleButton = addDrawableChild(ButtonWidget.builder(Text.literal("Speed: " + (speedToggled ? "On" : "Off")), btn -> {
            speedToggled = !speedToggled;
            manager.speedEnabled = speedToggled;
            btn.setMessage(Text.literal("Speed: " + (speedToggled ? "On" : "Off")));
        }).dimensions(contentX + padding, contentY + padding, 80, 18).build());

        speedKeyButton = addDrawableChild(ButtonWidget.builder(Text.literal(getKeybindText("Speed")), btn -> {
            manager.startBinding("Speed");
            btn.setMessage(Text.literal("Press a key..."));
        }).dimensions(contentX + padding + 210, contentY + padding, 60, 18).build());

        speedSlider = addDrawableChild(new SpeedSlider(contentX + padding + 86, contentY + padding, 120, 18));

        // Fly toggle and keybind
        flyToggleButton = addDrawableChild(ButtonWidget.builder(Text.literal("Fly: " + (flyToggled ? "On" : "Off")), btn -> {
            flyToggled = !flyToggled;
            manager.flyEnabled = flyToggled;
            btn.setMessage(Text.literal("Fly: " + (flyToggled ? "On" : "Off")));
        }).dimensions(contentX + padding, contentY + padding + 24, 80, 18).build());

        flyKeyButton = addDrawableChild(ButtonWidget.builder(Text.literal(getKeybindText("Fly")), btn -> {
            manager.startBinding("Fly");
            btn.setMessage(Text.literal("Press a key..."));
        }).dimensions(contentX + padding + 210, contentY + padding + 24, 60, 18).build());

        flySpeedSlider = addDrawableChild(new FlySpeedSlider(contentX + padding + 86, contentY + padding + 24, 120, 18));

        // Combat widgets
        // KillAura toggle and keybind
        killAuraToggleButton = addDrawableChild(ButtonWidget.builder(Text.literal("KillAura: " + (killAuraToggled ? "On" : "Off")), btn -> {
            killAuraToggled = !killAuraToggled;
            manager.killAuraEnabled = killAuraToggled;
            btn.setMessage(Text.literal("KillAura: " + (killAuraToggled ? "On" : "Off")));
        }).dimensions(contentX + padding, contentY + padding, 80, 18).build());

        killAuraKeyButton = addDrawableChild(ButtonWidget.builder(Text.literal(getKeybindText("KillAura")), btn -> {
            manager.startBinding("KillAura");
            btn.setMessage(Text.literal("Press a key..."));
        }).dimensions(contentX + padding + 210, contentY + padding, 60, 18).build());

        // Criticals toggle and keybind
        criticalsToggleButton = addDrawableChild(ButtonWidget.builder(Text.literal("Criticals: " + (criticalsToggled ? "On" : "Off")), btn -> {
            criticalsToggled = !criticalsToggled;
            manager.criticalsEnabled = criticalsToggled;
            btn.setMessage(Text.literal("Criticals: " + (criticalsToggled ? "On" : "Off")));
        }).dimensions(contentX + padding, contentY + padding + 24, 80, 18).build());

        criticalsKeyButton = addDrawableChild(ButtonWidget.builder(Text.literal(getKeybindText("Criticals")), btn -> {
            manager.startBinding("Criticals");
            btn.setMessage(Text.literal("Press a key..."));
        }).dimensions(contentX + padding + 210, contentY + padding + 24, 60, 18).build());

        // Player widgets
        // NoFall toggle and keybind
        noFallToggleButton = addDrawableChild(ButtonWidget.builder(Text.literal("NoFall: " + (noFallToggled ? "On" : "Off")), btn -> {
            noFallToggled = !noFallToggled;
            manager.noFallEnabled = noFallToggled;
            btn.setMessage(Text.literal("NoFall: " + (noFallToggled ? "On" : "Off")));
        }).dimensions(contentX + padding, contentY + padding, 80, 18).build());

        noFallKeyButton = addDrawableChild(ButtonWidget.builder(Text.literal(getKeybindText("NoFall")), btn -> {
            manager.startBinding("NoFall");
            btn.setMessage(Text.literal("Press a key..."));
        }).dimensions(contentX + padding + 210, contentY + padding, 60, 18).build());

        // Scaffold toggle and keybind
        scaffoldToggleButton = addDrawableChild(ButtonWidget.builder(Text.literal("Scaffold: " + (scaffoldToggled ? "On" : "Off")), btn -> {
            scaffoldToggled = !scaffoldToggled;
            manager.scaffoldEnabled = scaffoldToggled;
            btn.setMessage(Text.literal("Scaffold: " + (scaffoldToggled ? "On" : "Off")));
        }).dimensions(contentX + padding, contentY + padding + 24, 80, 18).build());

        scaffoldKeyButton = addDrawableChild(ButtonWidget.builder(Text.literal(getKeybindText("Scaffold")), btn -> {
            manager.startBinding("Scaffold");
            btn.setMessage(Text.literal("Press a key..."));
        }).dimensions(contentX + padding + 210, contentY + padding + 24, 60, 18).build());

        // Close
        closeButton = addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> {
            if (this.client != null) this.client.setScreen(null);
        }).dimensions(panelX + panelW - padding - 60, panelY + panelH - padding - 20, 60, 18).build());

        updateVisibility();
    }

    private String getKeybindText(String module) {
        int code = manager.getKeybind(module);
        if (code == -1) return "No Bind";
        String name = InputUtil.Type.KEYSYM.createFromCode(code).getLocalizedText().getString();
        return name == null || name.isEmpty() ? "No Bind" : name;
    }

    public void updateKeybindLabels() {
        if (speedKeyButton != null) {
            speedKeyButton.setMessage(Text.literal(getKeybindText("Speed")));
        }
        if (flyKeyButton != null) {
            flyKeyButton.setMessage(Text.literal(getKeybindText("Fly")));
        }
        if (killAuraKeyButton != null) {
            killAuraKeyButton.setMessage(Text.literal(getKeybindText("KillAura")));
        }
        if (criticalsKeyButton != null) {
            criticalsKeyButton.setMessage(Text.literal(getKeybindText("Criticals")));
        }
        if (noFallKeyButton != null) {
            noFallKeyButton.setMessage(Text.literal(getKeybindText("NoFall")));
        }
        if (scaffoldKeyButton != null) {
            scaffoldKeyButton.setMessage(Text.literal(getKeybindText("Scaffold")));
        }
    }

    // key capture for binding is handled in ModuleManager.handleKeybinds()

    private void computeLayout() {
        int margin = 16;
        panelW = Math.max(320, Math.min(this.width - margin * 2, 480));
        panelH = Math.max(180, Math.min(this.height - margin * 2, 240));
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        sidebarW = 90;
        contentX = panelX + sidebarW + padding;
        contentY = panelY + 28;
        contentW = panelW - sidebarW - padding * 2;
        contentH = panelH - (contentY - panelY) - padding;
        if (contentW < 140) contentW = 140; // minimal width safety
    }

    private void updateLayoutPositions() {
        // Sidebar buttons
        int sx = panelX + padding;
        int sy = panelY + 30;
        int sw = sidebarW - padding * 2;
        if (catMovementBtn != null) catMovementBtn.setPosition(sx, sy);
        if (catCombatBtn != null) catCombatBtn.setPosition(sx, sy + 20);
        if (catPlayerBtn != null) catPlayerBtn.setPosition(sx, sy + 40);
        if (catVisualsBtn != null) catVisualsBtn.setPosition(sx, sy + 60);
        if (catMovementBtn != null) catMovementBtn.setWidth(sw);
        if (catCombatBtn != null) catCombatBtn.setWidth(sw);
        if (catPlayerBtn != null) catPlayerBtn.setWidth(sw);
        if (catVisualsBtn != null) catVisualsBtn.setWidth(sw);

        // Content widgets (movement)
        int cx = contentX + padding;
        int cy = contentY + padding;
        if (speedToggleButton != null) {
            speedToggleButton.setPosition(cx, cy);
            speedToggleButton.setWidth(80);
        }
        if (speedKeyButton != null) {
            speedKeyButton.setPosition(cx + 210, cy);
            speedKeyButton.setWidth(60);
        }
        if (speedSlider != null) {
            speedSlider.setPosition(cx + 86, cy);
            speedSlider.setWidth(120);
        }
        if (flyToggleButton != null) {
            flyToggleButton.setPosition(cx, cy + 24);
            flyToggleButton.setWidth(80);
        }
        if (flyKeyButton != null) {
            flyKeyButton.setPosition(cx + 210, cy + 24);
            flyKeyButton.setWidth(60);
        }
        if (flySpeedSlider != null) {
            flySpeedSlider.setPosition(cx + 86, cy + 24);
            flySpeedSlider.setWidth(120);
        }

        // Combat widgets
        if (killAuraToggleButton != null) {
            killAuraToggleButton.setPosition(cx, cy);
            killAuraToggleButton.setWidth(80);
        }
        if (killAuraKeyButton != null) {
            killAuraKeyButton.setPosition(cx + 210, cy);
            killAuraKeyButton.setWidth(60);
        }
        if (criticalsToggleButton != null) {
            criticalsToggleButton.setPosition(cx, cy + 24);
            criticalsToggleButton.setWidth(80);
        }
        if (criticalsKeyButton != null) {
            criticalsKeyButton.setPosition(cx + 210, cy + 24);
            criticalsKeyButton.setWidth(60);
        }

        // Player widgets
        if (noFallToggleButton != null) {
            noFallToggleButton.setPosition(cx, cy);
            noFallToggleButton.setWidth(80);
        }
        if (noFallKeyButton != null) {
            noFallKeyButton.setPosition(cx + 210, cy);
            noFallKeyButton.setWidth(60);
        }
        if (scaffoldToggleButton != null) {
            scaffoldToggleButton.setPosition(cx, cy + 24);
            scaffoldToggleButton.setWidth(80);
        }
        if (scaffoldKeyButton != null) {
            scaffoldKeyButton.setPosition(cx + 210, cy + 24);
            scaffoldKeyButton.setWidth(60);
        }

        if (closeButton != null) {
            closeButton.setPosition(panelX + panelW - padding - 60, panelY + panelH - padding - 20);
        }
    }

    private void updateVisibility() {
        boolean movement = selected == Category.MOVEMENT;
        boolean combat = selected == Category.COMBAT;
        boolean player = selected == Category.PLAYER;
        // visuals category has no modules yet
        if (speedToggleButton != null) {
            speedToggleButton.visible = movement;
            speedToggleButton.active = movement;
        }
        if (speedKeyButton != null) {
            speedKeyButton.visible = movement;
            speedKeyButton.active = movement;
        }
        if (speedSlider != null) {
            speedSlider.visible = movement;
            speedSlider.active = movement;
        }
        if (flyToggleButton != null) {
            flyToggleButton.visible = movement;
            flyToggleButton.active = movement;
        }
        if (flyKeyButton != null) {
            flyKeyButton.visible = movement;
            flyKeyButton.active = movement;
        }
        if (flySpeedSlider != null) {
            flySpeedSlider.visible = movement;
            flySpeedSlider.active = movement;
        }
        if (killAuraToggleButton != null) {
            killAuraToggleButton.visible = combat;
            killAuraToggleButton.active = combat;
        }
        if (killAuraKeyButton != null) {
            killAuraKeyButton.visible = combat;
            killAuraKeyButton.active = combat;
        }
        if (criticalsToggleButton != null) {
            criticalsToggleButton.visible = combat;
            criticalsToggleButton.active = combat;
        }
        if (criticalsKeyButton != null) {
            criticalsKeyButton.visible = combat;
            criticalsKeyButton.active = combat;
        }
        if (noFallToggleButton != null) {
            noFallToggleButton.visible = player;
            noFallToggleButton.active = player;
        }
        if (noFallKeyButton != null) {
            noFallKeyButton.visible = player;
            noFallKeyButton.active = player;
        }
        if (scaffoldToggleButton != null) {
            scaffoldToggleButton.visible = player;
            scaffoldToggleButton.active = player;
        }
        if (scaffoldKeyButton != null) {
            scaffoldKeyButton.visible = player;
            scaffoldKeyButton.active = player;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        computeLayout();
        updateLayoutPositions();

        // Dim background
        context.fill(0, 0, this.width, this.height, 0x88000000);

        // Panel
        int x = panelX, y = panelY;
        context.fill(x, y, x + panelW, y + panelH, 0xCC1E1E1E);
        int borderColor = 0xFFFFFFFF;
        int t = 2;
        context.fill(x, y, x + panelW, y + t, borderColor);
        context.fill(x, y, x + t, y + panelH, borderColor);
        context.fill(x + panelW - t, y, x + panelW, y + panelH, borderColor);
        context.fill(x, y + panelH - t, x + panelW, y + panelH, borderColor);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.getTitle(), this.width / 2, y + 8, 0xFFFFFF);

        // Sidebar title and separator
        context.drawText(this.textRenderer, "Categories", panelX + padding, panelY + 18, 0xFFB0B0B0, false);
        int sepX = panelX + sidebarW;
        context.fill(sepX, y + 28, sepX + 1, y + panelH - padding, 0x66FFFFFF);

        // Content header
        String header = switch (selected) {
            case MOVEMENT -> "Movement";
            case COMBAT -> "Combat";
            case PLAYER -> "Player";
            case VISUALS -> "Visuals";
        };
        context.drawText(this.textRenderer, header, contentX + padding, contentY - 10, 0xFFFFFFFF, false);

        // ...existing code...

        // ...existing code...

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void resize(net.minecraft.client.MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        computeLayout();
        updateLayoutPositions();
    }

    // Slider for 1..10 (visual only)
    private class SpeedSlider extends SliderWidget {
        SpeedSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Text.empty(), (speedValue - 1) / 9.0);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Speed: " + speedValue));
        }

        @Override
        protected void applyValue() {
            speedValue = 1 + (int) Math.round(this.value * 9.0);
            manager.speedValue = speedValue;
            updateMessage();
        }
    }

    // Slider for 1..10 (visual only, for fly speed)
    private class FlySpeedSlider extends SliderWidget {
        FlySpeedSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Text.empty(), (manager.flySpeedValue - 1) / 9.0);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Fly Speed: " + manager.flySpeedValue));
        }

        @Override
        protected void applyValue() {
            manager.flySpeedValue = 1 + (int) Math.round(this.value * 9.0);
            updateMessage();
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
