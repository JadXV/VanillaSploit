package com.jadxv.vanillasploit.mixin.client;

import net.minecraft.client.gui.screen.SplashTextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SplashTextRenderer.class)
public interface SplashTextMixin {
    @Accessor("text")
    String getText();

    @Mutable
    @Accessor("text")
    void setText(String text);
}
