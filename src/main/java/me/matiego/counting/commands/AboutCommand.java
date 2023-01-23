package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

public class AboutCommand implements CommandHandler {
    /**
     * Returns the slash command.
     *
     * @return the slash command
     */
    @Override
    public @NotNull SlashCommandData getCommand() {
        return Commands.slash("about", Translation.COMMANDS__ABOUT__DESCRIPTION.getDefault())
                .setNameLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__ABOUT__NAME.toString()))
                .setDescriptionLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__ABOUT__DESCRIPTION.toString()))
                .addOptions(
                        new OptionData(OptionType.STRING, "ephemeral", Translation.COMMANDS__ABOUT__OPTION__DESCRIPTION.getDefault(), false)
                                .setNameLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__ABOUT__OPTION__NAME.toString()))
                                .setDescriptionLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__ABOUT__OPTION__DESCRIPTION.toString()))
                                .addChoice(Translation.COMMANDS__ABOUT__OPTION__VALUES__TRUE.toString(), "True")
                                .addChoice(Translation.COMMANDS__ABOUT__OPTION__VALUES__FALSE.toString(), "False")
                );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        boolean ephemeral = event.getOption("ephemeral", "False", OptionMapping::getAsString).equals("True");
        if (Main.getInstance().getStorage().getChannel(event.getChannel().getIdLong()) != null) ephemeral = true;
        event.reply(Translation.COMMANDS__ABOUT__MESSAGE.getFormatted(Main.getInstance().getDescription().getVersion()))
                .setEphemeral(ephemeral)
                .queue();
    }
}
