package me.matiego.counting.minecraft;

import me.matiego.counting.Main;
import me.matiego.counting.utils.Logs;
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

public class McAccounts {
    public McAccounts(@NotNull Main instance) {
        this.instance = instance;
    }

    private final Main instance;
    private final Map<UserSnowflake, UUID> cache = Collections.synchronizedMap(Utils.createLimitedSizeMap(500));

    public boolean setMinecraftAccount(@NotNull UserSnowflake user, @NotNull UUID uuid) {
        try (Connection conn = instance.getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO counting_minecraft_accounts VALUES(?, ?) ON DUPLICATE KEY UPDATE minecraft = ?")) {
            stmt.setString(1, user.getId());
            stmt.setString(2, uuid.toString());
            stmt.setString(3, uuid.toString());

            if (stmt.executeUpdate() > 0) {
                cache.put(user, uuid);
                return true;
            }
        } catch (Exception e) {
            Logs.error("Failed to set a user's minecraft account", e);
        }
        return false;
    }

    public @Nullable UUID getMinecraftAccount(@NotNull UserSnowflake user) {
        UUID cached = cache.get(user);
        if (cached != null) return cached;

        try (Connection conn = instance.getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT minecraft FROM counting_minecraft_accounts WHERE discord = ?;")) {
            stmt.setString(1, user.getId());

            ResultSet resultSet = stmt.executeQuery();
            if (!resultSet.next()) return null;

            return UUID.fromString(resultSet.getString("minecraft"));
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
