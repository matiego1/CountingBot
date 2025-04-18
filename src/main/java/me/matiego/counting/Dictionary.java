package me.matiego.counting;

import lombok.Getter;
import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

public class Dictionary {
    private static final int ER_DUP_ENTRY = 1062;

    public @NotNull Response loadDictionaryFromFile(@NotNull File file, @NotNull Type type, @NotNull Runnable afterChecks) {
        if (!type.isDictionarySupported()) return Response.FAILURE;
        if (!file.exists()) return Response.NO_CHANGES;

        afterChecks.run();

        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("TRUNCATE counting_" + type + "_dict")) {
            stmt.execute();
        } catch (SQLException e) {
            Logs.error("Failed to load the dictionary from file (" + type + ").", e);
            return Response.FAILURE;
        }

        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("SET GLOBAL local_infile=1")) {
            stmt.execute();
        } catch (SQLException ignored) {} //maybe it will work without it

        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("LOAD DATA LOCAL INFILE ? REPLACE INTO TABLE counting_" + type + "_dict COLUMNS TERMINATED BY ','")) {
            stmt.setString(1, file.getAbsolutePath());
            stmt.execute();
            return Response.SUCCESS;
        } catch (SQLException e) {
            Logs.error("Failed to load the dictionary from file (" + type + ").", e);
        }
        return Response.FAILURE;
    }

    public boolean addWordToDictionary(@NotNull Type type, @NotNull String word) {
        if (!type.isDictionarySupported()) return false;
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO counting_" + type + "_dict(word) VALUES(?) ON DUPLICATE KEY UPDATE word = ?")) {
            stmt.setString(1, word);
            stmt.setString(2, word);
            stmt.execute();
            return true;
        } catch (SQLException e) {
            Logs.error("Failed to add word to the dictionary (" + type + ").", e);
        }
        return false;
    }

    public boolean removeWordFromDictionary(@NotNull Type type, @NotNull String word) {
        if (!type.isDictionarySupported()) return false;
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM counting_" + type + "_dict WHERE word = ?")) {
            stmt.setString(1, word);
            stmt.execute();
            return true;
        } catch (SQLException e) {
            Logs.error("Failed to remove word from the dictionary (" + type + ").", e);
        }
        return false;
    }

    public boolean isWordInDictionary(@NotNull Type type, @NotNull String word) {
        if (!type.isDictionarySupported()) return false;
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT word FROM counting_" + type + "_dict WHERE word = ?")) {
            stmt.setString(1, word);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            Logs.error("Failed to check if word exists in the dictionary (" + type + ").", e);
        }
        return false;
    }

    public @NotNull Response markWordAsUsed(@NotNull Type type, long guildId, @NotNull String word) {
        if (type == Type.POLISH && word.equalsIgnoreCase("yeti")) return Response.SUCCESS;
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO counting_" + type + "_list(word, guild) VALUES(?, ?)")) {
            stmt.setString(1, word);
            stmt.setString(2, String.valueOf(guildId));
            stmt.execute();
            return Response.SUCCESS;
        } catch (SQLException e) {
            if (e.getErrorCode() == ER_DUP_ENTRY) return Response.NO_CHANGES;
            Logs.error("Failed to mark word as used (" + type + ").", e);
        }
        return Response.FAILURE;
    }

    public boolean unmarkWordAsUsed(@NotNull Type type, long guildId, @NotNull String word) {
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM counting_" + type + "_list WHERE word = ? AND guild = ?")) {
            stmt.setString(1, word);
            stmt.setString(2, String.valueOf(guildId));
            stmt.execute();
            return true;
        } catch (SQLException e) {
            Logs.error("Failed to unmark word as used (" + type + ").", e);
        }
        return false;
    }

    @Getter
    public enum Type {
        POLISH,
        ENGLISH,
        GERMAN,
        SPANISH,
        TAUTOLOGIES(false),
        MINECRAFT_ITEM;

        Type(boolean dictionarySupported) {
            this.dictionarySupported = dictionarySupported;
        }
        Type() {
            this(true);
        }

        private final boolean dictionarySupported;

        public static @Nullable Type getByString(@NotNull String string) {
            return Arrays.stream(values())
                    .filter(v -> v.toString().equalsIgnoreCase(string))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
