package me.matiego.counting;

import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Dictionary {
    private static final int ER_DUP_ENTRY = 1062;

    /**
     * Loads the dictionary from the file.
     * <b>Execution of this method may block the thread for a long time!</b>
     * @param file the file
     * @param type the type of the dictionary
     * @return {@code Response.SUCCESS} if a file was loaded successfully, {@code Response.NO_CHANGES} if the file does not exist, {@code Response.FAILURE} if an error occurred.
     */
    public @NotNull Response loadDictionaryFromFile(@NotNull File file, @NotNull Type type) {
        if (!type.isDictionarySupported()) return Response.FAILURE;
        if (!file.exists()) return Response.NO_CHANGES;

        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("TRUNCATE counting_" + type + "_dict")) {
            stmt.execute();
        } catch (SQLException e) {
            Logs.error("An error occurred while loading the dictionary (" + type + ").", e);
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
            Logs.error("An error occurred while loading the dictionary (" + type + ").", e);
        }
        return Response.FAILURE;
    }

    /**
     * Adds a word to the dictionary
     * @param type the type of the dictionary
     * @param word the word
     * @return {@code true} if the word was added successfully otherwise {@code false}
     */
    public boolean addWordToDictionary(@NotNull Type type, @NotNull String word) {
        if (!type.isDictionarySupported()) return false;
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO counting_" + type + "_dict(word) VALUES(?) ON DUPLICATE KEY UPDATE word = ?")) {
            stmt.setString(1, word);
            stmt.setString(2, word);
            stmt.execute();
            return true;
        } catch (SQLException e) {
            Logs.error("An error occurred while adding the word to the dictionary (" + type + ").", e);
        }
        return false;
    }

    /**
     * Removes a word from the dictionary
     * @param type the type of the dictionary
     * @param word the word
     * @return {@code true} if the word was removed successfully otherwise {@code false}
     */
    public boolean removeWordFromDictionary(@NotNull Type type, @NotNull String word) {
        if (!type.isDictionarySupported()) return false;
        try (Connection conn = Main.getInstance().getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM counting_" + type + "_dict WHERE word = ?")) {
            stmt.setString(1, word);
            stmt.execute();
            return true;
        } catch (SQLException e) {
            Logs.error("An error occurred while removing the word from the dictionary (" + type + ").", e);
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
            Logs.error("An error occurred while removing the word from the dictionary (" + type + ").", e);
        }
        return false;
    }

    /**
     * Marks a word as used.
     * @param type the type of the dictionary
     * @param word the word
     * @return {@code Response.SUCCESS} if the word was marked successfully, {@code Response.NO_CHANGES} if the word has already been marked or {@code Response.FAILURE} if an error occurred
     */
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
            Logs.error("An error occurred while modifying the dictionary (" + type + ").", e);
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
            Logs.error("An error occurred while modifying the dictionary (" + type + ").", e);
        }
        return false;
    }

    public enum Type {
        POLISH,
        ENGLISH,
        GERMAN,
        SPANISH,
        TAUTOLOGIES(false),
        MINECRAFT_ITEM(false);

        Type() {
            this(true);
        }
        Type(boolean dictionary) {
            this.dictionary = dictionary;
        }

        private final boolean dictionary;
        public boolean isDictionarySupported() {
            return dictionary;
        }

        public @NotNull String getTranslation() {
            try {
                return Translation.valueOf("COMMANDS__DICTIONARY__TYPES__" + name()).toString();
            } catch (IllegalArgumentException ignored) {}
            return "COMMANDS__DICTIONARY__TYPES__" + name();
        }

        static public @Nullable Type getByTranslation(@NotNull String string) {
            for (Type type : values()) {
                if (type.getTranslation().equalsIgnoreCase(string)) {
                    return type;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
