package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.Tasks;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.Pair;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MinecraftCommand extends CommandHandler {
    public MinecraftCommand(@NotNull Main instance) {
        this.instance = instance;
    }

    private final Main instance;

    @Override
    public @NotNull SlashCommandData getCommand() {
        return createSlashCommand(
                "minecraft",
                "Zarządza połączonym kontem Minecraft",
                true
        ).addSubcommands(
                createSubcommand(
                        "link",
                        "Połącz nowe konto Minecraft",
                        createOption(
                                "code",
                                "Kod weryfikacji",
                                OptionType.STRING,
                                true
                        ).setMaxLength(30)
                ),
                createSubcommand(
                        "unlink",
                        "Odłącz swoje konto Minecraft"
                ),
                createSubcommand(
                        "list",
                        "Wyświetl swoje połączone konto Minecraft"
                )
        );
    }

    @Override
    public @NotNull CompletableFuture<Integer> onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        event.deferReply(true).queue();
        InteractionHook hook = event.getHook();

        CompletableFuture<Integer> cooldown = new CompletableFuture<>();
        Tasks.async(() -> cooldown.complete(switch (String.valueOf(event.getSubcommandName())) {
            case "link" -> handleLinkSubcommand(event, hook);
            case "unlink" -> handleUnlinkSubcommand(event, hook);
            case "list" -> handleListSubcommand(event, hook);
            default -> 3;
        }));
        return cooldown;
    }

    private int handleLinkSubcommand(@NotNull SlashCommandInteraction event, @NotNull InteractionHook hook) {
        // TODO
        hook.sendMessage("Już wkrótce!").queue();
        return 3;
    }

    private int handleUnlinkSubcommand(@NotNull SlashCommandInteraction event, @NotNull InteractionHook hook) {
        // TODO
        hook.sendMessage("Już wkrótce!").queue();
        return 3;
    }

    private int handleListSubcommand(@NotNull SlashCommandInteraction event, @NotNull InteractionHook hook) {
        User user = event.getUser();
        Pair<UUID, Long> account = instance.getMinecraftAccounts().getMinecraftAccount(user);

        if (account == null) {
            hook.sendMessage("Nie połączyłeś jeszcze swojego konta albo napotkano niespodziewany błąd. Aby połączyć swoje konto, wejdź na serwer Minecraft i użyj komendy `/linkdiscord`.").queue();
            return 5;
        }

        // TODO
        hook.sendMessage("Już wkrótce").queue();
        return 3;
    }
}
