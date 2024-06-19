package me.matiego.counting.commands;

import me.matiego.counting.ChannelData;
import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
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

public class UnblockCommand extends CommandHandler {
    public UnblockCommand(@NotNull Main plugin) {
        this.plugin = plugin;
    }
    private final Main plugin;
    
    /**
     * Returns the slash command.
     *
     * @return the slash command
     */
    @Override
    public @NotNull CommandData getCommand() {
        return createSlashCommand("unblock", true, Permission.MANAGE_CHANNEL)
                .addOptions(
                        createOption(
                                "channel",
                                OptionType.CHANNEL,
                                false,
                                Translation.COMMANDS__UNBLOCK__OPTION__NAME,
                                Translation.COMMANDS__UNBLOCK__OPTION__DESCRIPTION
                        )
                                .setChannelTypes(ChannelType.TEXT),
                        ADMIN_KEY_OPTION_NOT_REQUIRED
                );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        event.deferReply(true).queue();
        InteractionHook hook = event.getHook();
        JDA jda = event.getJDA();
        User user = event.getUser();
        Guild guild = event.getGuild();
        if (guild == null) return;
        long guildId = guild.getIdLong();
        GuildChannel chn = event.getOption("channel", OptionMapping::getAsChannel);
        String adminKey = event.getOption("admin-key", OptionMapping::getAsString);

        Utils.async(() -> {
            if (chn == null) {
                List<ChannelData> channels = plugin.getStorage().getChannels();

                if (adminKey == null || !Utils.checkAdminKey(adminKey, user)) {
                    channels = channels.stream()
                            .filter(data -> data.getGuildId() == guildId)
                            .toList();
                }

                int success = 0;
                for (ChannelData channel : channels) {
                    if (channel.unblock(jda)) success++;
                }

                Logs.info("User " + Utils.getAsTag(user) + " unblocked " + success + " counting channel(s) out of " + channels.size() + ".");

                reply(hook, user, event.getName(), 7 * Utils.SECOND, Translation.COMMANDS__UNBLOCK__MESSAGE.getFormatted(success, channels.size()));
                return;
            }
            ChannelData data = plugin.getStorage().getChannel(chn.getIdLong());
            if (data == null) {
                reply(hook, user, event.getName(), 3 * Utils.SECOND, Translation.COMMANDS__UNBLOCK__NOT_COUNTING_CHANNEL.toString());
                return;
            }
            if (data.unblock(jda)) {
                Logs.info("User " + Utils.getAsTag(user) + " unblocked counting channel. (ID: `" + data.getChannelId() + "`; Guild ID: `" + data.getGuildId() + "`; Channel type: `" + data.getType() + "`)");
                reply(hook, user, event.getName(), 5 * Utils.SECOND, Translation.COMMANDS__UNBLOCK__SUCCESS.toString());
            } else {
                reply(hook, user, event.getName(), 3 * Utils.SECOND, Translation.COMMANDS__UNBLOCK__FAILURE.toString());
            }
        });
    }

    private void reply(@NotNull InteractionHook hook, @NotNull User user, @NotNull String command, long time, @NotNull String message) {
        hook.sendMessage(message).queue();
        plugin.getCommandHandler().putSlowdown(user, command, time);
    }
}
