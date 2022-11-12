package me.matiego.counting.commands;

import me.matiego.counting.ChannelData;
import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.ICommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenuInteraction;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CountingCommand implements ICommandHandler {
    private final Main plugin;

    public CountingCommand(@NotNull Main plugin) {
        this.plugin = plugin;
    }


    /**
     * Returns the slash command.
     *
     * @return the slash command
     */
    @Override
    public @NotNull SlashCommandData getCommand() {
        return Commands.slash("counting", Translation.COMMANDS__COUNTING__DESCRIPTION.getDefault())
                .setNameLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__COUNTING__NAME.toString()))
                .setDescriptionLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__COUNTING__DESCRIPTION.toString()))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
                .setGuildOnly(true)
                .addSubcommands(
                        getSubcommand(
                                "add",
                                Translation.COMMANDS__COUNTING__OPTIONS__ADD__NAME,
                                Translation.COMMANDS__COUNTING__OPTIONS__ADD__DESCRIPTION
                        ),
                        getSubcommand(
                                "remove",
                                Translation.COMMANDS__COUNTING__OPTIONS__REMOVE__NAME,
                                Translation.COMMANDS__COUNTING__OPTIONS__REMOVE__DESCRIPTION
                        ),
                        getSubcommand(
                                "list",
                                Translation.COMMANDS__COUNTING__OPTIONS__LIST__NAME,
                                Translation.COMMANDS__COUNTING__OPTIONS__LIST__DESCRIPTION
                        )
                );
    }

    private @NotNull SubcommandData getSubcommand(@NotNull String name, @NotNull Translation nameLoc, @NotNull Translation descriptionLoc, @NotNull OptionData... options) {
        return new SubcommandData(name, descriptionLoc.getDefault())
                .setNameLocalizations(Utils.getAllLocalizations(nameLoc.toString()))
                .setDescriptionLocalizations(Utils.getAllLocalizations(descriptionLoc.toString()))
                .addOptions(options);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        event.deferReply(true).queue();
        User user = event.getUser();
        InteractionHook hook = event.getHook();

        Utils.async(() -> {
            if (event.getChannel().getType() != ChannelType.TEXT) {
                hook.sendMessage(Translation.GENERAL__UNSUPPORTED_CHANNEL_TYPE.toString()).queue();
                return;
            }

            plugin.getCommandHandler().putSlowdown(user, event.getName(), 5 * Utils.SECOND);

            switch (Objects.requireNonNullElse(event.getSubcommandName(), "null")) {
                case "add" -> hook.sendMessage(Translation.COMMANDS__COUNTING__ADD.toString())
                        .addActionRow(
                                SelectMenu.create("counting-type")
                                        .addOptions(ChannelData.getSelectMenuOptions())
                                        .setRequiredRange(1, 1)
                                        .build())
                        .queue();
                case "remove" -> {
                    switch (plugin.getStorage().removeChannel(event.getChannel().getIdLong())) {
                        case SUCCESS -> {
                            hook.sendMessage(Translation.COMMANDS__COUNTING__REMOVE__SUCCESS.toString()).queue();
                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setTitle(Translation.GENERAL__CLOSE_EMBED.toString());
                            eb.setColor(Color.RED);
                            eb.setTimestamp(Instant.now());
                            eb.setFooter(Utils.getMemberAsTag(user, event.getMember()), Utils.getAvatar(user, event.getMember()));
                            event.getChannel().sendMessageEmbeds(eb.build()).queue();
                        }
                        case NO_CHANGES -> hook.sendMessage(Translation.COMMANDS__COUNTING__REMOVE__NO_CHANGES.toString()).queue();
                        case FAILURE -> hook.sendMessage(Translation.COMMANDS__COUNTING__REMOVE__FAILURE.toString()).queue();
                    }
                }
                case "list" -> {
                    StringBuilder msg = new StringBuilder(Translation.COMMANDS__COUNTING__LIST__LIST + "\n");
                    int emptyMsgLength = msg.length();
                    JDA jda = event.getJDA();
                    long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

                    for (ChannelData data : plugin.getStorage().getChannels()) {
                        GuildChannel chn = jda.getGuildChannelById(data.getChannelId());
                        if (chn != null && chn.getGuild().getIdLong() == guildId) {
                            msg.append(chn.getAsMention()).append(": ").append(data.getType()).append("\n");
                        } else if (plugin.getConfig().getLong("main-guild-id") == guildId) {
                            msg.append(chn == null ? "`" + data.getChannelId() + "`" : chn.getAsMention()).append(": ").append(data.getType());
                            if (chn != null) {
                                msg.append(" `[").append(chn.getGuild().getName()).append("]`");
                            }
                            msg.append("\n");
                        }
                    }

                    if (emptyMsgLength == msg.length()) {
                        hook.sendMessage(Translation.COMMANDS__COUNTING__LIST__EMPTY_LIST.toString()).queue();
                    } else {
                        hook.sendMessage(msg.toString()).queue();
                    }
                }
            }
        });
    }

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteraction event) {
        User user = event.getUser();
        if (event.getComponentId().equals("counting-type")) {
            event.deferReply(true).queue();
            Utils.async(() -> {
                ChannelData.Type type = Arrays.stream(ChannelData.Type.values())
                        .filter(value -> value.toString().equals(event.getValues().get(0)))
                        .findFirst()
                        .orElse(null);
                if (type == null) {
                    reply(event, Translation.GENERAL__UNKNOWN_CHANNEL_TYPE.toString());
                    return;
                }

                MessageChannelUnion channelUnion = event.getChannel();
                if (channelUnion.getType() != ChannelType.TEXT) {
                    reply(event, Translation.GENERAL__UNSUPPORTED_CHANNEL_TYPE.toString());
                    return;
                }
                TextChannel chn = channelUnion.asTextChannel();

                plugin.getCommandHandler().putSlowdown(user, "counting", 5 * Utils.SECOND);

                List<Webhook> webhooks = chn.retrieveWebhooks().complete();
                Webhook webhook = webhooks.isEmpty() ? chn.createWebhook("Counting bot").complete() : webhooks.get(0);

                switch (plugin.getStorage().addChannel(new ChannelData(chn.getIdLong(), chn.getGuild().getIdLong(), type, webhook))) {
                    case SUCCESS -> {
                        reply(event, Translation.COMMANDS__SELECT_MENU__SUCCESS.toString());
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle(Translation.GENERAL__OPEN_EMBED__TITLE.toString());
                        eb.setDescription(Translation.GENERAL__OPEN_EMBED__DESCRIPTION.getFormatted(type, type.getDescription()));
                        eb.setColor(Color.GREEN);
                        eb.setTimestamp(Instant.now());
                        eb.setFooter(Utils.getMemberAsTag(user, event.getMember()), Utils.getAvatar(user, event.getMember()));
                        chn.sendMessageEmbeds(eb.build()).queue(message -> message.pin().queue());
                    }
                    case NO_CHANGES -> reply(event, Translation.COMMANDS__SELECT_MENU__NO_CHANGES.toString());
                    case FAILURE -> reply(event, Translation.COMMANDS__SELECT_MENU__FAILURE.toString());
                }
            });
        }
    }

    private void reply(@NotNull SelectMenuInteraction event, @NotNull String msg) {
        event.getHook().sendMessage(msg).queue();
        event.editSelectMenu(event.getSelectMenu().asDisabled()).queue();
    }
}
