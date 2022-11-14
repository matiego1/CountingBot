package me.matiego.counting;

import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Pair;
import me.matiego.counting.utils.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserRanking {

    public @NotNull Response add(long user, long guild) {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO counting_user_ranking(id, guild, amount) VALUES(?, ?, 1) ON DUPLICATE KEY UPDATE amount = amount + 1")) {
            stmt.setString(1, String.valueOf(user));
            stmt.setString(2, String.valueOf(guild));
            stmt.execute();
            return Response.SUCCESS;
        } catch (SQLException e) {
            Logs.error("An error occurred while modifying user ranking.", e);
        }
        return Response.FAILURE;
    }

    public @NotNull Response remove(long user, long guild) {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE counting_user_ranking SET amount = amount - 1 WHERE id = ? AND guild = ? AND amount > 0;")) {
            stmt.setString(1, String.valueOf(user));
            stmt.setString(2, String.valueOf(guild));
            stmt.execute();
            return Response.SUCCESS;
        } catch (SQLException e) {
            Logs.error("An error occurred while modifying user ranking.", e);
        }
        return Response.FAILURE;
    }

    public int get(long user, long guild) {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT amount FROM counting_user_ranking WHERE id = ? AND guild = ?")) {
            stmt.setString(1, String.valueOf(user));
            stmt.setString(2, String.valueOf(guild));
            ResultSet result = stmt.executeQuery();
            if (result.next()) {
                return result.getInt("amount");
            }
            return 0;
        } catch (SQLException e) {
            Logs.error("An error occurred while modifying user ranking.", e);
        }
        return -1;
    }

    public int getPosition(long user, long guild) {
        //TODO: UserRanking#getPosition
        return -1;
    }

    public @NotNull List<Pair<Long, Integer>> getTop(long guild, @Range(from = 1, to = Integer.MAX_VALUE) int amount) {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, amount FROM counting_user_ranking WHERE guild = ? ORDER BY amount DESC LIMIT ?")) {
            stmt.setString(1, String.valueOf(guild));
            stmt.setInt(2, amount);
            ResultSet resultSet = stmt.executeQuery();
            List<Pair<Long, Integer>> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(new Pair<>(
                        resultSet.getLong("id"),
                        resultSet.getInt("amount")
                ));
            }
            return result;
        } catch (SQLException e) {
            Logs.error("An error occurred while modifying user ranking.", e);
        }
        return new ArrayList<>();
    }
}
