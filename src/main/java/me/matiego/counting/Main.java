package me.matiego.counting;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.neovisionaries.ws.client.DualStackMode;
import com.neovisionaries.ws.client.WebSocketFactory;
import lombok.Getter;
import me.matiego.counting.commands.*;
import me.matiego.counting.utils.DiscordUtils;
import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Response;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

public final class Main extends JavaPlugin {
    public Main() {
        instance = this;
    }

    @Getter private static Main instance;
    private MySQL mySQL;
    @Getter private Storage storage;
    @Getter private Dictionary dictionary;
    private Commands commands;
    @Getter private UserRanking userRanking;

    private JDA jda;
    private boolean isJdaEnabled = false;
    private ExecutorService callbackThreadPool;

    @Override
    public void onEnable() {
        long time = Utils.now();
        //noinspection ResultOfMethodCallIgnored - generate prime numbers
        Primes.isPrime(2);

        //Implementation checks
        if (ChannelData.Type.values().length > 25 || ChannelData.Type.values().length == 0) {
            Logs.error("Zero or too many types of the counting channels are implemented. Please contact the developer.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        //Save config file
        saveDefaultConfig();

        //Add console command
        PluginCommand command = getCommand("counting");
        if (command != null) command.setExecutor((sender, cmd, label, args) -> {
            JDA jda = getJda();
            if (jda == null) {
                sender.sendRichMessage("<red>Discord bot is offline!");
                return true;
            }
            reload(jda).thenRun(() -> sender.sendRichMessage("<green>Reloaded!"));
            return true;
        });

        //Open MySQL connection
        Logs.info("Opening the MySQL connection...");
        String username = getConfig().getString("database.username", "");
        String password = getConfig().getString("database.password", "");
        try {
            mySQL = new MySQL("jdbc:mysql://" + getConfig().getString("database.host") + ":" + getConfig().getString("database.port") + "/" + getConfig().getString("database.database") + "?user=" + username + "&password=" + password, username, password);
        } catch (Exception e) {
            Logs.error("Failed to open the MySQL connection.", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (!mySQL.createTables()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        //Load storages
        Logs.info("Loading saved channels...");
        storage = Storage.load(this);
        if (storage == null) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        dictionary = new Dictionary();
        userRanking = new UserRanking();

        //Enable Discord bot
        RestAction.setDefaultFailure(throwable -> Logs.error("An error occurred!", throwable));
        Logs.info("Enabling the Discord bot...");
        if (jda != null) {
            try {
                jda.shutdownNow();
                callbackThreadPool.shutdownNow();
            } catch (Exception e) {
                Logs.error("Failed to shut down previous instance of the Discord bot.", e);
            }
            jda = null;
            callbackThreadPool = null;
            Logs.warning("Restart the server instead of reloading it. It may break this plugin.");
        }
        try {
            callbackThreadPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), pool -> {
                ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                worker.setName("Counting - JDA Callback " + worker.getPoolIndex());
                return worker;
            }, null, true);
            JDABuilder builder = JDABuilder.create(DiscordUtils.getIntents())
                    .setToken(getConfig().getString("bot-token", ""))
                    .setMemberCachePolicy(MemberCachePolicy.NONE)
                    .setCallbackPool(callbackThreadPool, false)
                    .setGatewayPool(Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Counting - JDA Gateway").build()), true)
                    .setRateLimitScheduler(new ScheduledThreadPoolExecutor(5, new ThreadFactoryBuilder().setNameFormat("Counting - JDA Rate Limit").build()), true)
                    .setWebsocketFactory(new WebSocketFactory().setDualStackMode(DualStackMode.IPV4_ONLY))
                    .setHttpClient(DiscordUtils.getHttpClient())
                    .setAutoReconnect(true)
                    .setBulkDeleteSplittingEnabled(false)
                    .setEnableShutdownHook(false)
                    .setContextEnabled(false)
                    .disableCache(DiscordUtils.getDisabledCacheFlag())
                    .addEventListeners(new ListenerAdapter() {
                        @Override
                        public void onReady(@NotNull ReadyEvent event) {
                            Main.getInstance().onDiscordBotReady();
                            event.getJDA().removeEventListener(this);
                        }
                    });

            String activity = getConfig().getString("activity", "Counting...");
            try {
                builder.setActivity(Activity.customStatus(activity));
            } catch (IllegalArgumentException e) {
                Logs.warning("Failed to set bot's activity.", e);
            }

            jda = builder.build();
        } catch (Exception e) {
            Logs.error("Failed to enable the Discord bot." + (e instanceof InvalidTokenException ? " Is the provided bot token correct?" : ""), e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Logs.infoLocal("Plugin enabled in " + (Utils.now() - time) + "ms.");
    }

    public @NotNull CompletableFuture<Void> reload(@NotNull JDA jda) {
        reloadConfig();
        sendGuildsMessage();
        checkPermissions();
        int removed = removeNonExistentChannels(jda);
        if (removed > 0) Logs.info("Removed " + removed + " unknown counting channel(s).");
        return refreshWebhooks(jda).thenAccept(refreshedWebhooks -> {
            if (refreshedWebhooks > 0) {
                Logs.info("Refreshed " + refreshedWebhooks + " unknown webhook(s).");
            }
            refreshSlowmode(jda);
            Logs.info("Reloaded!");
        });
    }

    private void sendGuildsMessage() {
        JDA jda = getJda();
        if (jda == null) return;
        List<String> guilds = jda.getGuilds().stream().map(Guild::getName).toList();
        Logs.info("Bot's guilds: " + String.join(", ", guilds) + " (" + guilds.size() + ")");
    }


    public void onDiscordBotReady() {
        long time = Utils.now();

        isJdaEnabled = true;
        Logs.info("Discord bot enabled!");

        //Check permissions
        checkPermissions();

        //Check channels
        int removed = removeNonExistentChannels(jda);
        if (removed > 0) Logs.info("Removed " + removed + " unknown counting channel(s).");

        //Check webhooks
        refreshWebhooks(jda).thenAccept(refreshedWebhooks -> {

            if (refreshedWebhooks > 0) Logs.info("Refreshed " + refreshedWebhooks + " unknown webhook(s).");

            //Add event listeners
            commands = new Commands(jda,
                    new AboutCommand(this),
                    new BlockCommand(this),
                    new CountingCommand(this),
                    new DeleteMessageCommand(this),
                    new DeleteMessageContextCommand(this),
                    new DictionaryCommand(this),
                    new FeedbackCommand(this),
                    new PingCommand(this),
                    new RankingCommand(this),
                    new RankingContextCommand(this),
                    new ReloadCommand(this),
                    new UnblockCommand(this),
                    new WordListCommand(this)
            );
            jda.addEventListener(
                    new MessageHandler(this),
                    commands
            );

            // Refresh channels' slowmode
            refreshSlowmode(jda);

            Logs.info("Checks performed in " + (Utils.now() - time) + "ms.");

            sendGuildsMessage();
        });
    }

    public void checkPermissions() {
        jda.getGuilds().forEach(guild -> {
            if (!guild.getSelfMember().hasPermission(DiscordUtils.getRequiredPermissions())) {
                Logs.warning("The Discord bot does not have all the required permissions in the " + guild.getName() + " guild. Add the bot to it again.");
            }
        });
    }

    public int removeNonExistentChannels(@NotNull JDA jda) {
        return (int) getStorage().getChannels().stream()
                .map(ChannelData::getChannelId)
                .filter(id -> DiscordUtils.getSupportedChannelById(jda, id) == null)
                .map(id -> getStorage().removeChannel(id))
                .filter(response -> response == Response.SUCCESS)
                .count();
    }

    public @NotNull CompletableFuture<Integer> refreshWebhooks(@NotNull JDA jda) {
        return getStorage().getChannels().stream()
                .map(data -> jda.retrieveWebhookById(data.getWebhookId()).submit()
                        .handle((webhook, e) -> {
                            if (webhook != null) return CompletableFuture.completedFuture(0);
                            return refreshWebhook(jda, data);
                        })
                        .thenCompose(f -> f)
                )
                .reduce((a, b) -> a.thenCombine(b, Integer::sum))
                .orElseGet(() -> CompletableFuture.completedFuture(0));
    }

    private @NotNull CompletableFuture<Integer> refreshWebhook(@NotNull JDA jda, @NotNull ChannelData data) {
        CompletableFuture<Integer> failure = CompletableFuture.completedFuture(0);

        long id = data.getChannelId();
        if (getStorage().removeChannel(id) == Response.FAILURE) return failure;

        if (!(DiscordUtils.getSupportedChannelById(jda, id) instanceof IWebhookContainer chn)) return failure;
        return DiscordUtils.getOrCreateWebhook(chn)
                .handle((webhook, e) -> {
                    if (e == null) {
                        if (getStorage().addChannel(new ChannelData(id, data.getGuildId(), data.getType(), webhook)) != Response.FAILURE) return 1;
                    }
                    Logs.warning("Failed to refresh an unknown webhook. The counting channel will be removed.", e);
                    getStorage().removeChannel(id);
                    return 0;
                });
    }

    private void refreshSlowmode(@NotNull JDA jda) {
        getStorage().getChannels().stream()
                .map(ChannelData::getChannelId)
                .map(id -> DiscordUtils.getSupportedChannelById(jda, id))
                .filter(Objects::nonNull)
                .forEach(chn -> DiscordUtils.setSlowmode(this, chn));
    }

    @Override
    public void onDisable() {
        long time = Utils.now();

        //shut down JDA
        if (jda != null) {
            jda.getRegisteredListeners().forEach(listener -> jda.removeEventListener(listener));

            try {
                Logs.infoWithBlock("Shutting down Discord bot...").get(10, TimeUnit.SECONDS);
            } catch (Exception ignored) {}

            isJdaEnabled = false;
            try {
                disableDiscordBot();
            } catch (Exception e) {
                Logs.error("Failed to shut down Discord bot.", e);
            }
        }
        if (callbackThreadPool != null) {
            callbackThreadPool.shutdownNow();
            callbackThreadPool = null;
        }

        //close MySQL connection
        if (mySQL != null) mySQL.close();

        Logs.info("Disabled in " + (Utils.now() - time) + "ms.");
    }

    private void disableDiscordBot() throws Exception {
        if (jda == null) return;
        jda.shutdown();
        if (jda.awaitShutdown(5, TimeUnit.SECONDS)) return;
        Logs.warning("Discord bot took too long to shut down, skipping.");
        jda.shutdownNow();
        if (jda.awaitShutdown(3, TimeUnit.SECONDS)) return;
        jda = null;
    }

    public @NotNull Connection getMySQLConnection() throws SQLException {
        if (mySQL == null) throw new SQLException("The MySQL database has not been opened yet.");
        return mySQL.getConnection();
    }

    public synchronized JDA getJda() {
        return isJdaEnabled ? jda : null;
    }
}
