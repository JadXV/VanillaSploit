package com.jadxv.vanillasploit.mixin.client;

import com.jadxv.vanillasploit.ModuleManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class NoFallMixin {
    
    // Send spoofed packets to prevent fall damage
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        ModuleManager manager = ModuleManager.getInstance();
        if (!manager.noFallEnabled) return;
        
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        
        // Send spoofed packet to server if we're falling
        if (player.getVelocity().y < -0.1 && !player.isOnGround() && player.networkHandler != null) {
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            float yaw = player.getYaw();
            float pitch = player.getPitch();
            
            // Tell server we're on ground to prevent fall damage
            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, true, false));
        }
    }
}
