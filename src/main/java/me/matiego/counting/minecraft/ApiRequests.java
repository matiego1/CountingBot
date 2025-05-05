package me.matiego.counting.minecraft;

import me.matiego.counting.Main;
import me.matiego.counting.utils.Logs;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

public class ApiRequests {
    public ApiRequests(@NotNull Main instance) {
        this.instance = instance;
    }

    private final Main instance;

    public boolean isEnabled() {
        return instance.getConfig().getBoolean("minecraft.enabled");
    }

    public @NotNull CompletableFuture<UUID> getUuidByCode(@NotNull String code) {
        CompletableFuture<UUID> result = new CompletableFuture<>();
        if (!isEnabled()) {
            result.completeExceptionally(new McException("Łączenie kont jest wyłączone. Spróbuj ponownie później."));
            return result;
        }
        try {
            JSONObject params = new JSONObject();
            params.put("code", code);

            instance.getRequestsHandler().sendRequest("link", params)
                    .whenComplete((response, e) -> {
                        if (response != null) {
                            handleLinkResponse(response, result);
                        } else {
                            handleException(e, result);
                        }
                    });
        } catch (Exception e) {
            Logs.error("Failed to get uuid by code", e);
            result.completeExceptionally(new McException("Napotkano niespodziewany błąd. Spróbuj ponownie później."));
        }
        return result;
    }

    private void handleLinkResponse(@NotNull RequestsHandler.Response response, @NotNull CompletableFuture<UUID> result) {
        McException exception = switch (response.getStatusCode()) {
            case 400 -> {
                Logs.error("Failed to get uuid by code: " + response.getMessage());
                yield new McException("Napotkano niespodziewany błąd. Spróbuj ponownie później.");
            }
            case 404 ->
                    new McException("Błędny kod weryfikacji. Czy wygenerowałeś swój kod używając komendy `/linkdiscord` na serwerze Minecraft?");
            case 408 ->
                    new McException("Błędny kod weryfikacji. Pamiętaj, że kod weryfikacji jest ważny tylko 5 minut.");
            case 200 -> null;
            default -> {
                Logs.error("Unhandled status code: " + response.getStatusCode() + "; Message: " + response.getMessage());
                yield new McException("Napotkano niespodziewany błąd. Spróbuj ponownie później.");
            }
        };
        if (exception != null) {
            result.completeExceptionally(exception);
            return;
        }

        try {
            result.complete(UUID.fromString(new JSONObject(response.getMessage()).getString("uuid")));
        } catch (Exception e) {
            Logs.error("Failed to get uuid by code: " + response.getMessage(), e);
            result.completeExceptionally(new McException("Napotkano niespodziewany błąd. Spróbuj ponownie później."));
        }
    }

    public @NotNull CompletableFuture<Void> giveReward(@NotNull UUID uuid, double amount) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        if (!isEnabled()) {
            result.completeExceptionally(new McException("Nagrody za liczenie są wyłączone."));
            return result;
        }
        try {
            JSONObject params = new JSONObject();
            params.put("uuid", uuid.toString());
            params.put("amount", String.valueOf(amount));

            instance.getRequestsHandler().sendRequest("deposit", params)
                    .whenComplete((response, e) -> {
                        if (response != null) {
                            handleDepositResponse(response, result);
                        } else {
                            handleException(e, result);
                        }
                    });
        } catch (Exception e) {
            Logs.error("Failed to give a reward for counting", e);
            result.completeExceptionally(new McException("Napotkano niespodziewany błąd."));
        }
        return result;
    }

    private void handleDepositResponse(@NotNull RequestsHandler.Response response, @NotNull CompletableFuture<Void> result) {
        McException exception = switch (response.getStatusCode()) {
            case 400 -> {
                Logs.error("Failed to give a reward for counting: " + response.getMessage());
                yield new McException("Napotkano niespodziewany błąd. Spróbuj ponownie później.");
            }
            case 200 -> null;
            case 403 ->
                    new McException("Nagrody za liczenie zostały wyłączone przez administratora serwera Minecraft. Jeżeli nie chcesz otrzymywać tej wiadomości, rozłącz swoje konto Minecraft używając komendy /minecraft unlink.");
            case 404 ->
                    new McException("Serwer Minecraft nie rozpoznał twojego konta Minecraft. Połącz je ponownie.");
            case 500 ->
                    new McException("Serwer Minecraft napotkał błąd przy dawaniu Ci pieniędzy. Zgłoś ten błąd administratorowi.");
            case 503 ->
                    new McException("Błędna konfiguracja serwera Minecraft. Zgłoś ten błąd administratorowi.");
            default -> {
                Logs.error("Unhandled status code: " + response.getStatusCode() + "; Message: " + response.getMessage());
                yield new McException("Napotkano niespodziewany błąd.");
            }
        };
        if (exception != null) {
            result.completeExceptionally(exception);
        } else {
            result.complete(null);
        }
    }

    private void handleException(@NotNull Throwable e, @NotNull CompletableFuture<?> result) {
        if (e instanceof TimeoutException) {
            result.completeExceptionally(new McException("Nie udało się połączyć do serwera Minecraft. Czy jest on online?"));
        }
        Logs.error("Failed to get uuid by code", e);
        result.completeExceptionally(new McException("Napotkano niespodziewany błąd."));
    }
}
