package me.matiego.counting.utils;

import me.matiego.counting.Translation;
import net.dv8tion.jda.api.Permission;
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
import org.jetbrains.annotations.Nullable;

public abstract class CommandHandler {

    public static final OptionData ADMIN_KEY_OPTION = createRequiredStringOption(
            "admin-key",
            Translation.COMMANDS__OPTIONS__ADMIN_KEY__NAME,
            Translation.COMMANDS__OPTIONS__ADMIN_KEY__DESCRIPTION
    );

    public static final OptionData EPHEMERAL_OPTION = createOption(
                    "ephemeral",
                    OptionType.STRING,
                    false,
                    Translation.COMMANDS__OPTIONS__EPHEMERAL__NAME,
                    Translation.COMMANDS__OPTIONS__EPHEMERAL__DESCRIPTION
            )
            .addChoice(Translation.COMMANDS__OPTIONS__EPHEMERAL__TRUE.toString(), "True")
            .addChoice(Translation.COMMANDS__OPTIONS__EPHEMERAL__FALSE.toString(), "False");

    public abstract @NotNull CommandData getCommand();

    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {}
    public void onModalInteraction(@NotNull ModalInteraction event) {}
    public void onStringSelectInteraction(@NotNull StringSelectInteraction event) {}
    public void onMessageContextInteraction(@NotNull MessageContextInteraction event) {}
    public void onUserContextInteraction(@NotNull UserContextInteraction event) {}
    public void onButtonInteraction(@NotNull ButtonInteraction event) {}

    public static @NotNull SlashCommandData createSlashCommand(@NotNull String name, boolean guildOnly, @NotNull Permission... permissions) {
        Translation nameTranslation = getTranslation("COMMANDS__" + name + "__NAME");
        Translation descriptionTranslation = getTranslation("COMMANDS__" + name + "__DESCRIPTION");

        return Commands.slash(name, descriptionTranslation == null ? name : descriptionTranslation.getDefault())
                .setNameLocalizations(Utils.getAllLocalizations(nameTranslation == null ? name : nameTranslation.toString()))
                .setDescriptionLocalizations(Utils.getAllLocalizations(descriptionTranslation == null ? name : descriptionTranslation.toString()))
                .setGuildOnly(guildOnly)
                .setDefaultPermissions(permissions.length == 0 ? DefaultMemberPermissions.ENABLED : DefaultMemberPermissions.enabledFor(permissions));
    }

    public static @NotNull SubcommandData createSubcommand(@NotNull String name, @NotNull Translation nameTranslation, @NotNull Translation descriptionTranslation, @NotNull OptionData... options) {
        return new SubcommandData(name, descriptionTranslation.getDefault())
                .setNameLocalizations(Utils.getAllLocalizations(nameTranslation.toString()))
                .setDescriptionLocalizations(Utils.getAllLocalizations(descriptionTranslation.toString()))
                .addOptions(options);
    }

    public static @NotNull OptionData createRequiredStringOption(@NotNull String name, @NotNull Translation nameTranslation, @NotNull Translation descriptionTranslation) {
        return createOption(name, OptionType.STRING, true, nameTranslation, descriptionTranslation);
    }

    public static @NotNull OptionData createOption(@NotNull String name, @NotNull OptionType type, boolean required,  @NotNull Translation nameTranslation, @NotNull Translation descriptionTranslation) {
        return new OptionData(type, name, descriptionTranslation.getDefault(), required)
                .setNameLocalizations(Utils.getAllLocalizations(nameTranslation.toString()))
                .setDescriptionLocalizations(Utils.getAllLocalizations(descriptionTranslation.toString()));
    }

    private static @Nullable Translation getTranslation(@NotNull String path) {
        try {
            return Translation.valueOf(path);
        } catch (Exception e) {
            return null;
        }
    }
}
