package com.jadxv.vanillasploit;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Microsoft Authentication for Minecraft
 * Uses device code flow for authentication
 */
public class MicrosoftAuth {
    // Using Prism Launcher's approved MSA Client ID (GPL-3.0 open source)
    // Custom app registrations won't work - Minecraft API requires pre-approved apps
    private static final String CLIENT_ID = "c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb";
    private static final String SCOPE = "XboxLive.signin offline_access";
    
    private static final String DEVICE_CODE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    private static final String TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MC_AUTH_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";
    private static final String MC_ENTITLEMENTS_URL = "https://api.minecraftservices.com/entitlements/mcstore";
    
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private static final Gson gson = new Gson();
    
    public record DeviceCodeResponse(
        String userCode,
        String deviceCode,
        String verificationUri,
        int expiresIn,
        int interval
    ) {}
    
    public record AuthResult(
        String username,
        UUID uuid,
        String accessToken,
        String xuid,
        String clientId,
        String error
    ) {
        public boolean isSuccess() {
            return error == null;
        }
        
        public static AuthResult error(String error) {
            return new AuthResult(null, null, null, null, null, error);
        }
        
        public static AuthResult success(String username, UUID uuid, String accessToken, String xuid, String clientId) {
            return new AuthResult(username, uuid, accessToken, xuid, clientId, null);
        }
    }
    
    /**
     * Start device code flow - returns code for user to enter at microsoft.com/link
     */
    public static CompletableFuture<DeviceCodeResponse> startDeviceCodeFlow() {
        return CompletableFuture.supplyAsync(MicrosoftAuth::startDeviceCodeFlowSync);
    }
    
