package me.matiego.counting.commands;

import me.matiego.counting.Dictionary;
import me.matiego.counting.Main;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.DiscordUtils;
import me.matiego.counting.utils.Logs;
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
import java.util.concurrent.CompletableFuture;

public class DictionaryCommand extends CommandHandler {
    public DictionaryCommand(@NotNull Main instance) {
        this.instance = instance;
    }
    private final Main instance;

    @Override
    public @NotNull SlashCommandData getCommand() {
        OptionData languageOption = createOption(
                "language",
                "The dictionary type",
                OptionType.STRING,
                true
        ).addChoices(
                Arrays.stream(Dictionary.Type.values())
                        .map(Dictionary.Type::toString)
                        .map(value -> new Command.Choice(value, value))
                        .toList()
        );
        OptionData wordOption = createOption(
                "word",
                "The word to add/remove",
                OptionType.STRING,
                true
        );

        return createSlashCommand(
                "dictionary",
                "Manages dictionaries",
                true,
                Permission.ADMINISTRATOR
        ).addSubcommands(
                createSubcommand(
                        "add",
                        "Adds a word to the dictionary",
                        languageOption,
                        ADMIN_KEY_OPTION,
                        wordOption
                ),
                createSubcommand(
                        "remove",
                        "Removes a word from the dictionary",
                        languageOption,
                        ADMIN_KEY_OPTION,
                        wordOption
                ),
                createSubcommand(
                        "load",
                        "Loads the dictionary from a file",
                        languageOption,
                        ADMIN_KEY_OPTION,
                        createOption(
                                "file",
                                "The file's path",
                                OptionType.STRING,
                                true
                        )
                )
        );
    }

    @Override
    public @NotNull CompletableFuture<Integer> onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        long time = Utils.now();
        User user = event.getUser();

        event.deferReply(true).queue();
        InteractionHook hook = event.getHook();

        if (!Utils.checkAdminKey(event.getOption("admin-key", OptionMapping::getAsString), user)) {
            hook.sendMessage("Incorrect administrator key!").queue();
            return CompletableFuture.completedFuture(3);
        }

        Dictionary.Type type = Dictionary.Type.getByString(event.getOption("language", "null", OptionMapping::getAsString).toUpperCase());
        if (type == null) {
            hook.sendMessage("Unknown dictionary type.").queue();
            return CompletableFuture.completedFuture(3);
        }

        String word = event.getOption("word", "null", OptionMapping::getAsString);
        CompletableFuture<Integer> cooldown = new CompletableFuture<>();

        Utils.async(() -> {
            switch (String.valueOf(event.getSubcommandName())) {
                case "add" -> {
                    if (instance.getDictionary().addWordToDictionary(type, word)) {
                        hook.sendMessage("Successfully added this word to the dictionary. (%s ms)".formatted(Utils.now() - time)).queue();
                        cooldown.complete(5);
                        Logs.info(DiscordUtils.getAsTag(user) + " added the word `" + word + "` to the `" + type + "` dictionary.");
                    } else {
                        hook.sendMessage("Failed to add this word to the dictionary.").queue();
                        cooldown.complete(5);
                    }
                }
                case "remove" -> {
                    if (instance.getDictionary().removeWordFromDictionary(type, event.getOption("word", "null", OptionMapping::getAsString))) {
                        hook.sendMessage("Successfully removed this word from the dictionary. (%s ms)".formatted(Utils.now() - time)).queue();
                        cooldown.complete(5);
                        Logs.info(DiscordUtils.getAsTag(user) + " removed the word `" + word + "` from the `" + type + "` dictionary.");
                    } else {
                        hook.sendMessage("Failed to remove this word from the dictionary.").queue();
                        cooldown.complete(5);
                    }
                }
                case "load" -> {
                    File file = new File(instance.getDataFolder() + File.separator + event.getOption("file", OptionMapping::getAsString));
                    Logs.info(DiscordUtils.getAsTag(user) + " started loading a new `" + type + "` dictionary from file `" + file + "`.");
                    switch (instance.getDictionary().loadDictionaryFromFile(file, type, () -> cooldown.complete(30))) {
                        case SUCCESS -> {
                            hook.sendMessage("Successfully loaded the dictionary from this file. (%s ms)".formatted(Utils.now() - time)).queue();
                            Logs.info(DiscordUtils.getAsTag(user) + " finished loading a new `" + type + "` dictionary from file `" + file + "` - **Success**");
                        }
                        case NO_CHANGES -> {
                            hook.sendMessage("This file doesn't exist. (%s ms)".formatted(Utils.now() - time)).queue();
                            Logs.info(DiscordUtils.getAsTag(user) + " finished loading a new `" + type + "` dictionary from file `" + file + "` - **File does not exist**");
                            cooldown.complete(5);
                        }
                        case FAILURE -> {
                            hook.sendMessage("Failed to load the dictionary from this file. Does chosen language supports dictionary? (%s ms)".formatted(Utils.now() - time)).queue();
                            Logs.info(DiscordUtils.getAsTag(user) + " finished loading a new `" + type + "` dictionary from file `" + file + "` - **Failure**");
                            cooldown.complete(5);
                        }
                    }
                }
            }
        });
        return cooldown;
    }
}
