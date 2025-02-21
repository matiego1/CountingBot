package me.matiego.counting;

import lombok.Getter;
import me.matiego.counting.utils.Logs;
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
    public boolean add(@NotNull UserSnowflake user, long guild) {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO counting_user_ranking(id, guild, score) VALUES(?, ?, 1) ON DUPLICATE KEY UPDATE id = id, guild = guild, score = score + 1")) {
            stmt.setString(1, user.getId());
            stmt.setString(2, String.valueOf(guild));
            stmt.execute();
            return true;
        } catch (SQLException e) {
            Logs.error("Failed to increase the user's ranking.", e);
        }
        return false;
    }

    // TODO: add implementation
    public boolean remove(@NotNull UserSnowflake user, long guild) {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE counting_user_ranking SET score = score - 1 WHERE id = ? AND guild = ? AND score > 0")) {
            stmt.setString(1, user.getId());
            stmt.setString(2, String.valueOf(guild));
            stmt.execute();
            return true;
        } catch (SQLException e) {
            Logs.error("Failed to decrease the user's ranking.", e);
        }
        return false;
    }

    public @Nullable Data get(@NotNull UserSnowflake user, long guild) {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT score, (SELECT 1 + COUNT(*) FROM counting_user_ranking WHERE score > x.score AND guild = ?) AS pos FROM counting_user_ranking x WHERE id = ? AND guild = ?")) {
            stmt.setString(1, String.valueOf(guild));
            stmt.setString(2, user.getId());
            stmt.setString(3, String.valueOf(guild));
            ResultSet result = stmt.executeQuery();
            if (result.next()) {
                int score = result.getInt("score");
                int rank = result.getInt("pos");
                if (score == 0 || rank == 0) return null;
                return new Data(user, score, rank);
            }
            return null;
        } catch (SQLException e) {
            Logs.error("Failed to get the user's ranking.", e);
        }
        return null;
    }

    public @NotNull List<Data> getTop(long guild, @Range(from = 1, to = Integer.MAX_VALUE) int amount) {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, score, RANK() OVER(ORDER BY score DESC) pos, (SELECT SUM(score) FROM counting_user_ranking) total, (SELECT SUM(score) FROM counting_user_ranking WHERE guild = ?) total_guild FROM counting_user_ranking WHERE guild = ?;")) {
            stmt.setString(1, String.valueOf(guild));
            stmt.setString(2, String.valueOf(guild));

            int total = 0, total_guild = 0;

            ResultSet resultSet = stmt.executeQuery();
            List<Data> result = new ArrayList<>();
            while (resultSet.next()) {
                total = resultSet.getInt("total");
                total_guild = resultSet.getInt("total_guild");

                int rank = resultSet.getInt("pos");
                if (rank > amount) {
                    result.add(new Data(
                            UserSnowflake.fromId(0),
                            total,
                            total_guild
                    ));
                    return result;
                }

                result.add(new Data(
                        UserSnowflake.fromId(resultSet.getLong("id")),
                        resultSet.getInt("score"),
                        rank
                ));
            }

            result.add(new Data(
                    UserSnowflake.fromId(0),
                    total,
                    total_guild
            ));

            return result;
        } catch (SQLException e) {
            Logs.error("Failed to get the top users from the ranking.", e);
        }
        return new ArrayList<>();
    }

    @Getter
    public static class Data {
        private final UserSnowflake user;
        private final int score;
        private final int rank;

        public Data(@NotNull UserSnowflake user, int score, int rank) {
            this.user = user;
            this.score = score;
            this.rank = rank;
        }
    }
}
