package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class AboutCommand extends CommandHandler {
    public AboutCommand(@NotNull Main instance) {
        this.instance = instance;
    }
    private final Main instance;
    public static final String DEFAULT_ABOUT_MESSAGE = """
                **__Counting bot__**
                Licz na różne sposoby i baw się dobrze!
                
                Opisy kanałów: [tutaj](<https://github.com/matiego1/CountingBot/blob/master/README.md>)
                
                **Autor:** `matiego`
                **Data kompilacji:** `<version>`
                """;

    @Override
    public @NotNull SlashCommandData getCommand() {
        return createSlashCommand(
                "about",
                "Wyświetla informację o tym bocie",
                false
        ).addOptions(EPHEMERAL_OPTION);
    }

    @Override
    public @NotNull CompletableFuture<Integer> onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        boolean ephemeral = event.getOption("ephemeral", "False", OptionMapping::getAsString).equals("True");

        if (instance.getStorage().getChannel(event.getChannel().getIdLong()) != null) ephemeral = true;

        String message = instance.getConfig().getString("about-message", "");
        if (message.isBlank()) message = DEFAULT_ABOUT_MESSAGE;

        message = message.replace("<version>", Utils.getVersion());

        event.reply(message)
                .setEphemeral(ephemeral)
                .queue();

        return CompletableFuture.completedFuture(5);
    }
}
