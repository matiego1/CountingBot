package me.matiego.counting.commands;

import me.matiego.counting.ChannelData;
import me.matiego.counting.Main;
import me.matiego.counting.Tasks;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.DiscordUtils;
import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class UnblockCommand extends CommandHandler {
    public UnblockCommand(@NotNull Main instance) {
        this.instance = instance;
    }
    private final Main instance;

    @Override
    public @NotNull CommandData getCommand() {
        return createSlashCommand(
                "unblock",
                "Unblocks counting channel(s)",
                true,
                Permission.MANAGE_CHANNEL
        ).addOptions(
                createOption(
                        "channel",
                        "Counting channel to unblock. Leave empty to unblock all channels.",
                        OptionType.CHANNEL,
                        false
                ).setChannelTypes(ChannelType.TEXT, ChannelType.GUILD_PUBLIC_THREAD),
                ADMIN_KEY_OPTION_NOT_REQUIRED
        );
    }

    @Override
    public @NotNull CompletableFuture<Integer> onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        event.deferReply(true).queue();
        InteractionHook hook = event.getHook();

        JDA jda = event.getJDA();
        User user = event.getUser();
        GuildChannel chn = event.getOption("channel", OptionMapping::getAsChannel);
        String adminKey = event.getOption("admin-key", OptionMapping::getAsString);
        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        CompletableFuture<Integer> cooldown = new CompletableFuture<>();
        Tasks.async(() -> {
            if (chn == null) {
                List<ChannelData> channels = instance.getStorage().getChannels();

                if (!Utils.checkAdminKey(adminKey, user)) {
                    channels = channels.stream()
                            .filter(data -> data.getGuildId() == guildId)
                            .toList();
                }

                int channelsSize = channels.size();
                channels.stream()
                        .map(channel -> channel.unblock(jda)
                                .thenApply(b -> b ? 1 : 0)
                        )
                        .reduce((a, b) -> a.thenCombine(b, Integer::sum))
                        .orElseGet(() -> CompletableFuture.completedFuture(0))
                        .thenAccept(result -> {
                            Logs.info(DiscordUtils.getAsTag(user) + " has unblocked " + result + " counting channel(s) out of " + channelsSize + ". (Guild ID: `" + guildId + "`)");
                            hook.sendMessage("Successfully unblocked %s counting channel(s) out of %s.".formatted(result, channelsSize)).queue();
                            cooldown.complete(10);
                        });
                return;
            }

            ChannelData data = instance.getStorage().getChannel(chn.getIdLong());
            if (data == null) {
                hook.sendMessage("This isn't the counting channel.").queue();
                cooldown.complete(5);
                return;
            }

            data.unblock(jda).thenAccept(result -> {
                if (result) {
                    Logs.info(DiscordUtils.getAsTag(user) + " has unblocked the counting channel of type: `" + data.getType() + "`. (ID: `" + data.getChannelId() + "`)");
                    hook.sendMessage("Successfully unblocked this counting channel.").queue();
                    cooldown.complete(5);
                } else {
                    hook.sendMessage("Failed to unblock this counting channel.").queue();
                    cooldown.complete(5);
                }
            });
        });
        return cooldown;
    }
}
