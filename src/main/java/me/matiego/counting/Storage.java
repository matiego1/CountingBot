package me.matiego.counting;

import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Response;
import net.dv8tion.jda.api.entities.Webhook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Counting channels storage
 */
public class Storage {

    @SuppressWarnings("unused")
    private Storage() throws IllegalAccessException {
        throw new IllegalAccessException("Use Storage#load instead");
    }

    private Storage(@NotNull HashMap<Long, ChannelData> cache) {
        this.cache = cache;
    }

    private final HashMap<Long, ChannelData> cache;

    /**
     * Adds a new counting channel.
     * @param data the channel data
     * @return {@code Response.SUCCESS} if the channel was added successfully, otherwise {@code Response.FAILURE}
     */
    public synchronized @NotNull Response addChannel(@NotNull ChannelData data) {
        if (cache.containsKey(data.getChannelId())) return Response.NO_CHANGES;
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO counting_channels(chn, guild, type, url) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE chn = chn, guild = ?, type = ?, url = ?")) {
            stmt.setString(1, String.valueOf(data.getChannelId()));
            stmt.setString(2, String.valueOf(data.getGuildId()));
            stmt.setString(3, data.getType().name());
            stmt.setString(4, data.getWebhookUrl());

            stmt.setString(5, String.valueOf(data.getGuildId()));
            stmt.setString(6, data.getType().name());
            stmt.setString(7, data.getWebhookUrl());
            stmt.execute();

            cache.put(data.getChannelId(), data);
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
     * Returns a channel data.
     * @param id id of the channel
     * @return the channel data
     */
    public synchronized @Nullable ChannelData getChannel(long id) {
        return cache.get(id);
    }

    /**
     * Returns a list of all added channels.
     * @return the list of all added channels
     */
    public synchronized @NotNull List<ChannelData> getChannels() {
        return cache.values().stream().toList();
    }

    /**
     * Loads counting channels from the database.
     * @return a new instance of this class with loaded channels.
     */
    public static @Nullable Storage load() {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT chn, guild, type, url FROM counting_channels")) {
            ResultSet result = stmt.executeQuery();
            HashMap<Long, ChannelData> cache = new HashMap<>();
            while (result.next()) {
                try {
                    String url = result.getString("url");
                    Matcher matcher = Webhook.WEBHOOK_URL.matcher(url);
                    if (!matcher.matches()) throw new IllegalArgumentException("Incorrect webhook url");
                    cache.put(
                            Long.parseLong(result.getString("chn")),
                            new ChannelData(
                                    Long.parseLong(result.getString("chn")),
                                    Long.parseLong(result.getString("guild")),
                                    ChannelData.Type.valueOf(result.getString("type")),
                                    Long.parseLong(matcher.group(1)),
                                    url
                            )
                    );
                } catch (Exception e) {
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
