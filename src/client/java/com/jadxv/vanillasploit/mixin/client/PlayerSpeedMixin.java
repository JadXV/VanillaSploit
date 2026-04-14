package com.jadxv.vanillasploit.mixin.client;

import com.jadxv.vanillasploit.ModuleManager;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerSpeedMixin {
    @Unique
    private static final Identifier SPEED_MODIFIER_ID = Identifier.of("vanillasploit", "speed");
    @Unique
    private float lastSpeedMultiplier = 1.0f;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!player.getEntityWorld().isClient()) return;

        ModuleManager manager = ModuleManager.getInstance();
        float speedMultiplier = manager.getSpeedMultiplier();

        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (attribute == null) return;

        // Desired modifier presence/value this tick
        double amount = Math.max(0.0, speedMultiplier - 1.0); // 0.0 disables modifier
        EntityAttributeModifier existing = attribute.getModifier(SPEED_MODIFIER_ID);

        // If disabled or multiplier ~ 1.0, ensure modifier is removed
        if (amount <= 0.0001) {
            if (existing != null) {
                attribute.removeModifier(SPEED_MODIFIER_ID);
            }
            lastSpeedMultiplier = speedMultiplier;
            return;
        }

        // Ensure correct modifier exists; re-add every time if missing or value drifted
        boolean needsReplace = existing == null
                || existing.operation() != EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                || Math.abs(existing.value() - amount) > 1e-3;
        if (needsReplace) {
            if (existing != null) attribute.removeModifier(SPEED_MODIFIER_ID);
            EntityAttributeModifier modifier = new EntityAttributeModifier(
                    SPEED_MODIFIER_ID,
                    amount,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            attribute.addPersistentModifier(modifier);
        }

        lastSpeedMultiplier = speedMultiplier;
    }
}