    /**
     * Synchronous version of startDeviceCodeFlow
     */
    public static DeviceCodeResponse startDeviceCodeFlowSync() {
        try {
            String body = "client_id=" + CLIENT_ID + "&scope=" + SCOPE.replace(" ", "%20");
            System.out.println("[VanillaSploit] MS Auth starting device code flow...");
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DEVICE_CODE_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                return new DeviceCodeResponse(
                    json.get("user_code").getAsString(),
                    json.get("device_code").getAsString(),
                    json.get("verification_uri").getAsString(),
                    json.get("expires_in").getAsInt(),
                    json.get("interval").getAsInt()
                );
            } else {
                System.err.println("[VanillaSploit] MS Auth failed: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("[VanillaSploit] MS Auth exception: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Poll for device code completion and complete the full auth flow
     */
    public static CompletableFuture<AuthResult> pollForToken(DeviceCodeResponse deviceCode, Consumer<String> statusCallback) {
        return CompletableFuture.supplyAsync(() -> pollForTokenSync(deviceCode, statusCallback));
    }
    
    /**
     * Synchronous version of pollForToken
     */
    public static AuthResult pollForTokenSync(DeviceCodeResponse deviceCode, Consumer<String> statusCallback) {
        try {
            // Poll for MS token
            statusCallback.accept("Waiting for login...");
            System.out.println("[VanillaSploit] Polling for MS token...");
            String msToken = pollForMsToken(deviceCode);
            if (msToken == null) {
                System.err.println("[VanillaSploit] MS token is null!");
                return AuthResult.error("Login timed out or was cancelled");
            }
            System.out.println("[VanillaSploit] Got MS token!");
            
            // Xbox Live auth
            statusCallback.accept("Authenticating with Xbox Live...");
            System.out.println("[VanillaSploit] Authenticating with Xbox Live...");
            XboxAuthResult xboxResult = authenticateXbox(msToken);
            if (xboxResult == null) {
                System.err.println("[VanillaSploit] Xbox auth failed!");
                return AuthResult.error("Xbox Live authentication failed");
            }
            System.out.println("[VanillaSploit] Xbox auth success!");
            
            // XSTS token
            statusCallback.accept("Getting XSTS token...");
            System.out.println("[VanillaSploit] Getting XSTS token...");
            XboxAuthResult xstsResult = getXstsToken(xboxResult.token);
            if (xstsResult == null) {
                System.err.println("[VanillaSploit] XSTS token failed!");
                return AuthResult.error("XSTS token failed - check account status");
            }
            System.out.println("[VanillaSploit] XSTS token success!");
            
            // Minecraft auth
            statusCallback.accept("Authenticating with Minecraft...");
            System.out.println("[VanillaSploit] Authenticating with Minecraft...");
            McAuthResult mcResult = authenticateMinecraft(xstsResult.userHash, xstsResult.token);
            if (mcResult == null) {
                System.err.println("[VanillaSploit] MC auth failed!");
                return AuthResult.error("Minecraft authentication failed");
            }
            System.out.println("[VanillaSploit] MC auth success!");
            
            // Check ownership
            statusCallback.accept("Checking game ownership...");
            System.out.println("[VanillaSploit] Checking game ownership...");
            if (!checkGameOwnership(mcResult.accessToken)) {
                System.err.println("[VanillaSploit] No game ownership!");
                return AuthResult.error("Account doesn't own Minecraft");
            }
            System.out.println("[VanillaSploit] Game ownership confirmed!");
            
            // Get profile
            statusCallback.accept("Getting profile...");
            System.out.println("[VanillaSploit] Getting profile...");
            McProfile profile = getProfile(mcResult.accessToken);
            if (profile == null) {
                System.err.println("[VanillaSploit] Failed to get profile!");
                return AuthResult.error("Failed to get profile - try logging into Minecraft launcher first");
            }
            System.out.println("[VanillaSploit] Profile: " + profile.username + " / " + profile.uuid);
            
            statusCallback.accept("Success!");
            return AuthResult.success(
                profile.username,
                profile.uuid,
                mcResult.accessToken,
                xstsResult.userHash,
                null
            );
            
        } catch (Exception e) {
            e.printStackTrace();
            return AuthResult.error("Error: " + e.getMessage());
        }
    }
    
    private static String pollForMsToken(DeviceCodeResponse deviceCode) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long timeout = deviceCode.expiresIn * 1000L;
        int interval = Math.max(deviceCode.interval, 5) * 1000;
        
        while (System.currentTimeMillis() - startTime < timeout) {
            Thread.sleep(interval);
            
            try {
                String body = "client_id=" + CLIENT_ID +
                        "&device_code=" + deviceCode.deviceCode +
                        "&grant_type=urn:ietf:params:oauth:grant-type:device_code";
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(TOKEN_URL))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                
                if (response.statusCode() == 200) {
                    return json.get("access_token").getAsString();
                }
                
                // Check for errors
                if (json.has("error")) {
                    String error = json.get("error").getAsString();
                    if (error.equals("authorization_pending")) {
                        continue; // Keep polling
                    } else if (error.equals("authorization_declined") || error.equals("expired_token")) {
                        return null;
                    }
                }
            } catch (IOException e) {
                // Network error, continue polling
            }
        }
        return null;
    }
    
    private record XboxAuthResult(String token, String userHash) {}
    
    private static XboxAuthResult authenticateXbox(String msToken) {
        try {
            JsonObject properties = new JsonObject();
            properties.addProperty("AuthMethod", "RPS");
            properties.addProperty("SiteName", "user.auth.xboxlive.com");
            properties.addProperty("RpsTicket", "d=" + msToken);
            
            JsonObject body = new JsonObject();
            body.add("Properties", properties);
            body.addProperty("RelyingParty", "http://auth.xboxlive.com");
            body.addProperty("TokenType", "JWT");
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(XBOX_AUTH_URL))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String token = json.get("Token").getAsString();
                String userHash = json.getAsJsonObject("DisplayClaims")
                        .getAsJsonArray("xui")
                        .get(0).getAsJsonObject()
                        .get("uhs").getAsString();
                return new XboxAuthResult(token, userHash);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private static XboxAuthResult getXstsToken(String xblToken) {
        try {
            JsonObject properties = new JsonObject();
            properties.addProperty("SandboxId", "RETAIL");
            JsonArray userTokens = new JsonArray();
            userTokens.add(xblToken);
            properties.add("UserTokens", userTokens);
            
            JsonObject body = new JsonObject();
            body.add("Properties", properties);
            body.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
            body.addProperty("TokenType", "JWT");
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(XSTS_AUTH_URL))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String token = json.get("Token").getAsString();
                String userHash = json.getAsJsonObject("DisplayClaims")
                        .getAsJsonArray("xui")
                        .get(0).getAsJsonObject()
                        .get("uhs").getAsString();
                return new XboxAuthResult(token, userHash);
            } else if (response.statusCode() == 401) {
                // Check for XErr codes
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                if (json.has("XErr")) {
                    long xErr = json.get("XErr").getAsLong();
                    System.err.println("XSTS Error: " + xErr);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private record McAuthResult(String accessToken, String username) {}
    
    private static McAuthResult authenticateMinecraft(String userHash, String xstsToken) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MC_AUTH_URL))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[VanillaSploit] MC Auth response code: " + response.statusCode());
            System.out.println("[VanillaSploit] MC Auth response: " + response.body());
            
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                return new McAuthResult(
                    json.get("access_token").getAsString(),
                    json.get("username").getAsString()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private static boolean checkGameOwnership(String mcToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MC_ENTITLEMENTS_URL))
                    .header("Authorization", "Bearer " + mcToken)
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray items = json.getAsJsonArray("items");
                
                // Check if they have Minecraft (either owned or via Game Pass)
                for (int i = 0; i < items.size(); i++) {
                    String name = items.get(i).getAsJsonObject().get("name").getAsString();
                    if (name.equals("game_minecraft") || 
                        name.equals("product_minecraft") ||
                        name.equals("product_game_pass_pc") ||
                        name.equals("product_game_pass_ultimate")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private record McProfile(String username, UUID uuid) {}
    
    private static McProfile getProfile(String mcToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MC_PROFILE_URL))
                    .header("Authorization", "Bearer " + mcToken)
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String username = json.get("name").getAsString();
                String uuidStr = json.get("id").getAsString();
                
                // Parse UUID (it comes without dashes)
                UUID uuid = UUID.fromString(
                    uuidStr.substring(0, 8) + "-" +
                    uuidStr.substring(8, 12) + "-" +
                    uuidStr.substring(12, 16) + "-" +
                    uuidStr.substring(16, 20) + "-" +
                    uuidStr.substring(20)
                );
                
                return new McProfile(username, uuid);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
