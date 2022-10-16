package me.matiego.counting.commands;

import me.matiego.counting.Translation;
import me.matiego.counting.utils.ICommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.jetbrains.annotations.NotNull;

public class FeedbackCommand implements ICommandHandler {
    /**
     * Returns the slash command.
     *
     * @return the slash command
     */
    @Override
    public @NotNull SlashCommandData getCommand() {
        return Commands.slash("feedback", Translation.COMMANDS__FEEDBACK__DESCRIPTION.getDefault())
                .setNameLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__FEEDBACK__NAME.toString()))
                .setDescriptionLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__FEEDBACK__DESCRIPTION.toString()));
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        event.replyModal(
                Modal.create("feedback-modal", Translation.COMMANDS__FEEDBACK__TITLE.toString())
                        .addActionRows(
                                ActionRow.of(TextInput.create("subject", Translation.COMMANDS__FEEDBACK__SUBJECT.toString(), TextInputStyle.SHORT)
                                        .setRequiredRange(10, 100)
                                        .setPlaceholder(Translation.COMMANDS__FEEDBACK__SUBJECT_PLACEHOLDER.toString())
                                        .build()),
                                ActionRow.of(TextInput.create("description", Translation.COMMANDS__FEEDBACK__MODAL_DESCRIPTION.toString(), TextInputStyle.PARAGRAPH)
                                        .setRequiredRange(30, 4000)
                                        .build())
                        )
                        .build()
        ).queue();
    }
}
