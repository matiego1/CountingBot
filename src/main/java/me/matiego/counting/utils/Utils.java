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

    private static String version;
    public static @NotNull String getVersion() {
        if (version != null) return version;
        try (InputStream stream = Main.class.getClassLoader().getResourceAsStream("version.properties")) {
            Properties properties = new Properties();
            properties.load(stream);
            version = properties.getProperty("version", "<unknown version>");
            return version;
        } catch (Exception e) {
            Logs.warningLocal("Failed to get project version", e);
        }
        version = "<unknown version>";
        return version;
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

    public static @NotNull String parseMillisToString(long time, boolean useMilliseconds) {
        time = Math.round((double) time / (useMilliseconds ? 1 : 1000));
        int x = useMilliseconds ? 1000 : 1;
        String result = "";

        //days
        int d = (int) (time / (3600 * 24 * x));
        time -= (long) d * 3600 * 24 * x;
        if (d != 0) result += d + "d ";
        //hours
        int h = (int) (time / (3600 * x));
        time -= (long) h * 3600 * x;
        if (h != 0) result += h + "h ";
        //minutes
        int m = (int) (time / (60 * x));
        time -= (long) m * 60 * x;
        if (m != 0) result += m + "m ";
        //seconds
        int s = (int) (time / x);
        time -= (long) s * x;
        if (s != 0) result += s + "s ";
        //milliseconds
        int ms = (int) (time);
        if (ms != 0) result += ms + "ms ";

        if (result.isEmpty()) return useMilliseconds ? "0ms" : "0s";
        return result.substring(0, result.length() - 1);
    }
}
