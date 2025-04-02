package me.matiego.counting;

import lombok.Getter;
import me.matiego.counting.commands.AboutCommand;
import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.IOException;

@Getter
public class Config {
    private Config(@NotNull YamlFile config) {
        this.config = config;
    }

    private final YamlFile config;

    public static @NotNull Config loadConfig(@NotNull String filePath) throws Exception {
        YamlFile config = new YamlFile(new File(filePath));

        if (!config.exists()) {
            Logs.info("Creating a new config file...");
            config.createNewFile();
            setDefaultValues(config);
        }

        config.load();

        return new Config(config);
    }

    private static void setDefaultValues(@NotNull YamlFile config) throws IOException {
        config.setHeader("--- Counting Bot's Config ---");

        config.set("bot-token", "token");
        config.set("admin-key", Utils.DEFAULT_ADMIN_KEY);
        config.set("logs-channel-id", 0);

        config.set("about-message", AboutCommand.DEFAULT_ABOUT_MESSAGE);
        config.set("activity", "Counting...");

        config.set("slowmode", 2);
        config.set("anti-spam.count", 10);
        config.set("anti-spam.time", 3);

        config.set("database.database", "database");
        config.set("database.host", "localhost");
        config.set("database.port", 3306);
        config.set("database.username", "login");
        config.set("database.password", "password");

        config.save();
    }
}
