package me.matiego.counting.commands;

import me.matiego.counting.ChannelData;
import me.matiego.counting.Main;
import me.matiego.counting.Tasks;
import me.matiego.counting.minecraft.McException;
import me.matiego.counting.minecraft.McRewards;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.DiscordUtils;
import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.simpleyaml.configuration.file.FileConfiguration;

import java.time.Instant;
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
                        "info",
                        "Wyświetl swoje połączone konto Minecraft"
                )
        );
    }

    @Override
    public @NotNull CompletableFuture<Integer> onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        event.deferReply(true).queue();
        InteractionHook hook = event.getHook();

        Tasks.async(() -> {
            switch (String.valueOf(event.getSubcommandName())) {
                case "link" -> handleLinkSubcommand(event, hook);
                case "unlink" -> handleUnlinkSubcommand(event, hook);
                case "info" -> handleInfoSubcommand(event, hook);
            }
        });
        return CompletableFuture.completedFuture(10);
    }

    private void handleLinkSubcommand(@NotNull SlashCommandInteraction event, @NotNull InteractionHook hook) {
        User user = event.getUser();

        String code = event.getOption("code", OptionMapping::getAsString);
        if (code == null) return;

        if (instance.getMcAccounts().hasMinecraftAccount(user)) {
            hook.sendMessage("Już połączyłeś swojego konto.").queue();
            return;
        }

        UUID account;
        try {
            account = instance.getMcApiRequests().getUuidByCode(code);
        } catch (McException e) {
            hook.sendMessage(e.getMessage()).queue();
            return;
        }

        if (instance.getMcAccounts().setMinecraftAccount(user, account)) {
            Logs.info("User `" + DiscordUtils.getAsTag(user) + "` has linked account to `" + account + "`");
            hook.sendMessage("Pomyślnie połączono twoje konto!")
                    .setEmbeds(getEmbed(account))
                    .queue();
        } else {
            hook.sendMessage("Napotkano niespodziewany błąd. Spróbuj ponownie później.").queue();
        }
    }

    private void handleUnlinkSubcommand(@NotNull SlashCommandInteraction event, @NotNull InteractionHook hook) {
        User user = event.getUser();
        if (!instance.getMcAccounts().hasMinecraftAccount(user)) {
            hook.sendMessage("Nie połączyłeś jeszcze swojego konta albo napotkano niespodziewany błąd. Aby połączyć swoje konto, wejdź na serwer Minecraft i użyj komendy `/linkdiscord`.").queue();
            return;
        }

        if (instance.getMcAccounts().removeMinecraftAccount(user)) {
            Logs.info("User " + DiscordUtils.getAsTag(user) + " has unlinked account.");
            hook.sendMessage("Pomyślnie rozłączono twoje konto Minecraft.").queue();
        } else {
            hook.sendMessage("Napotkano niespodziewany błąd. Spróbuj ponownie później.").queue();
        }
    }

    private void handleInfoSubcommand(@NotNull SlashCommandInteraction event, @NotNull InteractionHook hook) {
        UUID account = instance.getMcAccounts().getMinecraftAccount(event.getUser());

        if (account == null) {
            hook.sendMessage("Nie połączyłeś jeszcze swojego konta albo napotkano niespodziewany błąd. Aby połączyć swoje konto, wejdź na serwer Minecraft i użyj komendy `/linkdiscord`.").queue();
            return;
        }

        FileConfiguration config = instance.getConfig();
        McRewards rewards = instance.getMcRewards();

        String interval = Utils.parseMillisToString(rewards.getInterval(config), false)
                .replace("m", " minut");
        StringBuilder message = new StringBuilder("**Nagrody za liczenie:** `(wiadomość / kanał / %s)`\n".formatted(interval));

        for (ChannelData.Type type : ChannelData.Type.values()) {
            message.append("- %s: `%s$`\n".formatted(type.toString(), Utils.doubleToString(rewards.getChannelReward(config, type.name()))));
        }
        message.append("\nJeśli ostatnia wiadomość na kanale została wysłana >%s temu, to nagroda jest zwiększana %s krotnie.".formatted(Utils.parseMillisToString(rewards.getOldMessage(config), false, false), Utils.doubleToString(rewards.getOldMessageMultiplier(config))));

        hook.sendMessage(message.toString())
                .setEmbeds(getEmbed(account)).queue();
    }

    private @NotNull MessageEmbed getEmbed(@NotNull UUID uuid) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Połączone konto Minecraft:");
        eb.setDescription("[`" + uuid + "`](<https://pl.namemc.com/search?q=" + uuid + ">)");
        eb.setThumbnail("https://mc-heads.net/avatar/" + uuid + ".png");
        eb.setColor(Utils.YELLOW);
        eb.setTimestamp(Instant.now());
        return eb.build();
    }
}
