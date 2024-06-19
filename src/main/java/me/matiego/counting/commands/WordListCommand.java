package me.matiego.counting.commands;

import me.matiego.counting.Dictionary;
import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public class WordListCommand extends CommandHandler {
    public WordListCommand(@NotNull Main plugin) {
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
                Translation.COMMANDS__LIST__OPTIONS__LANGUAGE__NAME,
                Translation.COMMANDS__LIST__OPTIONS__LANGUAGE__DESCRIPTION
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
                Translation.COMMANDS__LIST__OPTIONS__WORD__NAME,
                Translation.COMMANDS__LIST__OPTIONS__WORD__DESCRIPTION
        );

        return createSlashCommand("word-list", true, Permission.MANAGE_CHANNEL)
                .addSubcommands(
                        createSubcommand(
                                "add",
                                Translation.COMMANDS__LIST__OPTIONS__ADD__NAME,
                                Translation.COMMANDS__LIST__OPTIONS__ADD__DESCRIPTION
                        ).addOptions(
                                languageOption,
                                wordOption
                        ),
                        createSubcommand(
                                "remove",
                                Translation.COMMANDS__LIST__OPTIONS__REMOVE__NAME,
                                Translation.COMMANDS__LIST__OPTIONS__REMOVE__DESCRIPTION
                        ).addOptions(
                                languageOption,
                                wordOption
                        )
                );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        long time = Utils.now();
        event.deferReply(true).queue();
        InteractionHook hook = event.getHook();
        User user = event.getUser();

        Guild guild = event.getGuild();
        if (guild == null) return;
        long guildId = guild.getIdLong();

        Dictionary.Type type = Dictionary.Type.getByTranslation(event.getOption("language", "null", OptionMapping::getAsString).toUpperCase());
        if (type == null) {
            hook.sendMessage(Translation.GENERAL__UNKNOWN_LANGUAGE.toString()).queue();
            return;
        }

        Dictionary dictionary = plugin.getDictionary();
        String word = event.getOption("word", "null", OptionMapping::getAsString);
        Utils.async(() -> {
            switch (Objects.requireNonNullElse(event.getSubcommandName(), "null")) {
                case "add" -> {
                    if (type.isDictionarySupported() && !dictionary.isWordInDictionary(type, word)) {
                        reply(hook, user, event.getName(), 3 * Utils.SECOND, Translation.COMMANDS__LIST__ADD__NOT_IN_DICTIONARY.toString());
                        return;
                    }
                    switch (dictionary.markWordAsUsed(type, guildId, word)) {
                        case SUCCESS -> {
                            reply(hook, user, event.getName(), 7 * Utils.SECOND, Translation.COMMANDS__LIST__ADD__SUCCESS.getFormatted(Utils.now() - time));
                            Logs.info("User " + Utils.getAsTag(user) + " added the word `" + word + "` to the  `" + type + "` list of used world.");
                        }
                        case NO_CHANGES -> reply(hook, user, event.getName(), 3 * Utils.SECOND, Translation.COMMANDS__LIST__ADD__ALREADY_USED.toString());
                        case FAILURE -> reply(hook, user, event.getName(), 3 * Utils.SECOND, Translation.COMMANDS__LIST__ADD__FAILURE.toString());
                    }
                }
                case "remove" -> {
                    if (plugin.getDictionary().unmarkWordAsUsed(type, guildId, word)) {
                        reply(hook, user, event.getName(), 7 * Utils.SECOND, Translation.COMMANDS__LIST__REMOVE__SUCCESS.getFormatted(Utils.now() - time));
                        Logs.info("User " + Utils.getAsTag(user) + " removed the word `" + word + "` from the  `" + type + "` list of used world.");
                    } else {
                        reply(hook, user, event.getName(), 3 * Utils.SECOND, Translation.COMMANDS__LIST__REMOVE__FAILURE.toString());
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
