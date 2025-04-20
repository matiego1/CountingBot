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
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(getURI("link"))
                    .header("User-Agent", "Counting-Bot (" + Utils.getVersion() + ")")
                    .POST(HttpRequest.BodyPublishers.ofString(String.format("{\"code\": \"%s\"}", code)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                throw new McException("Błędny kod weryfikacji. Czy wygenerowałeś swój kod używając komendy `/linkdiscord` na serwerze Minecraft?");
            }
            if (response.statusCode() == 408) {
                throw new McException("Błędny kod weryfikacji. Pamiętaj, że kod weryfikacji jest ważny tylko 5 minut.");
            }
            if (response.statusCode() == 200) {
                return UUID.fromString(new JSONObject(response.body()).getString("uuid"));
            }
            throw new Exception("Unhandled status code: " + response.statusCode());
        } catch (Exception e) {
            Logs.error("Failed to get uuid by code", e);
            throw new McException("Napotkano niespodziewany błąd. Spróbuj ponownie później.");
        }
    }

    private @NotNull URI getURI(@NotNull String route) throws McException {
        String uri = instance.getConfig().getString("minecraft.api-url");
        if (uri == null) {
            throw new McException("Błędna konfiguracja bota. Skontaktuj się z administratorem.");
        }

        try {
            return URI.create(uri + "/" + route);
        } catch (Exception e) {
            Logs.warning("Failed to get URI of Minecraft API", e);
            throw new McException("Błędna konfiguracja bota. Skontaktuj się z administratorem.");
        }
    }
}
