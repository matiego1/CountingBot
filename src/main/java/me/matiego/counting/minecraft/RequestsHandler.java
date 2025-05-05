package me.matiego.counting.minecraft;

import io.javalin.websocket.WsCloseStatus;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import lombok.Getter;
import me.matiego.counting.Main;
import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RequestsHandler {
    public RequestsHandler(@NotNull Main instance) {
        this.instance = instance;
    }

    private final static int UNAUTHORIZED_CODE = 3000;
    private final static int TIMEOUT_SECONDS = 5;
    private final Main instance;
    private final Map<WsContext, String> users = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Response>> requests = Collections.synchronizedMap(Utils.createLimitedSizeMap(10_000));

    public void handle(@NotNull WsConfig ws) {
        ws.onConnect(ctx -> {
            if (!checkAuth(ctx)) return;

            String sessionHash = getSessionHash(ctx);
            users.put(ctx, sessionHash);
            Logs.info("User (" + sessionHash + ") has connected to the rewards API");
        });
        ws.onClose(ctx -> {
            if (!checkAuth(ctx)) return;

            String sessionHash = users.remove(ctx);
            Logs.info("User (" + sessionHash + ") has disconnected from the rewards API. Close status: " + ctx.closeStatus());
        });
        ws.onError(ctx -> {
            if (!checkAuth(ctx)) return;

            String sessionHash = users.get(ctx);
            Throwable e = ctx.error();
            Logs.warning("User (" + sessionHash + ") has encountered an error" + (e == null ? "" : ": " + e.getClass().getSimpleName() + ": " + e.getMessage()));
        });
        ws.onMessage(ctx -> {
            if (!checkAuth(ctx)) return;

            String message = ctx.message().trim();
            if (message.equals("ping")) return;

            Logs.debug("Received WebSocket message from " + getSessionHash(ctx) + ": `" + message + "`");

            try {
                parseMessage(message);
            } catch (Exception e) {
                Logs.error("Failed to parse a received WebSocket message", e);
            }
        });
    }

    private @NotNull String getSessionHash(@NotNull WsContext ctx) {
        return String.format("%s-%s", ctx.session.getRemoteAddress(), ctx.sessionId());
    }

    private boolean checkAuth(WsContext ctx) {
        if (instance.getWebServer().checkAuthCookie(ctx)) return true;
        ctx.closeSession(UNAUTHORIZED_CODE, "Unauthorized key, reference the key existing in config.yml");
        return false;
    }


    private void parseMessage(@NotNull String message) {
        JSONObject json = new JSONObject(message);

        String idString = json.getString("id");
        if (idString.equals("null")) {
            Logs.error("Minecraft server couldn't handle received message and responded with: " + message);
            return;
        }
        UUID id = UUID.fromString(idString);

        CompletableFuture<Response> completable = requests.remove(id);
        if (completable == null) {
            Logs.error("Failed to get a CompletableFuture by id: " + id);
            return;
        }

        Response response = new Response(
                json.getInt("status-code"),
                json.getString("message")
        );
        completable.complete(response);
    }

    public @NotNull CompletableFuture<Response> sendRequest(@NotNull String path, @NotNull JSONObject params) {
        CompletableFuture<Response> response = new CompletableFuture<>();
        response.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        UUID id = UUID.randomUUID();
        while (requests.putIfAbsent(id, response) != null) {
            id = UUID.randomUUID();
        }

        JSONObject message = new JSONObject();
        message.put("id", id.toString());
        message.put("path", path);
        message.put("params", params);

        Logs.debug("Sending WebSocket message: " + message);

        users.keySet().stream()
                .filter(ctx -> ctx.session.isOpen())
                .forEach(ctx -> ctx.send(message.toString()));
        return response;
    }

    public void closeSessions() {
        users.keySet().forEach(ctx -> ctx.closeSession(WsCloseStatus.NORMAL_CLOSURE));
    }

    @Getter
    public static class Response {
        public Response(int statusCode, @NotNull String message) {
            this.statusCode = statusCode;
            this.message = message;
        }

        private final int statusCode;
        private final String message;
    }
}
