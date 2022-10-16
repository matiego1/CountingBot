package me.matiego.counting;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.neovisionaries.ws.client.DualStackMode;
import com.neovisionaries.ws.client.WebSocketFactory;
import me.matiego.counting.commands.AboutCommand;
import me.matiego.counting.commands.FeedbackCommand;
import me.matiego.counting.commands.PingCommand;
import me.matiego.counting.utils.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
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
        //Checks
        if (ChannelData.Type.values().length > 25 || ChannelData.Type.values().length == 0) {
            Logs.error("Zero or too many types of the counting channels are implemented. Please contact the developer");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        //Save default config
        saveDefaultConfig();
        //Add console command
        PluginCommand command = getCommand("counting");
        if (command != null) command.setExecutor((sender, cmd, label, args) -> {
            reloadConfig();
            sender.sendMessage("&aSuccessfully reloaded config.");
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
        //Load storage
        Logs.info("Loading saved channels...");
        storage = Storage.load();
        if (storage == null) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        dictionary = new Dictionary();
        //Enable Discord bot
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
                    .setActivity(Activity.competing(Translation.GENERAL__STATUS.toString()))
                    .build();
            jda.awaitReady(); //Yes I know, this will block the thread.
        } catch (Exception e) {
            Logs.error("An error occurred while enabling the Discord bot." + (e instanceof InvalidTokenException ? " Is the provided bot token correct?" : ""), e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        //Check permissions
        jda.getGuilds().forEach(guild -> {
            if (!guild.getSelfMember().hasPermission(Utils.getRequiredPermissions())) {
                Logs.warning("The Discord bot does not have all the required permissions in the " + guild.getName() + " guild. Add the bot to it again.");
            }
        });
        //Check channels
        List<Pair<Long, ChannelData>> channels = getStorage().getChannels();
        int removed = (int) channels.stream()
                .map(Pair::getFirst)
                .filter(id -> jda.getTextChannelById(id) == null)
                .map(id -> getStorage().removeChannel(id))
                .filter(response -> response == Response.SUCCESS)
                .count();
        if (removed > 0) Logs.info("Successfully removed " + removed + " unknown counting channel(s).");
        //Check webhooks
        int refreshedWebhooks = (int) channels.stream()
                .filter(pair -> {
                    try {
                        jda.retrieveWebhookById(pair.getSecond().getWebhookId()).complete();
                        return false;
                    } catch (ErrorResponseException ignored) {}
                    return true;
                })
                .map(pair -> refreshWebhook(pair.getFirst(), pair.getSecond().getType()))
                .filter(Boolean::booleanValue)
                .count();
        if (refreshedWebhooks > 0) Logs.info("Successfully refreshed " + refreshedWebhooks + " unknown webhook(s).");
        //Add event listeners
        jda.addEventListener(
                new MessageHandler(),
                new CommandHandler(Arrays.asList(
                        new PingCommand(),
                        new AboutCommand(),
                        new FeedbackCommand()
                ))
        );

        EmbedBuilder eb = new EmbedBuilder();
        eb.setDescription("Bot has been enabled!");
        eb.setTimestamp(Instant.now());
        eb.setColor(Color.GREEN);
        TextChannel chn = jda.getTextChannelById(getConfig().getLong("logs-channel-id"));
        if (chn != null) chn.sendMessageEmbeds(eb.build()).queue();

        Logs.info("Plugin enabled in " + (System.currentTimeMillis() - time) + "ms.");
    }

    private boolean refreshWebhook(long id, @NotNull ChannelData.Type type) {
        if (getStorage().removeChannel(id) == Response.FAILURE) return false;

        TextChannel chn = jda.getTextChannelById(id);
        if (chn == null) return false;
        List<Webhook> webhooks = chn.retrieveWebhooks().complete();
        Webhook webhook = webhooks.isEmpty() ? chn.createWebhook("Counting bot").complete() : webhooks.get(0);

        if (getStorage().addChannel(id, new ChannelData(type, webhook)) != Response.FAILURE) return true;
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
        jda.getRegisteredListeners().forEach(listener -> jda.removeEventListener(listener));

        EmbedBuilder eb = new EmbedBuilder();
        eb.setDescription("Bot has been disabled!");
        eb.setTimestamp(Instant.now());
        eb.setColor(Color.RED);
        TextChannel chn = jda.getTextChannelById(getConfig().getLong("logs-channel-id"));
        if (chn != null) chn.sendMessageEmbeds(eb.build()).complete();

        if (jda == null) return;
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
}
