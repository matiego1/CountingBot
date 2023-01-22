package me.matiego.counting.commands;

import me.matiego.counting.ChannelData;
import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.ICommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BlockCommand implements ICommandHandler {
    public BlockCommand(@NotNull Main plugin) {
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
        return Commands.slash("block", Translation.COMMANDS__BLOCK__DESCRIPTION.getDefault())
                .setNameLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__BLOCK__NAME.toString()))
                .setDescriptionLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__BLOCK__DESCRIPTION.toString()))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
                .setGuildOnly(true)
                .addOptions(
                        new OptionData(OptionType.CHANNEL, "channel", Translation.COMMANDS__BLOCK__OPTION__DESCRIPTION.getDefault(), false)
                                .setNameLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__BLOCK__OPTION__NAME.toString()))
                                .setDescriptionLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__BLOCK__OPTION__DESCRIPTION.toString()))
                                .setChannelTypes(ChannelType.TEXT)
                );
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
                    if (channel.block(jda)) success++;
                }
                hook.sendMessage(Translation.COMMANDS__BLOCK__MESSAGE.getFormatted(success, channels.size())).queue();
                return;
            }
            ChannelData data = plugin.getStorage().getChannel(event.getChannel().getIdLong());
            if (data == null) {
                hook.sendMessage(Translation.COMMANDS__BLOCK__NOT_COUNTING_CHANNEL.toString()).queue();
                return;
            }
            if (data.block(jda)) {
                hook.sendMessage(Translation.COMMANDS__BLOCK__FAILURE.toString()).queue();
            } else {
                hook.sendMessage(Translation.COMMANDS__BLOCK__SUCCESS.toString()).queue();
            }
        });
    }
}
