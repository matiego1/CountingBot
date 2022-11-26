package me.matiego.counting;

import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Response;
import net.dv8tion.jda.api.entities.UserSnowflake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserRanking {

    public @NotNull Response add(@NotNull UserSnowflake user, long guild) {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO counting_user_ranking(id, guild, score) VALUES(?, ?, 1) ON DUPLICATE KEY UPDATE id = id, guild = guild, score = score + 1")) {
            stmt.setString(1, user.getId());
            stmt.setString(2, String.valueOf(guild));
            stmt.execute();
            return Response.SUCCESS;
        } catch (SQLException e) {
            Logs.error("An error occurred while modifying user ranking.", e);
        }
        return Response.FAILURE;
    }

    public @NotNull Response remove(@NotNull UserSnowflake user, long guild) {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE counting_user_ranking SET score = score - 1 WHERE id = ? AND guild = ? AND score > 0;")) {
            stmt.setString(1, user.getId());
            stmt.setString(2, String.valueOf(guild));
            stmt.execute();
            return Response.SUCCESS;
        } catch (SQLException e) {
            Logs.error("An error occurred while modifying user ranking.", e);
        }
        return Response.FAILURE;
    }

    public @Nullable Data get(@NotNull UserSnowflake user, long guild) {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT score, 1 + COUNT(*) AS pos FROM counting_user_ranking WHERE score > (SELECT score FROM counting_user_ranking WHERE id = ? AND GUILD = ?)")) {
            stmt.setString(1, user.getId());
            stmt.setString(2, String.valueOf(guild));
            ResultSet result = stmt.executeQuery();
            if (result.next()) {
                return new Data(
                        user,
                        result.getInt("score"),
                        result.getInt("pos")
                );
            }
            return null;
        } catch (SQLException e) {
            Logs.error("An error occurred while modifying user ranking.", e);
        }
        return null;
    }

    public @NotNull List<Data> getTop(long guild, @Range(from = 1, to = Integer.MAX_VALUE) int amount) {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, score, RANK() OVER(ORDER BY score DESC) pos FROM ranking WHERE guild = ?")) {
            stmt.setString(1, String.valueOf(guild));
            ResultSet resultSet = stmt.executeQuery();
            List<Data> result = new ArrayList<>();
            while (resultSet.next()) {
                int rank = resultSet.getInt("pos");
                if (rank > amount) return result;
                result.add(new Data(
                        UserSnowflake.fromId(resultSet.getLong("id")),
                        resultSet.getInt("score"),
                        rank
                ));
            }
            return result;
        } catch (SQLException e) {
            Logs.error("An error occurred while modifying user ranking.", e);
        }
        return new ArrayList<>();
    }

    public static class Data {
        private final UserSnowflake user;
        private final int score;
        private final int rank;

        public Data(@NotNull UserSnowflake user, int score, int rank) {
            this.user = user;
            this.score = score;
            this.rank = rank;
        }

        public @NotNull UserSnowflake getUser() {
            return user;
        }
        public int getScore() {
            return score;
        }
        public int getRank() {
            return rank;
        }
    }
}
