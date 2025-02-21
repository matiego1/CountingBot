package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ReloadCommand extends CommandHandler {
    public ReloadCommand(@NotNull Main instance) {
        this.instance = instance;
    }
    private final Main instance;

    @Override
    public @NotNull CommandData getCommand() {
        return createSlashCommand(
                "reload",
                "Reload bot's configuration",
                true,
                Permission.ADMINISTRATOR
        ).addOptions(ADMIN_KEY_OPTION);
    }

    @Override
    public @NotNull CompletableFuture<Integer> onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        event.deferReply(true).queue();
        InteractionHook hook = event.getHook();

        String adminKey = event.getOption("admin-key", OptionMapping::getAsString);
        if (!Utils.checkAdminKey(adminKey, event.getUser())) {
            hook.sendMessage("Incorrect administrator key!").queue();
            return CompletableFuture.completedFuture(5);
        }

        instance.reload(event.getJDA())
                .thenRun(() -> hook.sendMessage("Reloaded!").queue());
        return CompletableFuture.completedFuture(5);
    }
}
