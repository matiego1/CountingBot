package me.matiego.counting.commands;

import me.matiego.counting.Dictionary;
import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.*;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

public class DictionaryCommand implements CommandHandler {
    private final Main plugin;

    public DictionaryCommand(@NotNull Main plugin) {
        this.plugin = plugin;
    }


    /**
     * Returns the slash command.
     *
     * @return the slash command
     */
    @Override
    public @NotNull SlashCommandData getCommand() {
        OptionData languageOption =
                getOption(
                        "language",
                        Translation.COMMANDS__DICTIONARY__OPTIONS__LANGUAGE__NAME,
                        Translation.COMMANDS__DICTIONARY__OPTIONS__LANGUAGE__DESCRIPTION)
                .addChoices(
                        Arrays.stream(Dictionary.Type.values())
                                .map(Dictionary.Type::getTranslation)
                                .map(value -> new Command.Choice(value, value))
                                .toList()
                );
        OptionData wordOption =
                getOption(
                        "word", Translation.COMMANDS__DICTIONARY__OPTIONS__WORD__NAME,
                        Translation.COMMANDS__DICTIONARY__OPTIONS__WORD__DESCRIPTION
                );
        OptionData adminKeyOption =
                getOption(
                        "admin-key",
                        Translation.COMMANDS__DICTIONARY__OPTIONS__ADMIN_KEY__NAME,
                        Translation.COMMANDS__DICTIONARY__OPTIONS__ADMIN_KEY__DESCRIPTION
                );

        return Commands.slash("dictionary", Translation.COMMANDS__DICTIONARY__DESCRIPTION.getDefault())
                .setNameLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__DICTIONARY__NAME.toString()))
                .setDescriptionLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__DICTIONARY__DESCRIPTION.toString()))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
                .setGuildOnly(true)
                .addSubcommands(
                        getSubcommand(
                                "add",
                                Translation.COMMANDS__DICTIONARY__OPTIONS__ADD__NAME,
                                Translation.COMMANDS__DICTIONARY__OPTIONS__ADD__DESCRIPTION,
                                languageOption,
                                adminKeyOption,
                                wordOption
                        ),
                        getSubcommand(
                                "remove",
                                Translation.COMMANDS__DICTIONARY__OPTIONS__REMOVE__NAME,
                                Translation.COMMANDS__DICTIONARY__OPTIONS__REMOVE__DESCRIPTION,
                                languageOption,
                                adminKeyOption,
                                wordOption
                        ),
                        getSubcommand(
                                "load",
                                Translation.COMMANDS__DICTIONARY__OPTIONS__LOAD__NAME,
                                Translation.COMMANDS__DICTIONARY__OPTIONS__LOAD__DESCRIPTION,
                                languageOption,
                                adminKeyOption,
                                getOption(
                                        "file",
                                        Translation.COMMANDS__DICTIONARY__OPTIONS__FILE__NAME,
                                        Translation.COMMANDS__DICTIONARY__OPTIONS__FILE__DESCRIPTION
                                )
                        )
                );
    }

    private @NotNull OptionData getOption(@NotNull String name, @NotNull Translation nameLoc, @NotNull Translation descriptionLoc) {
        return new OptionData(OptionType.STRING, name, descriptionLoc.getDefault(), true)
                .setNameLocalizations(Utils.getAllLocalizations(nameLoc.toString()))
                .setDescriptionLocalizations(Utils.getAllLocalizations(descriptionLoc.toString()));
    }

    private @NotNull SubcommandData getSubcommand(@NotNull String name, @NotNull Translation nameLoc, @NotNull Translation descriptionLoc, @NotNull OptionData... options) {
        return new SubcommandData(name, descriptionLoc.getDefault())
                .setNameLocalizations(Utils.getAllLocalizations(nameLoc.toString()))
                .setDescriptionLocalizations(Utils.getAllLocalizations(descriptionLoc.toString()))
                .addOptions(options);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        long time = System.currentTimeMillis();
        event.deferReply(true).queue();
        InteractionHook hook = event.getHook();
        User user = event.getUser();


        if (!event.getOption("admin-key", "", OptionMapping::getAsString).equals(plugin.getConfig().getString("admin-key"))) {
            reply(hook, user, event.getName(), 3 * Utils.SECOND, Translation.GENERAL__INCORRECT_ADMIN_KEY.toString());
            return;
        }

        Dictionary.Type type = Dictionary.Type.getByTranslation(event.getOption("language", "null", OptionMapping::getAsString).toUpperCase());
        if (type == null) {
            hook.sendMessage(Translation.GENERAL__UNKNOWN_LANGUAGE.toString()).queue();
            return;
        }

        Utils.async(() -> {
            switch (Objects.requireNonNullElse(event.getSubcommandName(), "null")) {
                case "add" -> {
                    if (plugin.getDictionary().addWord(type, event.getOption("word", "null", OptionMapping::getAsString))) {
                        reply(hook, user, event.getName(), 7 * Utils.SECOND, Translation.COMMANDS__DICTIONARY__ADD__SUCCESS.getFormatted(System.currentTimeMillis() - time));
                    } else {
                        reply(hook, user, event.getName(), 3 * Utils.SECOND, Translation.COMMANDS__DICTIONARY__ADD__FAILURE.toString());
                    }
                }
                case "remove" -> {
                    if (plugin.getDictionary().removeWord(type, event.getOption("word", "null", OptionMapping::getAsString))) {
                        reply(hook, user, event.getName(), 7 * Utils.SECOND, Translation.COMMANDS__DICTIONARY__REMOVE__SUCCESS.getFormatted(System.currentTimeMillis() - time));
                    } else {
                        reply(hook, user, event.getName(), 3 * Utils.SECOND, Translation.COMMANDS__DICTIONARY__REMOVE__FAILURE.toString());
                    }
                }
                case "load" -> {
                    switch (plugin.getDictionary().loadDictionaryFromFile(new File(plugin.getDataFolder() + File.separator + event.getOption("file", "null", OptionMapping::getAsString)), type)) {
                        case SUCCESS -> reply(hook, user, event.getName(), 5 * 60 * Utils.SECOND, Translation.COMMANDS__DICTIONARY__LOAD__SUCCESS.getFormatted(System.currentTimeMillis() - time));
                        case NO_CHANGES -> reply(hook, user, event.getName(), 5 * Utils.SECOND, Translation.COMMANDS__DICTIONARY__LOAD__NO_CHANGES.getFormatted(System.currentTimeMillis() - time));
                        case FAILURE -> reply(hook, user, event.getName(), 30 * Utils.SECOND, Translation.COMMANDS__DICTIONARY__LOAD__FAILURE.getFormatted(System.currentTimeMillis() - time));
                    }
                }
            }
        });
    }

    private void reply(@NotNull InteractionHook hook, @NotNull User user, @NotNull String command, long time, @NotNull String message) {
        hook.sendMessage(message).queue();
        plugin.getCommandHandler().putSlowdown(user, command, time);
    }
}
