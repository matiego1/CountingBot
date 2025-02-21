package me.matiego.counting.commands;

import me.matiego.counting.ChannelData;
import me.matiego.counting.Main;
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

public class BlockCommand extends CommandHandler {
    public BlockCommand(@NotNull Main instance) {
        this.instance = instance;
    }
    private final Main instance;

    @Override
    public @NotNull CommandData getCommand() {
        return createSlashCommand(
                "block",
                "Blocks counting channel(s)",
                true,
                Permission.MANAGE_CHANNEL
        ).addOptions(
                createOption(
                        "channel",
                        "Counting channel to block. Leave empty to block all channels.",
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
        GuildChannel channel = event.getOption("channel", OptionMapping::getAsChannel);
        String adminKey = event.getOption("admin-key", OptionMapping::getAsString);
        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        CompletableFuture<Integer> cooldown = new CompletableFuture<>();
        Utils.async(() -> {
            if (channel == null) {
                List<ChannelData> channels = instance.getStorage().getChannels();

                if (!DiscordUtils.checkAdminKey(adminKey, user)) {
                    channels = channels.stream()
                            .filter(data -> data.getGuildId() == guildId)
                            .toList();
                }

                int channelsSize = channels.size();
                channels.stream()
                        .map(chn -> chn.block(jda)
                                .thenApply(b -> b ? 1 : 0)
                        )
                        .reduce((a, b) -> a.thenCombine(b, Integer::sum))
                        .orElseGet(() -> CompletableFuture.completedFuture(0))
                        .thenAccept(result -> {
                            Logs.info(DiscordUtils.getAsTag(user) + " has blocked " + result + " counting channel(s) out of " + channelsSize + ". (Guild ID: `" + guildId + "`)");
                            hook.sendMessage("Successfully blocked %s counting channel(s) out of %s.".formatted(result, channelsSize)).queue();
                            cooldown.complete(10);
                        });
                return;
            }

            ChannelData data = instance.getStorage().getChannel(channel.getIdLong());
            if (data == null) {
                hook.sendMessage("This isn't the counting channel.").queue();
                cooldown.complete(5);
                return;
            }

            data.block(jda).thenAccept(result -> {
                if (result) {
                    Logs.info(DiscordUtils.getAsTag(user) + " has blocked the counting channel of type: `" + data.getType() + "`. (ID: `" + data.getChannelId() + "`)");
                    hook.sendMessage("Successfully blocked this counting channel.").queue();
                    cooldown.complete(5);
                } else {
                    hook.sendMessage("Failed to block this counting channel.").queue();
                    cooldown.complete(5);
                }
            });
        });
        return cooldown;
    }
}
