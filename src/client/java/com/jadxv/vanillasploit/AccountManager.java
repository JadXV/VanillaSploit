package com.jadxv.vanillasploit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccountManager {
    private static AccountManager INSTANCE;
    private final List<Account> accounts = new ArrayList<>();
    private Account currentAccount = null;
    
    public static AccountManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AccountManager();
        }
        return INSTANCE;
    }
    
    public void addAccount(Account account) {
        // Don't add duplicates
        for (Account a : accounts) {
            if (a.username().equalsIgnoreCase(account.username())) {
                return;
            }
        }
        accounts.add(account);
    }
    
    public void removeAccount(Account account) {
        accounts.remove(account);
    }
    
    public List<Account> getAccounts() {
        return accounts;
    }
    
    public Account getCurrentAccount() {
        return currentAccount;
    }
    
    public void setCurrentAccount(Account account) {
        this.currentAccount = account;
    }
    
    public record Account(
        String username,
        UUID uuid,
        String accessToken,
        AccountType type,
        String xuid,
        String clientId
    ) {
        public static Account cracked(String username) {
            UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
            return new Account(username, offlineUuid, "", AccountType.CRACKED, null, null);
        }
        
        public static Account microsoft(String username, UUID uuid, String accessToken, String xuid, String clientId) {
            return new Account(username, uuid, accessToken, AccountType.MICROSOFT, xuid, clientId);
        }
        
        public boolean isCracked() {
            return type == AccountType.CRACKED;
        }
        
        public boolean isMicrosoft() {
            return type == AccountType.MICROSOFT;
        }
    }
    
    public enum AccountType {
        CRACKED,
        MICROSOFT
    }
}
