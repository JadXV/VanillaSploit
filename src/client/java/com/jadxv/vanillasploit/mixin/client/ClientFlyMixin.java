package com.jadxv.vanillasploit.mixin.client;

import com.jadxv.vanillasploit.ModuleManager;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientFlyMixin {
    @Unique
    private boolean lastAllow = false;
    @Unique
    private boolean lastFlying = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        ModuleManager manager = ModuleManager.getInstance();

        boolean isCreative = player.isCreative();
        boolean isSpectator = player.isSpectator();
        var abilities = player.getAbilities();

        boolean changed = false;

        if (manager.flyEnabled && !isSpectator) {
            // Enable flying like creative
            if (!abilities.allowFlying) { abilities.allowFlying = true; changed = true; }
            if (!abilities.flying) { abilities.flying = true; changed = true; }
            // Apply fly speed from manager (default creative is ~0.05F)
            float targetFly = manager.getFlySpeed();
            if (Math.abs(abilities.getFlySpeed() - targetFly) > 1e-4) {
                abilities.setFlySpeed(targetFly);
                changed = true;
            }
            player.fallDistance = 0.0F;
        } else {
            // If not creative/spectator, disable our fly override
            if (!isCreative && !isSpectator) {
                if (abilities.flying) { abilities.flying = false; changed = true; }
                if (abilities.allowFlying) { abilities.allowFlying = false; changed = true; }
                if (Math.abs(abilities.getFlySpeed() - 0.05F) > 1e-4) {
                    abilities.setFlySpeed(0.05F); // reset to vanilla default
                    changed = true;
                }
            }
        }

        // Send to server if anything changed
        if (changed) {
            player.sendAbilitiesUpdate();
        }

        lastAllow = abilities.allowFlying;
        lastFlying = abilities.flying;
    }
}
