package me.matiego.counting.utils;

import me.matiego.counting.Main;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Utils {
    public static final Color GREEN = Color.decode("#5dd55d");
    public static final Color YELLOW = Color.decode("#f0e479");
    public static final Color RED = Color.decode("#f17f8b");

    public static void async(@NotNull Runnable task) {
        try {
            Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), task);
        } catch (IllegalPluginAccessException e) {
            Logs.warning("An error occurred while running an async task. The task will be run synchronously.");
            task.run();
        }
    }

    public static long now() {
        return System.currentTimeMillis();
    }

    public static <K, V> HashMap<K, V> createLimitedSizeMap(int maxEntries) {
        return new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        };
    }

    public static @NotNull String getVersion() {
        //noinspection deprecation
        return Main.getInstance().getDescription().getVersion();
    }

    public static boolean checkAdminKey(@Nullable String string, @NotNull User user) {
        if (string == null) return false;
        if (string.equals(Main.getInstance().getConfig().getString("admin-key"))) {
            Logs.info(DiscordUtils.getAsTag(user) + " successfully used the administrator key.");
            return true;
        }
        return false;
    }
}
