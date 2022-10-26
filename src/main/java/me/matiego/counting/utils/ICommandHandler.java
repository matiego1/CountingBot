package me.matiego.counting.utils;

import net.dv8tion.jda.api.interactions.ModalInteraction;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.context.MessageContextInteraction;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenuInteraction;
import org.jetbrains.annotations.NotNull;

public interface ICommandHandler {
    /**
     * Returns the slash command.
     * @return the slash command
     */
    @NotNull CommandData getCommand();

    default void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {}

    default void onModalInteraction(@NotNull ModalInteraction event) {}

    default void onSelectMenuInteraction(@NotNull SelectMenuInteraction event) {}

    default void onMessageContextInteraction(@NotNull MessageContextInteraction event) {}
}
