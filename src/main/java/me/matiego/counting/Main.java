package me.matiego.counting;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.neovisionaries.ws.client.DualStackMode;
import com.neovisionaries.ws.client.WebSocketFactory;
import me.matiego.counting.commands.*;
import me.matiego.counting.utils.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * The main class.
 */
public final class Main extends JavaPlugin {

    public Main() {
        instance = this;
    }

    private static Main instance;
    private MySQL mySQL;
    private Storage storage;
    private Dictionary dictionary;
    private CommandHandler commandHandler;
    private UserRanking userRanking;

    private JDA jda;
    private ExecutorService callbackThreadPool;

    /**
     * The plugin enable logic.
     */
    @Override
    public void onEnable() {
        long time = System.currentTimeMillis();
        //noinspection ResultOfMethodCallIgnored - generating prime numbers
        Primes.isPrime(2);

        //Implementation checks
        if (ChannelData.Type.values().length > 25 || ChannelData.Type.values().length == 0) {
            Logs.error("Zero or too many types of the counting channels are implemented. Please contact the developer");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        //Save config file
        saveDefaultConfig();

        //Add console command
        PluginCommand command = getCommand("counting");
        if (command != null) command.setExecutor((sender, cmd, label, args) -> {
            reloadConfig();
            sender.sendRichMessage("<green>Successfully reloaded config.");
            return true;
        });

        //Open MySQL connection
        Logs.info("Opening the MySQL connection...");
        String username = getConfig().getString("database.username", "");
        String password = getConfig().getString("database.password", "");
        try {
            mySQL = new MySQL("jdbc:mysql://" + getConfig().getString("database.host") + ":" + getConfig().getString("database.port") + "/" + getConfig().getString("database.database") + "?user=" + username + "&password=" + password, username, password);
        } catch (Exception e) {
            Logs.error("An error occurred while opening the MySQL connection.", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (!mySQL.createTable()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        //Load storages
        Logs.info("Loading saved channels...");
        storage = Storage.load();
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
                Logs.error("An error occurred while shutting down the Discord bot.", e);
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
            jda = JDABuilder.create(Utils.getIntents())
                    .setToken(getConfig().getString("bot-token", ""))
                    .setMemberCachePolicy(MemberCachePolicy.NONE)
                    .setCallbackPool(callbackThreadPool, false)
                    .setGatewayPool(Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Counting - JDA Gateway").build()), true)
                    .setRateLimitPool(new ScheduledThreadPoolExecutor(5, new ThreadFactoryBuilder().setNameFormat("Counting - JDA Rate Limit").build()), true)
                    .setWebsocketFactory(new WebSocketFactory().setDualStackMode(DualStackMode.IPV4_ONLY))
                    .setHttpClient(Utils.getHttpClient())
                    .setAutoReconnect(true)
                    .setBulkDeleteSplittingEnabled(false)
                    .setEnableShutdownHook(false)
                    .setContextEnabled(false)
                    .disableCache(Utils.getDisabledCacheFlag())
                    .setActivity(Activity.playing(Translation.GENERAL__STATUS.toString()))
                    .addEventListeners(new ListenerAdapter() {
                        @Override
                        public void onReady(@NotNull ReadyEvent event) {
                            Main.getInstance().onDiscordBotReady();
                            Main.getInstance().getJda().removeEventListener(this);
                        }
                    })
                    .build();
        } catch (Exception e) {
            Logs.error("An error occurred while enabling the Discord bot." + (e instanceof InvalidTokenException ? " Is the provided bot token correct?" : ""), e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Logs.info("Plugin enabled in " + (System.currentTimeMillis() - time) + "ms.");
    }

    public void onDiscordBotReady() {
        long time = System.currentTimeMillis();
        Logs.info("Discord bot enabled!");

        EmbedBuilder eb = new EmbedBuilder();
        eb.setDescription("Bot has been enabled!");
        eb.setTimestamp(Instant.now());
        eb.setColor(Color.GREEN);
        TextChannel logsChn = jda.getTextChannelById(getConfig().getLong("logs-channel-id"));
        if (logsChn != null) logsChn.sendMessageEmbeds(eb.build()).queue();

        //Check permissions
        jda.getGuilds().forEach(guild -> {
            if (!guild.getSelfMember().hasPermission(Utils.getRequiredPermissions())) {
                Logs.warning("The Discord bot does not have all the required permissions in the " + guild.getName() + " guild. Add the bot to it again.");
            }
        });

        //Check channels
        List<ChannelData> channels = getStorage().getChannels();
        int removed = (int) channels.stream()
                .map(ChannelData::getChannelId)
                .filter(id -> jda.getTextChannelById(id) == null)
                .map(id -> getStorage().removeChannel(id))
                .filter(response -> response == Response.SUCCESS)
                .count();
        if (removed > 0) Logs.info("Successfully removed " + removed + " unknown counting channel(s).");

        //Check webhooks
        List<Pair<CompletableFuture<Webhook>, ChannelData>> futures1 = channels.stream()
                .map(data -> new Pair<>(jda.retrieveWebhookById(data.getWebhookId()).submit(), data))
                .toList();
        int refreshedWebhooks = 0;
        for (Pair<CompletableFuture<Webhook>, ChannelData> future : futures1) {
            try {
                future.getFirst().get();
            } catch (Exception e) {
                if (refreshWebhook(future.getSecond())) refreshedWebhooks++;
            }
        }
        if (refreshedWebhooks > 0) Logs.info("Successfully refreshed " + refreshedWebhooks + " unknown webhook(s).");

        //Modify permissions
        Logs.info("Unblocking the counting channels...");
        List<Pair<CompletableFuture<Void>, String>> futures2 = getStorage().getChannels().stream()
                .map(ChannelData::getChannelId)
                .map(id -> jda.getTextChannelById(id))
                .filter(Objects::nonNull)
                .map(chn -> {
                    Pair<CompletableFuture<Void>, String> result = new Pair<>(null, "[ID: " + chn.getIdLong() + "; Name: " + chn.getName() + "]");
                    try {
                        result.setFirst(chn.getManager().sync().submit());
                    } catch (Exception e) {
                        Logs.error("An error occurred while unblocking the counting channel. Is this channel in any category? " + result.getSecond(), e);
                    }
                    return result;
                })
                .toList();
        for (Pair<CompletableFuture<Void>, String> future : futures2) {
            try {
                future.getFirst().get();
            } catch (Exception e) {
                Logs.error("An error occurred while unblocking the counting channel. " + future.getSecond());
            }
        }

        //Add event listeners
        commandHandler = new CommandHandler(Arrays.asList(
                new PingCommand(),
                new AboutCommand(),
                new FeedbackCommand(this),
                new CountingCommand(this),
                new DictionaryCommand(this),
                new RankingCommand(),
                new DeleteMessageCommand(),
                new RankingContextCommand()
        ));
        jda.addEventListener(
                new MessageHandler(),
                commandHandler
        );

        Logs.info("Checks performed in " + (System.currentTimeMillis() - time) + "ms.");
    }

    private boolean refreshWebhook(@NotNull ChannelData data) {
        long id = data.getChannelId();
        if (getStorage().removeChannel(id) == Response.FAILURE) return false;

        TextChannel chn = jda.getTextChannelById(id);
        if (chn == null) return false;
        List<Webhook> webhooks = chn.retrieveWebhooks().complete();
        Webhook webhook = webhooks.isEmpty() ? chn.createWebhook("Counting bot").complete() : webhooks.get(0);

        if (getStorage().addChannel(new ChannelData(id, data.getGuildId(), data.getType(), webhook)) != Response.FAILURE) return true;
        Logs.warning("An error occurred while refreshing an unknown webhook. The counting channel has been removed.");
        getStorage().removeChannel(id);
        return false;
    }

    /**
     * The plugin disable logic.
     */
    @Override
    public void onDisable() {
        long time = System.currentTimeMillis();
        //shut down JDA
        if (jda != null) {
            jda.getRegisteredListeners().forEach(listener -> jda.removeEventListener(listener));

            List<Pair<CompletableFuture<PermissionOverride>, String>> futures = getStorage().getChannels().stream()
                    .map(ChannelData::getChannelId)
                    .map(id -> jda.getTextChannelById(id))
                    .filter(Objects::nonNull)
                    .map(chn -> {
                        Pair<CompletableFuture<PermissionOverride>, String> result = new Pair<>(null, "[ID: " + chn.getIdLong() + "; Name: " + chn.getName() + "]");
                        try {
                            result.setFirst(
                                    chn.upsertPermissionOverride(chn.getGuild().getSelfMember()).grant(Permission.MESSAGE_SEND).submit()
                                            .thenCompose(v -> chn.upsertPermissionOverride(chn.getGuild().getPublicRole()).deny(Permission.MESSAGE_SEND).submit())
                            );
                        } catch (Exception e) {
                            Logs.error("An error occurred while blocking the counting channel. " + result.getSecond(), e);
                        }
                        return result;
                    })
                    .filter(pair -> pair.getFirst() != null)
                    .toList();
            for (Pair<CompletableFuture<PermissionOverride>, String> future : futures) {
                try {
                    future.getFirst().get();
                } catch (Exception e) {
                    Logs.error("An error occurred while blocking the counting channel. " + future.getSecond());
                }
            }

            EmbedBuilder eb = new EmbedBuilder();
            eb.setDescription("Bot has been disabled!");
            eb.setTimestamp(Instant.now());
            eb.setColor(Color.RED);
            TextChannel chn = jda.getTextChannelById(getConfig().getLong("logs-channel-id"));
            if (chn != null) chn.sendMessageEmbeds(eb.build()).complete();

            CompletableFuture<Void> shutdownTask = new CompletableFuture<>();
            jda.addEventListener(new ListenerAdapter() {
                @Override
                public void onShutdown(@NotNull ShutdownEvent event) {
                    shutdownTask.complete(null);
                }
            });
            jda.shutdown();
            try {
                shutdownTask.get(5, TimeUnit.SECONDS);
                Logs.info("Successfully shut down the Discord bot.");
            } catch (Exception e) {
                Logs.warning("Discord bot took too long to shut down, skipping. Ignore any errors from this point.");
            }
            jda = null;
            if (callbackThreadPool != null) callbackThreadPool.shutdownNow();
            callbackThreadPool = null;
        }
        //close MySQL connection
        if (mySQL != null) mySQL.close();
        Logs.info("Plugin disabled in " + (System.currentTimeMillis() - time) + "ms.");
    }

    /**
     * Returns an instance of the Main class.
     * @return the instance of the Main class
     */
    public static Main getInstance() {
        return instance;
    }

    /**
     * Returns a mysql connection.
     * @return the mysql connection
     * @throws SQLException if an error occurred while fetching the connection
     */
    public @NotNull Connection getMySQLConnection() throws SQLException {
        if (mySQL == null) throw new SQLException("The MySQL database has not been opened yet.");
        return mySQL.getConnection();
    }

    /**
     * Returns the data storage.
     * @return the data storage
     */
    public Storage getStorage() {
        return storage;
    }

    /**
     * Returns the Discord bot instance.
     * @return the Discord bot instance
     */
    public synchronized JDA getJda() {
        return jda;
    }

    /**
     * Returns the dictionary.
     * @return the dictionary
     */
    public Dictionary getDictionary() {
        return dictionary;
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    public UserRanking getUserRanking() {
        return userRanking;
    }
}
