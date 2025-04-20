package me.matiego.counting.minecraft;

import me.matiego.counting.Main;
import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class McApiRequests {
    public McApiRequests(@NotNull Main instance) {
        this.instance = instance;
    }

    private final Main instance;
    private HttpClient client;

    public void initiateHttpClient() {
        closeHttpClient();

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);

        client = HttpClient.newBuilder()
                .cookieHandler(CookieHandler.getDefault())
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    public void closeHttpClient() {
        if (client == null) return;
        client.close();
        client = null;
    }

    public @NotNull UUID getUuidByCode(@NotNull String code) throws McException {
        if (!instance.getConfig().getBoolean("minecraft.enabled")) {
            throw new McException("Łączenie kont jest wyłączone. Spróbuj ponownie później");
        }
        try {
            HttpRequest.Builder builder;
            try {
                builder = HttpRequest.newBuilder()
                        .uri(getURI("link"));
            } catch (IllegalArgumentException e) {
                Logs.warning("Incorrect Minecraft api URL", e);
                throw new McException("Błędna konfiguracja bota. Skontaktuj się z administratorem.");
            }

            HttpRequest request = builder
                    .header("User-Agent", "Counting-Bot (" + Utils.getVersion() + ")")
                    .header("key", String.valueOf(instance.getConfig().getString("minecraft.api.key")))
                    .POST(HttpRequest.BodyPublishers.ofString("code=" + code))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                Logs.warning("Minecraft api key is incorrect");
                throw new McException("Błędna konfiguracja bota. Skontaktuj się z administratorem.");
            }
            if (response.statusCode() == 404) {
                throw new McException("Błędny kod weryfikacji. Czy wygenerowałeś swój kod używając komendy `/linkdiscord` na serwerze Minecraft?");
            }
            if (response.statusCode() == 408) {
                throw new McException("Błędny kod weryfikacji. Pamiętaj, że kod weryfikacji jest ważny tylko 5 minut.");
            }
            if (response.statusCode() == 200) {
                return UUID.fromString(new JSONObject(response.body()).getString("uuid"));
            }

            String error = "<unknown>";
            try {
                error = new JSONObject(response.body()).getString("title");
            } catch (Exception ignored) {}
            throw new Exception("Unhandled status code: " + response.statusCode() + "; Error message: " + error);
        } catch (McException e) {
            throw e;
        } catch (Exception e) {
            Logs.error("Failed to get uuid by code", e);
            throw new McException("Napotkano niespodziewany błąd. Spróbuj ponownie później.");
        }
    }

    private @NotNull URI getURI(@NotNull String route) throws McException {
        String uri = instance.getConfig().getString("minecraft.api.url");
        if (uri == null) {
            Logs.warning("Minecraft api URL is null");
            throw new McException("Błędna konfiguracja bota. Skontaktuj się z administratorem.");
        }

        try {
            return URI.create(uri + "/" + route);
        } catch (Exception e) {
            Logs.warning("Failed to get URL of Minecraft api", e);
            throw new McException("Błędna konfiguracja bota. Skontaktuj się z administratorem.");
        }
    }
}
