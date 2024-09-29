package me.matiego.counting.utils;

import me.matiego.counting.Main;
import org.bukkit.Bukkit;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Utils {
    public static final int SECOND = 1000;

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
}
