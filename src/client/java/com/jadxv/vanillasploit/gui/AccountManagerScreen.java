package com.jadxv.vanillasploit.gui;

import com.jadxv.vanillasploit.AccountManager;
import com.jadxv.vanillasploit.AccountManager.Account;
import com.jadxv.vanillasploit.MicrosoftAuth;
import com.jadxv.vanillasploit.mixin.client.SessionAccessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccountManagerScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget usernameField;
    private int scrollOffset = 0;
    private Account selectedAccount = null;
    private List<ButtonWidget> accountButtons = new ArrayList<>();
    
    // Microsoft auth state
    private boolean msAuthInProgress = false;
    private String msAuthStatus = "";
    private String msAuthCode = "";
    private ButtonWidget msLoginButton;
    
    public AccountManagerScreen(Screen parent) {
        super(Text.literal("Account Manager"));
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int listWidth = 220;
        int listX = centerX - listWidth / 2;
        
        // Username input field for cracked accounts
        this.usernameField = new TextFieldWidget(
            this.textRenderer, 
            listX, 
            50, 
            listWidth - 60, 
            20, 
            Text.literal("Username")
        );
        this.usernameField.setPlaceholder(Text.literal("Cracked username..."));
        this.usernameField.setMaxLength(16);
        this.addDrawableChild(this.usernameField);
        
        // Add cracked account button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add"), button -> {
            String username = this.usernameField.getText().trim();
            if (!username.isEmpty()) {
                AccountManager.getInstance().addAccount(Account.cracked(username));
                this.usernameField.setText("");
                refreshAccountList();
            }
        }).dimensions(listX + listWidth - 55, 50, 55, 20).build());
        
        // Microsoft Login button
        msLoginButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("§b+ Microsoft Account"), button -> {
            if (!msAuthInProgress) {
                startMicrosoftAuth();
            }
        }).dimensions(listX, 75, listWidth, 20).build());
        
        // Login button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Login"), button -> {
            if (selectedAccount != null) {
                loginAs(selectedAccount);
            }
        }).dimensions(listX, this.height - 60, 70, 20).build());
        
        // Remove button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), button -> {
            if (selectedAccount != null) {
                AccountManager.getInstance().removeAccount(selectedAccount);
                selectedAccount = null;
                refreshAccountList();
            }
        }).dimensions(listX + 75, this.height - 60, 70, 20).build());
        
        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> {
            this.client.setScreen(this.parent);
        }).dimensions(listX + 150, this.height - 60, 70, 20).build());
        
        // Create account list buttons
        refreshAccountList();
    }
    
    private void startMicrosoftAuth() {
        System.out.println("[VanillaSploit] startMicrosoftAuth called!");
        msAuthInProgress = true;
        msAuthStatus = "Starting...";
        msLoginButton.active = false;
        
        // Run on a new thread to avoid blocking
        new Thread(() -> {
            System.out.println("[VanillaSploit] Auth thread started!");
            try {
                System.out.println("[VanillaSploit] Calling startDeviceCodeFlowSync...");
                MicrosoftAuth.DeviceCodeResponse deviceCode = MicrosoftAuth.startDeviceCodeFlowSync();
                System.out.println("[VanillaSploit] deviceCode result: " + deviceCode);
                
                if (deviceCode == null) {
                    if (this.client != null) {
                        this.client.execute(() -> {
                            msAuthInProgress = false;
                            msAuthStatus = "§cFailed to start login - check internet";
                            msLoginButton.active = true;
                        });
                    }
                    return;
                }
                
                // Update UI on main thread
                if (this.client != null) {
                    this.client.execute(() -> {
                        msAuthCode = deviceCode.userCode();
                        msAuthStatus = "Code: §e§l" + msAuthCode + "§r - Opening browser...";
                    });
                }
                
                // Print code to console for easy access
                System.out.println("========================================");
                System.out.println("[VanillaSploit] MICROSOFT LOGIN CODE: " + deviceCode.userCode());
                System.out.println("[VanillaSploit] Enter at: " + deviceCode.verificationUri());
                System.out.println("========================================");
                
                // Open the URL in browser
                Util.getOperatingSystem().open(deviceCode.verificationUri());
                
                // Update status
                if (this.client != null) {
                    this.client.execute(() -> {
                        msAuthStatus = "Code: §e§l" + msAuthCode + "§r - Enter at §9microsoft.com/link";
                    });
                }
                
                // Start polling (blocking)
                MicrosoftAuth.AuthResult result = MicrosoftAuth.pollForTokenSync(deviceCode, status -> {
                    if (this.client != null) {
                        this.client.execute(() -> {
                            msAuthStatus = status;
                        });
                    }
                });
                
                // Handle result on main thread
                if (this.client != null) {
                    this.client.execute(() -> {
                        msAuthInProgress = false;
                        msLoginButton.active = true;
                        
                        if (result != null && result.isSuccess()) {
                            // Add the account
                            Account account = Account.microsoft(
                                result.username(),
                                result.uuid(),
                                result.accessToken(),
                                result.xuid(),
                                result.clientId()
                            );
                            AccountManager.getInstance().addAccount(account);
                            msAuthStatus = "§aAdded: " + result.username();
                            refreshAccountList();
                        } else {
                            msAuthStatus = "§c" + (result != null ? result.error() : "Unknown error");
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (this.client != null) {
                    this.client.execute(() -> {
                        msAuthInProgress = false;
                        msAuthStatus = "§cError: " + e.getMessage();
                        msLoginButton.active = true;
                    });
                }
            }
        }, "MS-Auth-Thread").start();
    }
    
    private void refreshAccountList() {
        // Remove old account buttons
        for (ButtonWidget btn : accountButtons) {
            this.remove(btn);
        }
        accountButtons.clear();
        
        int centerX = this.width / 2;
        int listWidth = 220;
        int listX = centerX - listWidth / 2;
        int listY = 100;
        int entryHeight = 22;
        int maxVisible = 6;
        
        var accounts = AccountManager.getInstance().getAccounts();
        for (int i = 0; i < Math.min(accounts.size(), maxVisible); i++) {
            int index = i + scrollOffset;
            if (index >= accounts.size()) break;
            
            Account account = accounts.get(index);
            int entryY = listY + i * entryHeight;
            
            String label = account.username();
            
            // Show account type
            if (account.isMicrosoft()) {
                label = "§b⬢ §f" + label;
            } else {
                label = "§7⬡ §f" + label;
            }
            
            Account current = AccountManager.getInstance().getCurrentAccount();
            if (current != null && current.username().equals(account.username())) {
                label += " §a[Active]";
            }
            
            final Account accountRef = account;
            ButtonWidget btn = ButtonWidget.builder(Text.literal(label), b -> {
                selectedAccount = accountRef;
                updateButtonHighlights();
            }).dimensions(listX, entryY, listWidth, 20).build();
            
            accountButtons.add(btn);
            this.addDrawableChild(btn);
        }
        
        updateButtonHighlights();
    }
    
    private void updateButtonHighlights() {
        // Update button appearances based on selection
        var accounts = AccountManager.getInstance().getAccounts();
        for (int i = 0; i < accountButtons.size(); i++) {
            int index = i + scrollOffset;
            if (index >= accounts.size()) break;
            
            Account account = accounts.get(index);
            ButtonWidget btn = accountButtons.get(i);
            
            String label = account.username();
            
            // Show account type
            if (account.isMicrosoft()) {
                label = "§b⬢ §f" + label;
            } else {
                label = "§7⬡ §f" + label;
            }
            
            Account current = AccountManager.getInstance().getCurrentAccount();
            if (current != null && current.username().equals(account.username())) {
                label += " §a[Active]";
            }
            if (account.equals(selectedAccount)) {
                label = "§e> " + label + " §e<";
            }
            
            btn.setMessage(Text.literal(label));
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Call super first to render background only once
        super.render(context, mouseX, mouseY, delta);
        
        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Subtitle
        context.drawCenteredTextWithShadow(this.textRenderer, "§7⬢ = Microsoft  §7⬡ = Cracked", this.width / 2, 32, 0xAAAAAA);
        
        int centerX = this.width / 2;
        
        // Show Microsoft auth status
        if (!msAuthStatus.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, msAuthStatus, centerX, this.height - 92, 0xFFFFFF);
        }
        
        // Show current session info
        if (this.client != null && this.client.getSession() != null) {
            String currentUser = "Current: " + this.client.getSession().getUsername();
            context.drawCenteredTextWithShadow(this.textRenderer, currentUser, centerX, this.height - 78, 0xAAAAAA);
        }
        
        // Instructions
        var accounts = AccountManager.getInstance().getAccounts();
        if (accounts.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, "No accounts added yet", centerX, 140, 0x888888);
            context.drawCenteredTextWithShadow(this.textRenderer, "Add a cracked username above or", centerX, 155, 0x666666);
            context.drawCenteredTextWithShadow(this.textRenderer, "click Microsoft Account to login", centerX, 170, 0x666666);
        }
    }
    
    private void loginAs(Account account) {
        if (this.client == null) return;
        
        try {
            Session newSession;
            
            if (account.isMicrosoft()) {
                // Microsoft account - use real credentials
                newSession = new Session(
                    account.username(),
                    account.uuid(),
                    account.accessToken(),
                    Optional.ofNullable(account.xuid()),
                    Optional.ofNullable(account.clientId())
                );
            } else {
                // Cracked account - offline mode
                newSession = new Session(
                    account.username(),
                    account.uuid(),
                    "",  // Empty access token for offline
                    Optional.empty(),
                    Optional.empty()
                );
            }
            
            // Use mixin accessor to set session
            ((SessionAccessor) this.client).setSession(newSession);
            AccountManager.getInstance().setCurrentAccount(account);
            
            // Go back to title screen
            this.client.setScreen(new TitleScreen());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}
