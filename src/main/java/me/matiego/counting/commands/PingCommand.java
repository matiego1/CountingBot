package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

public class PingCommand extends CommandHandler {
    /**
     * Returns the slash command.
     *
     * @return the slash command
     */
    @Override
    public @NotNull SlashCommandData getCommand() {
        return CommandHandler.createSlashCommand("ping", false)
                .addOptions(CommandHandler.EPHEMERAL_OPTION);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        boolean ephemeral = event.getOption("ephemeral", "False", OptionMapping::getAsString).equals("True");
        if (Main.getInstance().getStorage().getChannel(event.getChannel().getIdLong()) != null) ephemeral = true;

        long time = Utils.now();
        event.reply("Pong!")
                .setEphemeral(ephemeral)
                .flatMap(v -> event.getHook().editOriginalFormat("Pong: %d ms", Utils.now() - time))
                .queue();
    }
}
