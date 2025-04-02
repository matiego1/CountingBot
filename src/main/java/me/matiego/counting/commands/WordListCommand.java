package me.matiego.counting.commands;

import me.matiego.counting.Dictionary;
import me.matiego.counting.Main;
import me.matiego.counting.Tasks;
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

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class WordListCommand extends CommandHandler {
    public WordListCommand(@NotNull Main instance) {
        this.instance = instance;
    }
    private final Main instance;

    @Override
    public @NotNull SlashCommandData getCommand() {
        OptionData languageOption = createOption(
                "language",
                "The word list type",
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
                "The word to mark/unmark as used",
                OptionType.STRING,
                true
        );

        return createSlashCommand(
                "word-list",
                "Manages used word list in this guild",
                true,
                Permission.MANAGE_CHANNEL
        ).addSubcommands(
                createSubcommand(
                        "add",
                        "Marks the word as used in this guild",
                        languageOption,
                        wordOption
                ),
                createSubcommand(
                        "remove",
                        "Unmarks the word as used in this guild",
                        languageOption,
                        wordOption
                )
        );
    }

    @Override
    public @NotNull CompletableFuture<Integer> onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        long time = Utils.now();

        event.deferReply(true).queue();
        InteractionHook hook = event.getHook();

        User user = event.getUser();
        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        Dictionary.Type type = Dictionary.Type.getByString(event.getOption("language", "null", OptionMapping::getAsString).toUpperCase());
        if (type == null) {
            hook.sendMessage("Unknown dictionary type.").queue();
            return CompletableFuture.completedFuture(3);
        }

        String word = event.getOption("word", "null", OptionMapping::getAsString);

        Tasks.async(() -> {
            switch (Objects.requireNonNullElse(event.getSubcommandName(), "null")) {
                case "add" -> addWord(type, guildId, word, hook, user, time);
                case "remove" -> removeWord(type, guildId, word, hook, user, time);
            }
        });
        return CompletableFuture.completedFuture(5);
    }

    private void addWord(@NotNull Dictionary.Type type, long guildId, @NotNull String word, @NotNull InteractionHook hook, @NotNull User user, long time) {
        Dictionary dictionary = instance.getDictionary();
        if (type.isDictionarySupported() && !dictionary.isWordInDictionary(type, word)) {
            hook.sendMessage("This word doesn't exist in the dictionary.").queue();
            return;
        }
        switch (dictionary.markWordAsUsed(type, guildId, word)) {
            case SUCCESS -> {
                Logs.info(DiscordUtils.getAsTag(user) + " has added the word `" + word + "` to the `" + type + "` list of used words. (Guild ID: `" + guildId + "`)");
                hook.sendMessage("Successfully marked this word as used in this guild. (%s ms)".formatted(Utils.now() - time)).queue();
            }
            case NO_CHANGES -> hook.sendMessage("This word has already been marked as used in this guild. (%s ms)".formatted(Utils.now() - time)).queue();
            case FAILURE -> hook.sendMessage("Failed to mark this word as used in this guild. (%s ms)".formatted(Utils.now() - time)).queue();
        }
    }

    private void removeWord(@NotNull Dictionary.Type type, long guildId, @NotNull String word, @NotNull InteractionHook hook, @NotNull User user, long time) {
        if (instance.getDictionary().unmarkWordAsUsed(type, guildId, word)) {
            Logs.info(DiscordUtils.getAsTag(user) + " has removed the word `" + word + "` from the `" + type + "` list of used words. (Guild ID: `" + guildId + "`)");
            hook.sendMessage("Successfully unmarked this word as used in this guild. (%s ms)".formatted(Utils.now() - time)).queue();
        } else {
            hook.sendMessage("Failed to unmark this word as used in this guild. (%s ms)".formatted(Utils.now() - time)).queue();
        }
    }
}
