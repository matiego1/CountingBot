package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class PingCommand extends CommandHandler {
    public PingCommand(@NotNull Main instance) {
        this.instance = instance;
    }
    private final Main instance;

    @Override
    public @NotNull SlashCommandData getCommand() {
        return createSlashCommand(
                "ping",
                "Pokazuje obecny ping bota",
                false
        ).addOptions(EPHEMERAL_OPTION);
    }

    @Override
    public @NotNull CompletableFuture<Integer> onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        boolean ephemeral = event.getOption("ephemeral", "False", OptionMapping::getAsString).equals("True");
        if (instance.getStorage().getChannel(event.getChannel().getIdLong()) != null) ephemeral = true;

        long time = Utils.now();
        event.reply("Pong!")
                .setEphemeral(ephemeral)
                .flatMap(v -> event.getHook().editOriginalFormat("Pong: %d ms", Utils.now() - time))
                .queue();

        return CompletableFuture.completedFuture(3);
    }
}
