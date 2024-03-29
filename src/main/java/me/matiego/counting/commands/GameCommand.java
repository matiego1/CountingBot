package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.utils.CommandHandler;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;

public class GameCommand extends CommandHandler {
    public GameCommand(@NotNull Main plugin) {
        this.plugin = plugin;
    }
    private final Main plugin;

    @Override
    public @NotNull CommandData getCommand() {
        return createSlashCommand("game", true)
                .addOptions(EPHEMERAL_OPTION);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        boolean ephemeral = event.getOption("ephemeral", "False", OptionMapping::getAsString).equals("True");

        if (plugin.getStorage().getChannel(event.getChannel().getIdLong()) != null) ephemeral = true;

        event.reply("Już wkrótce używając tej komendy będzie można pograć z kimś w jakąś (prostą) grę, np. kółko i krzyżyk, itp. :)").setEphemeral(ephemeral).queue();
    }
}
