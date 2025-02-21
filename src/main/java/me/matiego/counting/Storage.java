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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

public class Storage {
    private Storage(@NotNull Main instance, @NotNull HashMap<Long, ChannelData> cache) {
        this.instance = instance;
        this.cache = cache;
    }

    private final Main instance;
    private final HashMap<Long, ChannelData> cache;

    public synchronized @NotNull Response addChannel(@NotNull ChannelData data) {
        if (cache.containsKey(data.getChannelId())) return Response.NO_CHANGES;
        try (Connection conn = instance.getMySQLConnection();
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
            Logs.error("Failed to add the counting channel to the storage.", e);
        }
        return Response.FAILURE;
    }

    public synchronized @NotNull Response removeChannel(long id) {
        if (!cache.containsKey(id)) return Response.NO_CHANGES;
        try (Connection conn = instance.getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM counting_channels WHERE chn = ?;")) {
            stmt.setString(1, String.valueOf(id));
            stmt.execute();
            cache.remove(id);
            return Response.SUCCESS;
        } catch (SQLException e) {
            Logs.error("Failed to remove the counting channel from the storage.", e);
        }
        return Response.FAILURE;
    }

    public synchronized @Nullable ChannelData getChannel(long id) {
        return cache.get(id);
    }

    public synchronized @NotNull List<ChannelData> getChannels() {
        return new ArrayList<>(cache.values());
    }

    public static @Nullable Storage load(@NotNull Main plugin) {
        try (Connection conn = plugin.getMySQLConnection();
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
                    Logs.warning("Failed to load one of the counting channels from the storage.", e);
                }
            }
            return new Storage(plugin, cache);
        } catch (SQLException e) {
            Logs.error("Failed to load the storage.", e);
        }
        return null;
    }

}
