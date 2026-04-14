package com.jadxv.vanillasploit.mixin.client;

import com.jadxv.vanillasploit.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class CriticalsMixin {
    
    // Intercept attackEntity which sends the actual attack packet to server
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        ModuleManager manager = ModuleManager.getInstance();
        if (!manager.criticalsEnabled) return;
        
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        
        if (networkHandler == null || mc.player == null) return;
        
        ClientPlayerEntity clientPlayer = mc.player;
        
        if (clientPlayer.isOnGround() && !clientPlayer.isTouchingWater() && !clientPlayer.isInLava() && !clientPlayer.isClimbing() && !clientPlayer.hasVehicle()) {
            double x = clientPlayer.getX();
            double y = clientPlayer.getY();
            double z = clientPlayer.getZ();
            float yaw = clientPlayer.getYaw();
            float pitch = clientPlayer.getPitch();
            
            // Send position packets BEFORE the attack packet to make server think we're falling
            // Simulate: jump up -> peak -> falling down
            networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.0625, z, yaw, pitch, false, clientPlayer.horizontalCollision));
            networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.1250, z, yaw, pitch, false, clientPlayer.horizontalCollision));
            networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.0625, z, yaw, pitch, false, clientPlayer.horizontalCollision));
            networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.00001, z, yaw, pitch, false, clientPlayer.horizontalCollision));
        }
    }
}
