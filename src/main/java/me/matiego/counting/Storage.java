package me.matiego.counting;

import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Pair;
import me.matiego.counting.utils.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Storage {

    @SuppressWarnings("unused")
    public Storage() throws IllegalAccessException {
        throw new IllegalAccessException("Use Storage#load instead");
    }

    private Storage(@NotNull HashMap<Long, Pair<ChannelType, String>> cache) {
        this.cache = cache;
    }

    private final HashMap<Long, Pair<ChannelType, String>> cache;

    public synchronized @NotNull Response addChannel(long id, @NotNull ChannelType type, @NotNull String url) {
        if (cache.containsKey(id)) return Response.NO_CHANGES;
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO counting_channels(chn, type, url) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE chn = chn, type = ?, url = ?")) {
            stmt.setString(1, String.valueOf(id));
            stmt.setString(2, type.name());
            stmt.setString(3, url);
            stmt.setString(4, type.name());
            stmt.setString(5, url);
            stmt.execute();
            cache.put(id, new Pair<>(type, url));
            return Response.SUCCESS;
        } catch (SQLException e) {
            Logs.error("An error occurred while saving the channel to the storage", e);
        }
        return Response.FAILURE;
    }

    public synchronized @NotNull Response removeChannel(long id) {
        if (!cache.containsKey(id)) return Response.NO_CHANGES;
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM counting_channels WHERE chn = ?;")) {
            stmt.setString(1, String.valueOf(id));
            stmt.execute();
            cache.remove(id);
            return Response.SUCCESS;
        } catch (SQLException e) {
            Logs.error("An error occurred while removing the channel from the storage", e);
        }
        return Response.FAILURE;
    }

    public synchronized @Nullable Pair<ChannelType, String> getChannelType(long id) {
        return cache.get(id);
    }

    public synchronized @NotNull List<Pair<Long, ChannelType>> getChannels() {
        List<Pair<Long, ChannelType>> result = new ArrayList<>();
        cache.forEach((id, pair) -> result.add(new Pair<>(id, pair.getFirst())));
        return result;
    }

    public static @Nullable Storage load() {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT chn, type, url FROM counting_channels")) {
            ResultSet result = stmt.executeQuery();
            HashMap<Long, Pair<ChannelType, String>> cache = new HashMap<>();
            while (result.next()) {
                try {
                    cache.put(Long.parseLong(result.getString("chn")), new Pair<>(ChannelType.valueOf(result.getString("type")), result.getString("url")));
                } catch (IllegalArgumentException e) {
                    Logs.warning("An error occurred while loading the counting channels: " + e.getMessage());
                }
            }
            return new Storage(cache);
        } catch (SQLException e) {
            Logs.error("An error occurred while loading the storage", e);
        }
        return null;
    }

}
