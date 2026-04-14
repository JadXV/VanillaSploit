package com.jadxv.vanillasploit;

import com.jadxv.vanillasploit.gui.VanillaSploitClickGuiScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category; // import Category
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class VanillaSploitClient implements ClientModInitializer {

    private static KeyBinding openClickGuiKey;

    @SuppressWarnings("deprecation")
    @Override
    public void onInitializeClient() {
    // Register HUD overlay for active modules
    net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(new ActiveModulesHud());
        // Register Right Shift as the key to open the Click GUI
        openClickGuiKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.vanillasploit.open_click_gui",   // translation key
                InputUtil.Type.KEYSYM,        // key type
                GLFW.GLFW_KEY_RIGHT_SHIFT,    // default key
                Category.MISC                 // use enum instead of String
            )
        );

        // Open our GUI when the key is pressed, and handle module keybinds
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ModuleManager manager = ModuleManager.getInstance();
            manager.handleKeybinds();

            // If we're waiting for a bind, capture the next pressed key
            if (manager.getBindingModule() != null && client != null && client.getWindow() != null) {
                // Iterate reasonable range of GLFW key codes to find a press
                for (int code = 32; code <= 348; code++) { // from SPACE to MENU
                    if (InputUtil.isKeyPressed(client.getWindow(), code)) {
                        if (code == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                            manager.setKeybind(manager.getBindingModule(), -1);
                        } else {
                            manager.setKeybind(manager.getBindingModule(), code);
                        }
                        manager.stopBinding();
                        // Update the GUI button labels if the ClickGui is open
                        if (client.currentScreen instanceof VanillaSploitClickGuiScreen) {
                            ((VanillaSploitClickGuiScreen) client.currentScreen).updateKeybindLabels();
                        }
                        break;
                    }
                }
            }

            if (openClickGuiKey.wasPressed()) {
                MinecraftClient mc = client != null ? client : MinecraftClient.getInstance();
                if (mc.currentScreen == null) {
                    mc.setScreen(new VanillaSploitClickGuiScreen());
                }
            }
        });
    }
}
