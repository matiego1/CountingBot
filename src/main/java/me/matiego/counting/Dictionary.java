package me.matiego.counting;

import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Response;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Dictionary {
    public enum Type {
        POLISH,
        ENGLISH,
        GERMAN,
        SPANISH
    }
    public @NotNull Response loadDictionaryFromFile(@NotNull File file, @NotNull Type type) {
        if (!file.exists()) return Response.NO_CHANGES;
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("TRUNCATE counting_" + type.toString().toLowerCase())) {
            stmt.execute();
        } catch (SQLException e) {
            Logs.error("An error occurred while loading the dictionary (" + type.toString().toLowerCase() + ").", e);
            return Response.FAILURE;
        }
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("LOAD DATA LOCAL INFILE ? REPLACE INTO TABLE counting_" + type.toString().toLowerCase() + "  COLUMNS TERMINATED BY ','")) {
            stmt.setString(1, file.getAbsolutePath());
            stmt.execute();
            return Response.SUCCESS;
        } catch (SQLException e) {
            Logs.error("An error occurred while loading the dictionary (" + type.toString().toLowerCase() + ").", e);
        }
        return Response.FAILURE;
    }

    public boolean addWord(@NotNull Type type, @NotNull String word) {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO counting_" + type.toString().toLowerCase() + "(word, used) VALUES(?, false) ON DUPLICATE KEY UPDATE word = ?, used = false")) {
            stmt.setString(1, word);
            stmt.setString(2, word);
            stmt.execute();
            return true;
        } catch (SQLException e) {
            Logs.error("An error occurred while adding the word to the dictionary (" + type.toString().toLowerCase() + ").", e);
        }
        return false;
    }

    public boolean removeWord(@NotNull Type type, @NotNull String word) {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM counting_" + type.toString().toLowerCase() + " WHERE word = ?")) {
            stmt.setString(1, word);
            stmt.execute();
            return true;
        } catch (SQLException e) {
            Logs.error("An error occurred while removing the word from the dictionary (" + type.toString().toLowerCase() + ").", e);
        }
        return false;
    }

    public @NotNull Response useWord(@NotNull Type type, @NotNull String word) {
        if (type == Type.POLISH && word.equalsIgnoreCase("yeti")) return Response.SUCCESS;
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE counting_" + type.toString().toLowerCase() + " SET used = true WHERE word = ? AND used = false")) {
            stmt.setString(1, word);
            return stmt.executeUpdate() > 0 ? Response.SUCCESS : Response.NO_CHANGES;
        } catch (SQLException e) {
            Logs.error("An error occurred while modifying the dictionary (" + type.toString().toLowerCase() + ").", e);
        }
        return Response.FAILURE;
    }
}