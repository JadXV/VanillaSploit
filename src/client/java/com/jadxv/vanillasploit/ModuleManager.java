package com.jadxv.vanillasploit;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public class ModuleManager {
    private static ModuleManager INSTANCE;

    // Speed module state
    public boolean speedEnabled = false;
    public int speedValue = 5; // 1-10

    // Fly module state
    public boolean flyEnabled = false;
    public int flySpeedValue = 5; // 1-10

    // KillAura module state
    public boolean killAuraEnabled = false;

    // Criticals module state
    public boolean criticalsEnabled = false;

    // NoFall module state
    public boolean noFallEnabled = false;

    // Scaffold module state
    public boolean scaffoldEnabled = false;

    // Keybinds: module name -> key code (GLFW)
    private final Map<String, Integer> keybinds = new HashMap<>();
    private final Map<String, Boolean> lastPressed = new HashMap<>();
    // For binding state
    private String bindingModule = null;

    private ModuleManager() {
        keybinds.put("Speed", -1);
        keybinds.put("Fly", -1);
        keybinds.put("KillAura", -1);
        keybinds.put("Criticals", -1);
        keybinds.put("NoFall", -1);
        keybinds.put("Scaffold", -1);
        lastPressed.put("Speed", false);
        lastPressed.put("Fly", false);
        lastPressed.put("KillAura", false);
        lastPressed.put("Criticals", false);
        lastPressed.put("NoFall", false);
        lastPressed.put("Scaffold", false);
    }
    // Keybind API
    public int getKeybind(String module) {
        return keybinds.getOrDefault(module, -1);
    }

    public void setKeybind(String module, int keyCode) {
        keybinds.put(module, keyCode);
        lastPressed.put(module, false);
    }

    public String getBindingModule() {
        return bindingModule;
    }

    public void startBinding(String module) {
        bindingModule = module;
    }

    public void stopBinding() {
        bindingModule = null;
    }

    // Call this from client tick to handle keybind toggles
    public void handleKeybinds() {
        if (bindingModule != null) return; // don't toggle while binding
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return;
        if (mc.currentScreen != null) return; // don't toggle while any screen is open
        for (Map.Entry<String, Integer> entry : keybinds.entrySet()) {
            String module = entry.getKey();
            int code = entry.getValue();
            boolean wasDown = lastPressed.getOrDefault(module, false);
            boolean isDown = code >= 0 && InputUtil.isKeyPressed(mc.getWindow(), code);
            if (isDown && !wasDown) {
                toggleModule(module);
            }
            lastPressed.put(module, isDown);
        }
    }

    private void toggleModule(String module) {
        switch (module) {
            case "Speed" -> speedEnabled = !speedEnabled;
            case "Fly" -> flyEnabled = !flyEnabled;
            case "KillAura" -> killAuraEnabled = !killAuraEnabled;
            case "Criticals" -> criticalsEnabled = !criticalsEnabled;
            case "NoFall" -> noFallEnabled = !noFallEnabled;
            case "Scaffold" -> scaffoldEnabled = !scaffoldEnabled;
        }
    }

    public static ModuleManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ModuleManager();
        }
        return INSTANCE;
    }

    public float getSpeedMultiplier() {
        if (!speedEnabled) return 1.0f;
        // Map 1-10 to multipliers 1.0 - 5.0
        return 1.0f + ((speedValue - 1) / 9.0f) * 4.0f;
    }

    public float getFlySpeed() {
        // Default creative fly speed is ~0.05f. We'll map 1..10 to 0.02..0.30 linearly.
        float min = 0.02f;
        float max = 0.30f;
        return min + ((flySpeedValue - 1) / 9.0f) * (max - min);
    }
}
