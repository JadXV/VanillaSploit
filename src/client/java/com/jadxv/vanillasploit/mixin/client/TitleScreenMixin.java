package com.jadxv.vanillasploit.mixin.client;

import com.jadxv.vanillasploit.gui.AccountManagerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Shadow
    private SplashTextRenderer splashText;

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (this.splashText != null) {
            ((SplashTextMixin)(Object)this.splashText).setText("VanillaSploit!");
        }
        
        // Add Account Manager button in top right
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Accounts"), button -> {
            this.client.setScreen(new AccountManagerScreen((TitleScreen)(Object)this));
        }).dimensions(this.width - 85, 5, 80, 20).build());
    }
}
