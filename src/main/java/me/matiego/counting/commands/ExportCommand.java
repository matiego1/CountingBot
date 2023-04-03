package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class ExportCommand implements CommandHandler {
    public ExportCommand(@NotNull Main plugin) {
        this.plugin = plugin;
    }
    private final Main plugin;

    /**
     * Returns the slash command.
     *
     * @return the slash command
     */
    @Override
    public @NotNull CommandData getCommand() {
        return Commands.slash("export", "Exports the database table to file")
                .setNameLocalizations(Utils.getAllLocalizations("export"))
                .setDescriptionLocalizations(Utils.getAllLocalizations("Exports the database tables to files"))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .setGuildOnly(true)
                .addOptions(
                        new OptionData(OptionType.STRING, "table", "The table name", true)
                                .setNameLocalizations(Utils.getAllLocalizations("table"))
                                .setDescriptionLocalizations(Utils.getAllLocalizations("The table name")),
                        new OptionData(OptionType.STRING, "admin-key", "The secret administrator key", true)
                                .setNameLocalizations(Utils.getAllLocalizations("admin-key"))
                                .setDescriptionLocalizations(Utils.getAllLocalizations("The secret administrator key"))
                );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        event.deferReply(true).queue();
        InteractionHook hook = event.getHook();

        String table = event.getOption("table", OptionMapping::getAsString);

        if (!event.getOption("admin-key", "", OptionMapping::getAsString).equals(plugin.getConfig().getString("admin-key"))) {
            hook.sendMessage("Incorrect administrator key!").queue();
            return;
        }

        Utils.async(() -> {
            if (table == null) {
                hook.sendMessage("Unknown table.").queue();
                return;
            }

            File file = exportTable(table);
            if (file == null) {
                hook.sendMessage("An error occurred. Check the console to more information.").queue();
                return;
            }

            hook.sendMessage("The file is saved to disk in the following location: `" + file.getAbsolutePath() + "`").queue();
        });
    }

    private @Nullable File exportTable(@NotNull String tableName) {
        try (Connection conn = plugin.getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM counting_" + tableName)) {
            return writeToFile(tableName, stmt.executeQuery());
        } catch (SQLException e) {
            Logs.error("An error occurred while exporting a table", e);
        }
        return null;
    }

    private @Nullable File writeToFile(@NotNull String tableName, @NotNull ResultSet result) {
        String path = plugin.getDataFolder().getAbsolutePath() + File.separator + "export" + File.separator + "export_" + tableName + "_" + (Utils.now() / 1000) + "_" + RandomStringUtils.randomAlphabetic(4) + ".csv";

        File file = new File(path);

        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8)) {
            while (result.next()) {
                int columns = result.getMetaData().getColumnCount();
                for (int i = 1; i < columns; i++) {
                    writer.print(parseObject(result.getObject(i)) + ",");
                }
                writer.println(parseObject(result.getObject(columns)));
            }
            return new File(path);
        } catch (Exception e) {
            Logs.error("An error occurred while exporting a table", e);
        }
        return null;
    }

    private @NotNull String parseObject(@Nullable Object object) {
        if (object == null) return "\"null\"";
        if (object instanceof Number number) return number.toString();
        if (object instanceof Boolean bool) return bool ? "1" : "0";
        return "\"" + object + "\"";
    }
}