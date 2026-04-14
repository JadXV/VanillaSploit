package com.jadxv.vanillasploit.mixin.client;

import com.jadxv.vanillasploit.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientPlayerEntity.class)
public abstract class KillAuraMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        ModuleManager manager = ModuleManager.getInstance();
        if (!manager.killAuraEnabled) {
            return;
        }

        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        // Respect vanilla attack cooldown for the current weapon
        // 1.0f means fully cooled down for max damage
        if (player.getAttackCooldownProgress(0.5f) < 1.0f) {
            return;
        }

        // Find entities within 3 block radius
        Box searchBox = player.getBoundingBox().expand(3.0);
        List<Entity> entities = player.getEntityWorld().getOtherEntities(player, searchBox);

        // Pick the closest valid living entity (exclude self and dead entities)
        Entity bestTarget = null;
        double bestDistSq = Double.MAX_VALUE;
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity) || entity instanceof ClientPlayerEntity) continue;
            if (!entity.isAlive()) continue;
            double distSq = player.squaredDistanceTo(entity);
            if (distSq <= 9.0 && distSq < bestDistSq) { // 3 blocks radius squared
                bestDistSq = distSq;
                bestTarget = entity;
            }
        }

        if (bestTarget != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.interactionManager != null) {
                mc.interactionManager.attackEntity(player, bestTarget);
                player.swingHand(Hand.MAIN_HAND);
            }
        }
    }
}
