package me.matiego.counting.minecraft;

import me.matiego.counting.Main;
import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Pair;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.entities.UserSnowflake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class MinecraftAccounts {
    public MinecraftAccounts(@NotNull Main instance) {
        this.instance = instance;
    }

    private final Main instance;
    private final Map<UserSnowflake, Pair<UUID, Long>> cache = Collections.synchronizedMap(Utils.createLimitedSizeMap(500));

    public boolean addMinecraftAccount(long server, @NotNull UserSnowflake user, @NotNull UUID uuid) {
        try (Connection conn = instance.getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO counting_minecraft_accounts VALUES(?, ?, ?);")) {
            stmt.setString(1, String.valueOf(server));
            stmt.setString(2, user.getId());
            stmt.setString(3, uuid.toString());

            if (stmt.executeUpdate() > 0) {
                cache.put(user, new Pair<>(uuid, server));
                return true;
            }
        } catch (Exception e) {
            Logs.error("Failed to add a user's minecraft account", e);
        }
        return false;
    }

    public @Nullable Pair<UUID, Long> getMinecraftAccount(@NotNull UserSnowflake user) {
        Pair<UUID, Long> cached = cache.get(user);
        if (cached != null) return cached;

        try (Connection conn = instance.getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT server, minecraft FROM counting_minecraft_accounts WHERE discord = ?;")) {
            stmt.setString(1, user.getId());

            ResultSet resultSet = stmt.executeQuery();
            if (!resultSet.next()) return null;
            return new Pair<>(
                    UUID.fromString(resultSet.getString("minecraft")),
                    Long.parseLong(resultSet.getString("server"))
            );
        } catch (Exception e) {
            Logs.error("Failed to get a user's minecraft account", e);
        }
        return null;
    }

    public boolean hasMinecraftAccount(@NotNull UserSnowflake user) {
        return getMinecraftAccount(user) != null;
    }

    public boolean removeMinecraftAccount(@NotNull UserSnowflake user) {
        cache.remove(user);
        try (Connection conn = instance.getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM counting_minecraft_accounts WHERE discord = ?;")) {
            stmt.setString(1, user.getId());

            stmt.execute();
            return true;
        } catch (Exception e) {
            Logs.error("Failed to remove a user's minecraft account", e);
        }
        return false;
    }
}
