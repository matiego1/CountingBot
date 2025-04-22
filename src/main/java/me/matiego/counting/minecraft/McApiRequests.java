package me.matiego.counting.minecraft;

import me.matiego.counting.Main;
import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
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

        SSLContext sslContext = getSSLContext();

        HttpClient.Builder builder = HttpClient.newBuilder()
                .cookieHandler(CookieHandler.getDefault())
                .followRedirects(HttpClient.Redirect.ALWAYS);
        if (sslContext != null) {
            builder.sslContext(sslContext);
        }
        client = builder.build();
    }

    public @Nullable SSLContext getSSLContext() {
        // TODO: accept only known certificate
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (Exception e) {
            Logs.error("Failed to get SSL context", e);
        }
        return null;
    }

    public void closeHttpClient() {
        if (client == null) return;
        client.close();
        client = null;
    }

    public boolean isEnabled() {
        return instance.getConfig().getBoolean("minecraft.enabled");
    }

    public @NotNull UUID getUuidByCode(@NotNull String code) throws McException {
        if (!isEnabled()) {
            throw new McException("Łączenie kont jest wyłączone. Spróbuj ponownie później.");
        }
        try {
            HttpRequest.Builder builder;
            try {
                builder = HttpRequest.newBuilder()
                        .uri(getURI("link"));
            } catch (IllegalArgumentException e) {
                Logs.error("Incorrect Counting-MC API URL", e);
                throw new McException("Błędna konfiguracja bota. Zgłoś ten błąd administratorowi.");
            }

            String params = "code=" + code;

            HttpRequest request = builder
                    .header("User-Agent", "Counting-Bot (" + Utils.getVersion() + ")")
                    .header("key", String.valueOf(instance.getConfig().getString("minecraft.api.key")))
                    .POST(HttpRequest.BodyPublishers.ofString(params))
                    .build();

            HttpResponse<String> response;
            try {
                Logs.debug("Sending request to CountingBot-MC API: /link " + params);
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (ConnectException e) {
                Logs.error("Failed to get uuid by code", e);
                throw new McException("Napotkano niespodziewany błąd. Czy serwer Minecraft jest online?");
            }

            if (response.statusCode() == 400) {
                Logs.error("Failed to get uuid by code: " + getErrorTitle(response.body()));
                throw new McException("Napotkano niespodziewany błąd. Spróbuj ponownie później.");
            }
            if (response.statusCode() == 401) {
                Logs.error("Counting-MC API key is incorrect");
                throw new McException("Błędna konfiguracja bota. Zgłoś ten błąd administratorowi.");
            }
            if (response.statusCode() == 404) {
                throw new McException("Błędny kod weryfikacji. Czy wygenerowałeś swój kod używając komendy `/linkdiscord` na serwerze Minecraft?");
            }
            if (response.statusCode() == 408) {
                throw new McException("Błędny kod weryfikacji. Pamiętaj, że kod weryfikacji jest ważny tylko 5 minut.");
            }
            if (response.statusCode() == 200) {
                return UUID.fromString(getJsonValue(response.body(), "uuid"));
            }

            throw new IllegalStateException("Unhandled status code: " + response.statusCode() + "; Error message: " + getErrorTitle(response.body()));
        } catch (McException e) {
            throw e;
        } catch (Exception e) {
            Logs.error("Failed to get uuid by code", e);
            throw new McException("Napotkano niespodziewany błąd. Spróbuj ponownie później.");
        }
    }

    public void giveReward(@NotNull UUID uuid, double amount) throws McException {
        if (!isEnabled()) {
            throw new McException("Nagrody za liczenie są wyłączone.");
        }
        try {
            HttpRequest.Builder builder;
            try {
                builder = HttpRequest.newBuilder()
                        .uri(getURI("deposit"));
            } catch (IllegalArgumentException e) {
                Logs.error("Incorrect Counting-MC API URL", e);
                throw new McException("Błędna konfiguracja bota. Zgłoś ten błąd administratorowi.");
            }

            String params = "uuid=" + uuid + "&amount=" + amount;

            HttpRequest request = builder
                    .header("User-Agent", "Counting-Bot (" + Utils.getVersion() + ")")
                    .header("key", String.valueOf(instance.getConfig().getString("minecraft.api.key")))
                    .POST(HttpRequest.BodyPublishers.ofString(params))
                    .build();

            HttpResponse<String> response;
            try {
                Logs.debug("Sending request to CountingBot-MC API: /deposit " + params);
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (ConnectException e) {
                Logs.error("Failed to give a reward for counting", e);
                throw new McException("Napotkano niespodziewany błąd. Czy serwer Minecraft jest online?");
            }

            if (response.statusCode() == 200) return;
            if (response.statusCode() == 400) {
                Logs.error("Failed to give a reward for counting: " + getErrorTitle(response.body()));
                throw new McException("Napotkano niespodziewany błąd.");
            }
            if (response.statusCode() == 401) {
                Logs.error("Counting-MC API key is incorrect");
                throw new McException("Błędna konfiguracja bota. Zgłoś ten błąd administratorowi.");
            }
            if (response.statusCode() == 403) {
                throw new McException("Nagrody za liczenie zostały wyłączone przez administratora serwera Minecraft. Jeżeli nie chcesz otrzymywać tej wiadomości, rozłącz swoje konto Minecraft używając komendy /minecraft unlink.");
            }
            if (response.statusCode() == 404) {
                throw new McException("Serwer Minecraft nie rozpoznał twojego konta Minecraft. Połącz je ponownie.");
            }
            if (response.statusCode() == 500) {
                throw new McException("Serwer Minecraft napotkał błąd przy dawaniu Ci pieniędzy. Zgłoś ten błąd administratorowi.");
            }
            if (response.statusCode() == 503) {
                throw new McException("Błędna konfiguracja serwera Minecraft. Zgłoś ten błąd administratorowi.");
            }

            throw new IllegalStateException("Unhandled status code: " + response.statusCode() + "; Error message: " + getErrorTitle(response.body()));
        } catch (McException e) {
            throw e;
        } catch (Exception e) {
            Logs.error("Failed to give a reward for counting", e);
            throw new McException("Napotkano niespodziewany błąd.");
        }
    }

    private @NotNull URI getURI(@NotNull String route) throws McException {
        String uri = instance.getConfig().getString("minecraft.api.url");
        if (uri == null) {
            Logs.warning("Counting-MC API URL is not set");
            throw new McException("Błędna konfiguracja bota. Zgłoś ten błąd administratorowi.");
        }

        try {
            return URI.create(uri + "/" + route);
        } catch (Exception e) {
            Logs.warning("Failed to get URL of Counting-MC API", e);
            throw new McException("Błędna konfiguracja bota. Zgłoś ten błąd administratorowi.");
        }
    }

    private @NotNull String getJsonValue(@NotNull String body, @NotNull String value) {
        return new JSONObject(body).getString(value);
    }

    private @NotNull String getErrorTitle(@NotNull String body) {
        try {
            return getJsonValue(body, "title");
        } catch (Exception ignored) {}
        return body;
    }
}
