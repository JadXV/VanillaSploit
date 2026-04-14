package com.jadxv.vanillasploit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
@SuppressWarnings("deprecation")
public class ActiveModulesHud implements HudRenderCallback {
    @Override
    public void onHudRender(net.minecraft.client.gui.DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        ModuleManager manager = ModuleManager.getInstance();
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        Window window = mc.getWindow();
        int x = window.getScaledWidth() - 6;
        int y = 6;
        
        // Draw Scaffold block counter HUD (above hotbar)
        if (manager.scaffoldEnabled && mc.player != null) {
            drawScaffoldHud(context, mc, tr, window);
        }

        // Draw VanillaSploit title
        String title = "VanillaSploit";
        int titleWidth = tr.getWidth(title);
        context.drawText(tr, title, x - titleWidth, y, 0xFFFF55FF, false);
        y += tr.fontHeight + 6;

        // Draw active modules with keybinds and settings
        if (manager.speedEnabled) {
            String keyName = getKeyName(manager.getKeybind("Speed"));
            String text = "Speed [" + keyName + "] " + manager.speedValue;
            int textWidth = tr.getWidth(text);
            context.drawText(tr, text, x - textWidth, y, 0xFF00FFAA, true);
            y += tr.fontHeight + 2;
        }
        if (manager.flyEnabled) {
            String keyName = getKeyName(manager.getKeybind("Fly"));
            String text = "Fly [" + keyName + "] " + manager.flySpeedValue;
            int textWidth = tr.getWidth(text);
            context.drawText(tr, text, x - textWidth, y, 0xFF00FFAA, true);
            y += tr.fontHeight + 2;
        }
        if (manager.killAuraEnabled) {
            String keyName = getKeyName(manager.getKeybind("KillAura"));
            String text = "KillAura [" + keyName + "]";
            int textWidth = tr.getWidth(text);
            context.drawText(tr, text, x - textWidth, y, 0xFF00FFAA, true);
            y += tr.fontHeight + 2;
        }
        if (manager.criticalsEnabled) {
            String keyName = getKeyName(manager.getKeybind("Criticals"));
            String text = "Criticals [" + keyName + "]";
            int textWidth = tr.getWidth(text);
            context.drawText(tr, text, x - textWidth, y, 0xFF00FFAA, true);
            y += tr.fontHeight + 2;
        }
        if (manager.noFallEnabled) {
            String keyName = getKeyName(manager.getKeybind("NoFall"));
            String text = "NoFall [" + keyName + "]";
            int textWidth = tr.getWidth(text);
            context.drawText(tr, text, x - textWidth, y, 0xFF00FFAA, true);
            y += tr.fontHeight + 2;
        }
        if (manager.scaffoldEnabled) {
            String keyName = getKeyName(manager.getKeybind("Scaffold"));
            String text = "Scaffold [" + keyName + "]";
            int textWidth = tr.getWidth(text);
            context.drawText(tr, text, x - textWidth, y, 0xFF00FFAA, true);
            y += tr.fontHeight + 2;
        }
    }

    private String getKeyName(int keyCode) {
        if (keyCode == -1) return "None";
        String name = InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText().getString();
        return (name == null || name.isEmpty()) ? "None" : name;
    }
    
    private void drawScaffoldHud(net.minecraft.client.gui.DrawContext context, MinecraftClient mc, TextRenderer tr, Window window) {
        // Find the first block in hotbar and count total blocks
        ItemStack blockStack = ItemStack.EMPTY;
        int totalBlocks = 0;
        
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                if (blockStack.isEmpty()) {
                    blockStack = stack;
                }
                totalBlocks += stack.getCount();
            }
        }
        
        // Also count blocks in inventory (not hotbar)
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                if (blockStack.isEmpty()) {
                    blockStack = stack;
                }
                // Only count same type of block
                if (!blockStack.isEmpty() && stack.getItem() == blockStack.getItem()) {
                    totalBlocks += stack.getCount();
                }
            }
        }
        
        // Position: center of screen, above hotbar and hearts
        int centerX = window.getScaledWidth() / 2;
        int hudY = window.getScaledHeight() - 22 - 22 - 16 - 6; // hotbar(22) + hearts area(22) + extra padding(16) + gap(6)
        
        // Smaller box
        int boxWidth = 40;
        int boxHeight = 18;
        int boxX = centerX - boxWidth / 2;
        int boxY = hudY;
        
        // Draw rounded background (simulate with filled rects)
        int bgColor = 0x90000000;
        int radius = 3;
        
        // Main body
        context.fill(boxX + radius, boxY, boxX + boxWidth - radius, boxY + boxHeight, bgColor);
        // Top and bottom strips
        context.fill(boxX, boxY + radius, boxX + boxWidth, boxY + boxHeight - radius, bgColor);
        // Corner pixels for rounded effect
        context.fill(boxX + 1, boxY + 1, boxX + radius, boxY + radius, bgColor);
        context.fill(boxX + boxWidth - radius, boxY + 1, boxX + boxWidth - 1, boxY + radius, bgColor);
        context.fill(boxX + 1, boxY + boxHeight - radius, boxX + radius, boxY + boxHeight - 1, bgColor);
        context.fill(boxX + boxWidth - radius, boxY + boxHeight - radius, boxX + boxWidth - 1, boxY + boxHeight - 1, bgColor);
        
        // Draw block icon (smaller, centered) and count
        String countText = String.valueOf(totalBlocks);
        int textColor = totalBlocks > 64 ? 0xFF00FF00 : (totalBlocks > 0 ? 0xFFFFFFFF : 0xFFFF5555);
        
        if (!blockStack.isEmpty()) {
            // With block icon: icon on left, count on right
            context.drawItem(blockStack, boxX + 2, boxY + 1);
            context.drawText(tr, countText, boxX + 20, boxY + 5, textColor, true);
        } else {
            // No blocks: center the "0" text
            int textWidth = tr.getWidth(countText);
            context.drawText(tr, countText, boxX + (boxWidth - textWidth) / 2, boxY + 5, textColor, true);
        }
    }
}
