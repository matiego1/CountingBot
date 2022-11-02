package me.matiego.counting.commands;

import me.matiego.counting.Translation;
import me.matiego.counting.utils.ICommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

public class PingCommand implements ICommandHandler {
    /**
     * Returns the slash command.
     *
     * @return the slash command
     */
    @Override
    public @NotNull SlashCommandData getCommand() {
        return Commands.slash("ping", Translation.COMMANDS__PING__DESCRIPTION.getDefault())
                .setNameLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__PING__NAME.toString()))
                .setDescriptionLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__PING__DESCRIPTION.toString()))
                .addOptions(
                        new OptionData(OptionType.BOOLEAN, "ephemeral", Translation.COMMANDS__PING__OPTION__DESCRIPTION.getDefault(), false)
                                .setNameLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__PING__OPTION__NAME.toString()))
                                .setDescriptionLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__PING__OPTION__DESCRIPTION.toString()))
                                .addChoice(Translation.COMMANDS__ABOUT__OPTION__VALUES__TRUE.toString(), 1)
                                .addChoice(Translation.COMMANDS__ABOUT__OPTION__VALUES__FALSE.toString(), 0)
                );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        long time = System.currentTimeMillis();
        event.reply("Pong!")
                .setEphemeral(event.getOption("ephemeral", 0, OptionMapping::getAsInt) == 1)
                .flatMap(v -> event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time))
                .queue();
    }
}
