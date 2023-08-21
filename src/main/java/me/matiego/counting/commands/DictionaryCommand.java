package me.matiego.counting.commands;

import me.matiego.counting.Dictionary;
import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

public class DictionaryCommand extends CommandHandler {
    public DictionaryCommand(@NotNull Main plugin) {
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
        OptionData languageOption = createOption(
                "language",
                OptionType.STRING,
                true,
                Translation.COMMANDS__DICTIONARY__OPTIONS__LANGUAGE__NAME,
                Translation.COMMANDS__DICTIONARY__OPTIONS__LANGUAGE__DESCRIPTION
        ).addChoices(
                Arrays.stream(Dictionary.Type.values())
                        .map(Dictionary.Type::getTranslation)
                        .map(value -> new Command.Choice(value, value))
                        .toList()
        );
        OptionData wordOption = createOption(
                "word",
                OptionType.STRING,
                true,
                Translation.COMMANDS__DICTIONARY__OPTIONS__WORD__NAME,
                Translation.COMMANDS__DICTIONARY__OPTIONS__WORD__DESCRIPTION
        );

        return createSlashCommand("dictionary", true, Permission.MANAGE_CHANNEL)
                .addSubcommands(
                        createSubcommand(
                                "add",
                                Translation.COMMANDS__DICTIONARY__OPTIONS__ADD__NAME,
                                Translation.COMMANDS__DICTIONARY__OPTIONS__ADD__DESCRIPTION
                        ).addOptions(
                                languageOption,
                                ADMIN_KEY_OPTION,
                                wordOption
                        ),
                        createSubcommand(
                                "remove",
                                Translation.COMMANDS__DICTIONARY__OPTIONS__REMOVE__NAME,
                                Translation.COMMANDS__DICTIONARY__OPTIONS__REMOVE__DESCRIPTION
                        ).addOptions(
                                languageOption,
                                ADMIN_KEY_OPTION,
                                wordOption
                        ),
                        createSubcommand(
                                "load",
                                Translation.COMMANDS__DICTIONARY__OPTIONS__LOAD__NAME,
                                Translation.COMMANDS__DICTIONARY__OPTIONS__LOAD__DESCRIPTION
                        ).addOptions(
                                languageOption,
                                ADMIN_KEY_OPTION,
                                createOption(
                                        "file",
                                        OptionType.STRING,
                                        true,
                                        Translation.COMMANDS__DICTIONARY__OPTIONS__FILE__NAME,
                                        Translation.COMMANDS__DICTIONARY__OPTIONS__FILE__DESCRIPTION
                                )
                        )
                );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        long time = Utils.now();
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
                    if (plugin.getDictionary().addWordToDictionary(type, event.getOption("word", "null", OptionMapping::getAsString))) {
                        reply(hook, user, event.getName(), 7 * Utils.SECOND, Translation.COMMANDS__DICTIONARY__ADD__SUCCESS.getFormatted(Utils.now() - time));
                    } else {
                        reply(hook, user, event.getName(), 3 * Utils.SECOND, Translation.COMMANDS__DICTIONARY__ADD__FAILURE.toString());
                    }
                }
                case "remove" -> {
                    if (plugin.getDictionary().removeWordFromDictionary(type, event.getOption("word", "null", OptionMapping::getAsString))) {
                        reply(hook, user, event.getName(), 7 * Utils.SECOND, Translation.COMMANDS__DICTIONARY__REMOVE__SUCCESS.getFormatted(Utils.now() - time));
                    } else {
                        reply(hook, user, event.getName(), 3 * Utils.SECOND, Translation.COMMANDS__DICTIONARY__REMOVE__FAILURE.toString());
                    }
                }
                case "load" -> {
                    switch (plugin.getDictionary().loadDictionaryFromFile(new File(plugin.getDataFolder() + File.separator + event.getOption("file", "null", OptionMapping::getAsString)), type)) {
                        case SUCCESS -> reply(hook, user, event.getName(), 5 * 60 * Utils.SECOND, Translation.COMMANDS__DICTIONARY__LOAD__SUCCESS.getFormatted(Utils.now() - time));
                        case NO_CHANGES -> reply(hook, user, event.getName(), 5 * Utils.SECOND, Translation.COMMANDS__DICTIONARY__LOAD__NO_CHANGES.getFormatted(Utils.now() - time));
                        case FAILURE -> reply(hook, user, event.getName(), 30 * Utils.SECOND, Translation.COMMANDS__DICTIONARY__LOAD__FAILURE.getFormatted(Utils.now() - time));
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
