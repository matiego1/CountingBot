package me.matiego.counting.counting;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.neovisionaries.ws.client.DualStackMode;
import com.neovisionaries.ws.client.WebSocketFactory;
import me.matiego.counting.counting.utils.Logs;
import me.matiego.counting.counting.utils.Primes;
import me.matiego.counting.counting.utils.Utils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.*;

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

    @Override
    public void onEnable() {
        long time = System.currentTimeMillis();
        //noinspection ResultOfMethodCallIgnored - generating prime numbers
        Primes.isPrime(2);
        //Checks
        if (ChannelType.values().length > 25 || ChannelType.values().length == 0) {
            Logs.error("Zero or too many types of the counting channels are implemented. Please contact the developer");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        //Config
        saveDefaultConfig();
        //MySQL
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
        //Storage
        Logs.info("Loading saved channels...");
        storage = Storage.load();
        if (storage == null) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        dictionary = new Dictionary();
        //Discord bot
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
                    .addEventListeners(new DiscordCommands(this))
                    .addEventListeners(new MessageHandler())
                    .build();
            jda.updateCommands().addCommands(
                    Commands.slash("ping", "Shows the current ping of the bot")
                            .addOption(OptionType.BOOLEAN, "ephemeral", "whether this message should only be visible to you", false),
                    Commands.slash("counting", "Manages the counting channels")
                            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
                            .setGuildOnly(true)
                            .addSubcommands(
                                    new SubcommandData("add", "Opens a new counting channel"),
                                    new SubcommandData("remove", "Closes the counting channel"),
                                    new SubcommandData("list", "Shows all opened counting channel in this guild")),
                    Commands.slash("dictionary", "Manages dictionaries")
                            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
                            .setGuildOnly(true)
                            .addSubcommands(
                                    new SubcommandData("add", "Adds a word to the dictionary")
                                            .addOption(OptionType.STRING, "language", "The dictionary's type", true)
                                            .addOption(OptionType.STRING, "word", "A word to add", true),
                                    new SubcommandData("remove", "Removes a word from the dictionary")
                                            .addOption(OptionType.STRING, "language", "The dictionary's type", true)
                                            .addOption(OptionType.STRING, "word", "A word to remove", true),
                                    new SubcommandData("load", "Loads a file into the dictionary")
                                            .addOption(OptionType.STRING, "language", "The dictionary type", true)
                                            .addOption(OptionType.STRING, "admin-key", "The secret administrator key", true)
                                            .addOption(OptionType.STRING, "file", "The dictionary file", true)
                            )
            ).queue();
        } catch (Exception e) {
            Logs.error("An error occurred while enabling the Discord bot." + (e instanceof LoginException ? " Is the provided bot token correct?" : ""), e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Logs.info("Plugin enabled in " + (System.currentTimeMillis() - time) + "ms.");
    }

    @Override
    public void onDisable() {
        long time = System.currentTimeMillis();
        //shut down JDA
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

    public static Main getInstance() {
        return instance;
    }

    public @NotNull Connection getMySQLConnection() throws SQLException {
        if (mySQL == null) throw new SQLException("The MySQL database has not been opened yet.");
        return mySQL.getConnection();
    }

    public Storage getStorage() {
        return storage;
    }

    public JDA getJda() {
        return jda;
    }

    public Dictionary getDictionary() {
        return dictionary;
    }
}
