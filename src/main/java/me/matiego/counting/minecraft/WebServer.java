package me.matiego.counting.minecraft;

import io.javalin.Javalin;
import io.javalin.community.ssl.SslPlugin;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinGson;
import io.javalin.websocket.WsContext;
import me.matiego.counting.Main;
import me.matiego.counting.utils.Logs;
import org.jetbrains.annotations.NotNull;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

// Based on https://github.com/servertap-io/servertap
public class WebServer {
    public WebServer(@NotNull Main instance, @NotNull YamlFile config) {
        this.instance = instance;

        this.tlsEnabled = config.getBoolean("minecraft.api.tls.enabled", false);
        this.sni = config.getBoolean("minecraft.api.tls.sni", false);
        this.keyStorePath = config.getString("minecraft.api.tls.keystore", "keystore.jks");
        this.keyStorePassword = config.getString("minecraft.api.tls.keystorePassword", "");
        this.authKey = config.getString("minecraft.api.key", "change_me");
        this.corsOrigin = config.getStringList("minecraft.api.corsOrigins");
        this.port = config.getInt("minecraft.api.port", 4567);

        javalin = Javalin.create(this::configureJavalin);
    }

    private static final String KEY_COOKIE = "x-counting-key";

    private final Main instance;
    private final Javalin javalin;

    private final boolean tlsEnabled;
    private final boolean sni;
    private final String keyStorePath;
    private final String keyStorePassword;
    private final String authKey;
    private final List<String> corsOrigin;
    private final int port;

    private void configureJavalin(@NotNull JavalinConfig config) {
        config.jsonMapper(new JavalinGson());
        config.http.defaultContentType = "application/json";
        config.showJavalinBanner = false;

        configureTLS(config);
        configureCors(config);

        if ("change_me".equalsIgnoreCase(authKey)) {
            Logs.error("AUTH KEY IS SET TO DEFAULT \"change_me\"");
            Logs.error("CHANGE THE key IN THE config.yml FILE");
            Logs.error("FAILURE TO CHANGE THE KEY MAY RESULT IN SERVER COMPROMISE");
        }
    }

    public boolean checkAuthCookie(@NotNull WsContext ctx) {
        String authCookie = ctx.cookie(KEY_COOKIE);
        return authCookie != null && Objects.equals(authCookie, authKey);
    }

    private void configureTLS(@NotNull JavalinConfig config) {
        if (!tlsEnabled) {
            Logs.warning("TLS is not enabled.");
            return;
        }
        try {
            final String fullKeystorePath = Main.PATH + File.separator + keyStorePath;

            if (Files.exists(Paths.get(fullKeystorePath))) {
                SslPlugin plugin = new SslPlugin(conf -> {
                    conf.keystoreFromPath(fullKeystorePath, keyStorePassword);
                    conf.http2 = false;
                    conf.insecure = false;
                    conf.secure = true;
                    conf.securePort = port;
                    conf.sniHostCheck = sni;
                });
                config.registerPlugin(plugin);
                Logs.info("TLS is enabled.");
            } else {
                Logs.warning(String.format("TLS is enabled but %s doesn't exist. TLS disabled.", fullKeystorePath));
            }
        } catch (Exception e) {
            Logs.error("Error while enabling TLS: " + e.getMessage());
            Logs.warning("TLS is not enabled.");
        }
    }

    private void configureCors(@NotNull JavalinConfig config) {
        config.bundledPlugins.enableCors(cors -> cors.addRule(corsConfig -> {
            if (corsOrigin.contains("*")) {
                Logs.info("Enabling CORS for *");
                corsConfig.anyHost();
            } else {
                corsOrigin.forEach(origin -> {
                    Logs.info(String.format("Enabling CORS for %s", origin));
                    corsConfig.allowHost(origin);
                });
            }
        }));
    }

    public void addRoutes() {
        javalin.ws("", instance.getRequestsHandler()::handle);

        javalin.get("/ping", handler -> handler.status(200).json("ok"));
    }

    public void start() {
        javalin.start(port);
    }

    public void stop() {
        instance.getRequestsHandler().closeSessions();
        javalin.stop();
    }
}
