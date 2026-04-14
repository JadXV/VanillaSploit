package com.jadxv.vanillasploit.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface SessionAccessor {
    @Accessor("session")
    @Mutable
    void setSession(Session session);
}
