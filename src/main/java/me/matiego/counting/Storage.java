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

/**
 * Counting channels storage
 */
public class Storage {

    @SuppressWarnings("unused")
    public Storage() throws IllegalAccessException {
        throw new IllegalAccessException("Use Storage#load instead");
    }

    private Storage(@NotNull HashMap<Long, Pair<ChannelType, String>> cache) {
        this.cache = cache;
    }

    private final HashMap<Long, Pair<ChannelType, String>> cache;

    /**
     * Adds a new counting channel.
     * @param id id of the channel
     * @param type type of the channel
     * @param url a webhook url
     * @return {@code Response.SUCCESS} if the channel was added successfully, otherwise {@code Response.FAILURE}
     */
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

    /**
     * Remove the counting channel.
     * @param id id of the channel
     * @return {@code Response.SUCCESS} if the channel was removed successfully, {@code Response.NO_CHANGES} if the channel has not been added yet or {@code Response.FAILURE} if an error occurred
     */
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

    /**
     * Returns a pair of the channel type and the webhook url associated with it.
     * @param id id of the channel
     * @return the pair of the channel type and the webhook url
     */
    public synchronized @Nullable Pair<ChannelType, String> getChannel(long id) {
        return cache.get(id);
    }

    /**
     * Returns a list of all added channels.
     * @return the list of all added channels
     */
    public synchronized @NotNull List<Pair<Long, ChannelType>> getChannels() {
        List<Pair<Long, ChannelType>> result = new ArrayList<>();
        cache.forEach((id, pair) -> result.add(new Pair<>(id, pair.getFirst())));
        return result;
    }

    /**
     * Loads counting channels from the database.
     * @return a new instance of this class with loaded channels.
     */
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
