package me.matiego.counting.utils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.*;
import net.dv8tion.jda.api.interactions.commands.context.MessageContextInteraction;
import net.dv8tion.jda.api.interactions.commands.context.UserContextInteraction;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectInteraction;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class CommandHandler {

    public static final OptionData ADMIN_KEY_OPTION = createOption(
            "admin-key",
            "The secret administrator key",
            OptionType.STRING,
            true
    );

    public static final OptionData ADMIN_KEY_OPTION_NOT_REQUIRED = createOption(
            "admin-key",
            "The secret administrator key",
            OptionType.STRING,
            false
    );

    public static final OptionData EPHEMERAL_OPTION = createOption(
            "ephemeral",
            "Czy wiadomość ma być widoczna tylko dla ciebie",
            OptionType.STRING,
            false
    )
            .addChoice("Tak, chcę żebym tylko ja widział tę wiadomość", "True")
            .addChoice("Nie, chcę żeby wszyscy widzieli tę wiadomość", "False");

    public abstract @NotNull CommandData getCommand();

    public @NotNull CompletableFuture<Integer> onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        return CompletableFuture.completedFuture(0);
    }
    public void onModalInteraction(@NotNull ModalInteraction event) {}
    public void onStringSelectInteraction(@NotNull StringSelectInteraction event) {}
    public void onMessageContextInteraction(@NotNull MessageContextInteraction event) {}
    public void onUserContextInteraction(@NotNull UserContextInteraction event) {}
    public void onButtonInteraction(@NotNull ButtonInteraction event) {}

    public static @NotNull SlashCommandData createSlashCommand(@NotNull String name, @NotNull String description, boolean guildOnly, @NotNull Permission... permissions) {
        return Commands.slash(name, description)
                .setContexts(guildOnly ? InteractionContextType.GUILD : InteractionContextType.BOT_DM)
                .setDefaultPermissions(permissions.length == 0 ? DefaultMemberPermissions.ENABLED : DefaultMemberPermissions.enabledFor(permissions));
    }

    public static @NotNull SubcommandData createSubcommand(@NotNull String name, @NotNull String description, @NotNull OptionData... options) {
        return new SubcommandData(name, description)
                .addOptions(options);
    }

    public static @NotNull OptionData createOption(@NotNull String name, @NotNull String description, @NotNull OptionType type, boolean required) {
        return new OptionData(type, name, description, required);
    }
}
