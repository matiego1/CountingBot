package me.matiego.counting.utils;

import me.matiego.counting.Main;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class Utils {
    public static final Color GREEN = Color.decode("#5dd55d");
    public static final Color YELLOW = Color.decode("#f0e479");
    public static final Color RED = Color.decode("#f17f8b");

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
        try (InputStream stream = Main.class.getClassLoader().getResourceAsStream("version.properties")) {
            Properties properties = new Properties();
            properties.load(stream);
            return properties.getProperty("version", "<unknown version>");
        } catch (Exception e) {
            Logs.warningLocal("Failed to get project version", e);
        }
        return "<unknown version>";
    }

    public static String DEFAULT_ADMIN_KEY = "REPLACE_ME";

    public static boolean checkAdminKey(@Nullable String string, @NotNull User user) {
        if (string == null) return false;
        String adminKey = Main.getInstance().getConfig().getString("admin-key");
        if (adminKey == null || adminKey.isBlank() || adminKey.equalsIgnoreCase(DEFAULT_ADMIN_KEY)) {
            Logs.warning("The admin-key is not set.");
            return false;
        }
        if (string.equals(adminKey)) {
            Logs.info(DiscordUtils.getAsTag(user) + " successfully used the administrator key.");
            return true;
        }
        return false;
    }
}
