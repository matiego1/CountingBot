package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.CommandHandler;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

public class AboutCommand extends CommandHandler {
    public AboutCommand(@NotNull Main plugin) {
        this.plugin = plugin;
    }
    private final Main plugin;    
    
    /**
     * Returns the slash command.
     *
     * @return the slash command
     */
    @Override
    public @NotNull SlashCommandData getCommand() {
        return createSlashCommand("about", false)
                .addOptions(EPHEMERAL_OPTION);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        boolean ephemeral = event.getOption("ephemeral", "False", OptionMapping::getAsString).equals("True");

        if (plugin.getStorage().getChannel(event.getChannel().getIdLong()) != null) ephemeral = true;

        //noinspection deprecation
        event.reply(Translation.COMMANDS__ABOUT__MESSAGE.getFormatted(plugin.getDescription().getVersion()))
                .setEphemeral(ephemeral)
                .queue();
    }
}
