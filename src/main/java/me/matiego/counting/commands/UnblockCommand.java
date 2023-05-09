package me.matiego.counting.commands;

import me.matiego.counting.ChannelData;
import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
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
        return CommandHandler.createSlashCommand("unblock", true, Permission.MANAGE_CHANNEL)
                .addOptions(CommandHandler.createOption(
                        "channel",
                        OptionType.CHANNEL,
                        false,
                        Translation.COMMANDS__UNBLOCK__OPTION__NAME,
                        Translation.COMMANDS__UNBLOCK__OPTION__DESCRIPTION
                ));
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        event.deferReply(true).queue();
        InteractionHook hook = event.getHook();
        JDA jda = event.getJDA();
        GuildChannel chn = event.getOption("channel", OptionMapping::getAsChannel);

        Utils.async(() -> {
            if (chn == null) {
                List<ChannelData> channels = plugin.getStorage().getChannels();
                int success = 0;
                for (ChannelData channel : channels) {
                    if (channel.unblock(jda)) success++;
                }
                hook.sendMessage(Translation.COMMANDS__UNBLOCK__MESSAGE.getFormatted(success, channels.size())).queue();
                return;
            }
            ChannelData data = plugin.getStorage().getChannel(chn.getIdLong());
            if (data == null) {
                hook.sendMessage(Translation.COMMANDS__UNBLOCK__NOT_COUNTING_CHANNEL.toString()).queue();
                return;
            }
            if (data.unblock(jda)) {
                hook.sendMessage(Translation.COMMANDS__UNBLOCK__SUCCESS.toString()).queue();
            } else {
                hook.sendMessage(Translation.COMMANDS__UNBLOCK__FAILURE.toString()).queue();
            }
        });
    }
}
