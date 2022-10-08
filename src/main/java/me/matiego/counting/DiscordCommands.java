package me.matiego.counting;

import me.matiego.counting.utils.Pair;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A Discord commands handler.
 */
public class DiscordCommands extends ListenerAdapter {
    public DiscordCommands(@NotNull Main plugin) {
        this.plugin = plugin;
    }

    private final Main plugin;

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        User user = event.getUser();
        if (event.getName().equals("ping")) {
            long time = System.currentTimeMillis();
            event.reply("Pong!").setEphemeral(event.getOption("ephemeral", false, OptionMapping::getAsBoolean)).flatMap(v -> event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time)).queue();
        } else if (event.getName().equals("about")) {
            event.reply(Translation.COMMANDS__ABOUT.getFormatted(plugin.getDescription().getVersion())).setEphemeral(event.getOption("ephemeral", false, OptionMapping::getAsBoolean)).queue();
        } else if (event.getName().equals("feedback")) {
            event.replyModal(Modal.create("feedback-modal", Translation.COMMANDS__FEEDBACK__TITLE.toString())
                    .addActionRows(
                            ActionRow.of(TextInput.create("subject", Translation.COMMANDS__FEEDBACK__SUBJECT.toString(), TextInputStyle.SHORT)
                                    .setRequiredRange(10, 100)
                                    .setPlaceholder(Translation.COMMANDS__FEEDBACK__SUBJECT_PLACEHOLDER.toString())
                                    .build()),
                            ActionRow.of(TextInput.create("description", Translation.COMMANDS__FEEDBACK__DESCRIPTION.toString(), TextInputStyle.PARAGRAPH)
                                    .setRequiredRange(30, 4000)
                                    .build())
                    )
                    .build()).queue();
        } else if (event.getName().equals("counting")) {
            event.deferReply(true).queue();
            InteractionHook hook = event.getHook();
            Utils.async(() -> {
                if (event.getChannel().getType() != ChannelType.TEXT) {
                    hook.sendMessage(Translation.GENERAL__UNSUPPORTED_CHANNEL_TYPE.toString()).queue();
                    return;
                }
                switch (Objects.requireNonNullElse(event.getSubcommandName(), "null")) {
                    case "add" ->
                            hook.sendMessage(Translation.COMMANDS__COUNTING__ADD.toString())
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
                                eb.setFooter(Utils.getName(user, event.getMember()) + "#" + user.getDiscriminator(), Utils.getAvatar(user, event.getMember()));
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
                        long guildId = -1;
                        try {
                            guildId = Objects.requireNonNull(event.getGuild()).getIdLong();
                        } catch (NullPointerException ignored) {}
                        for (Pair<Long, ChannelData> pair : plugin.getStorage().getChannels()) {
                            GuildChannel chn = jda.getGuildChannelById(pair.getFirst());
                            if (chn != null && chn.getGuild().getIdLong() == guildId) {
                                msg.append(chn.getAsMention()).append(": ").append(pair.getSecond().getType()).append("\n");
                            } else if (plugin.getConfig().getLong("main-guild-id") == guildId) {
                                msg.append(chn == null ? "`" + pair.getFirst() + "`" : chn.getAsMention()).append(": ").append(pair.getSecond().getType());
                                if (chn != null) {
                                    msg.append("; Guild: `[").append(chn.getGuild().getName()).append("]`");
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
        } else if (event.getName().equals("dictionary")) {
            event.deferReply(true).queue();
            InteractionHook hook = event.getHook();
            String typeString = event.getOption("language", "null", OptionMapping::getAsString).toUpperCase();
            Dictionary.Type type = Arrays.stream(Dictionary.Type.values())
                    .filter(value -> value.toString().equals(typeString))
                    .findFirst()
                    .orElse(null);
            if (type == null) {
                hook.sendMessage(Translation.GENERAL__UNKNOWN_LANGUAGE.toString()).queue();
                return;
            }
            Utils.async(() -> {
                switch (Objects.requireNonNullElse(event.getSubcommandName(), "null")) {
                    case "add" -> {
                        if (plugin.getDictionary().addWord(type, event.getOption("word", "null", OptionMapping::getAsString))) {
                            hook.sendMessage(Translation.COMMANDS__DICTIONARY__ADD__SUCCESS.toString()).queue();
                        } else {
                            hook.sendMessage(Translation.COMMANDS__DICTIONARY__ADD__FAILURE.toString()).queue();
                        }
                    }
                    case "remove" -> {
                        if (plugin.getDictionary().removeWord(type, event.getOption("word", "null", OptionMapping::getAsString))) {
                            hook.sendMessage(Translation.COMMANDS__DICTIONARY__REMOVE__SUCCESS.toString()).queue();
                        } else {
                            hook.sendMessage(Translation.COMMANDS__DICTIONARY__REMOVE__FAILURE.toString()).queue();
                        }
                    }
                    case "load" -> {
                        if (!event.getOption("admin-key", "", OptionMapping::getAsString).equalsIgnoreCase(plugin.getConfig().getString("admin-key"))) {
                            hook.sendMessage(Translation.COMMANDS__DICTIONARY__LOAD__INCORRECT_KEY.toString()).queue();
                            return;
                        }
                        hook.sendMessage(
                                switch (plugin.getDictionary().loadDictionaryFromFile(new File(plugin.getDataFolder() + File.separator + event.getOption("file", "null", OptionMapping::getAsString)), type)) {
                                    case SUCCESS -> Translation.COMMANDS__DICTIONARY__LOAD__SUCCESS.toString();
                                    case NO_CHANGES -> Translation.COMMANDS__DICTIONARY__LOAD__NO_CHANGES.toString();
                                    case FAILURE -> Translation.COMMANDS__DICTIONARY__LOAD__FAILURE.toString();
                                }).queue();
                    }
                }
            });
        }
    }

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {
        User user = event.getUser();
        if (event.getComponentId().equals("counting-type")) {
            event.deferReply(true).queue();
            Utils.async(() -> {
                ChannelData.Type type = Arrays.stream(ChannelData.Type.values())
                        .filter(value -> value.toString().equals(event.getValues().get(0)))
                        .findFirst()
                        .orElse(null);
                if (type == null) {
                    replySelectMenu(event, Translation.GENERAL__UNKNOWN_CHANNEL_TYPE.toString());
                    return;
                }

                MessageChannelUnion channelUnion = event.getChannel();
                if (channelUnion.getType() != ChannelType.TEXT) {
                    replySelectMenu(event, Translation.GENERAL__UNSUPPORTED_CHANNEL_TYPE.toString());
                    return;
                }
                TextChannel chn = channelUnion.asTextChannel();

                List<Webhook> webhooks = chn.retrieveWebhooks().complete();
                Webhook webhook = webhooks.isEmpty() ? chn.createWebhook("Counting bot").complete() : webhooks.get(0);

                String topic = Translation.GENERLA__CHANNEL_TOPIC.toString();
                if (!topic.isBlank()) chn.getManager().setTopic(topic).queue();

                switch (plugin.getStorage().addChannel(chn.getIdLong(), new ChannelData(type, webhook))) {
                    case SUCCESS -> {
                        replySelectMenu(event, Translation.COMMANDS__SELECT_MENU__SUCCESS.toString());
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle(Translation.GENERAL__OPEN_EMBED__TITLE.toString());
                        eb.setDescription(Translation.GENERAL__OPEN_EMBED__DESCRIPTION.getFormatted(type, type.getDescription()));
                        eb.setColor(Color.GREEN);
                        eb.setTimestamp(Instant.now());
                        eb.setFooter(Utils.getName(user, event.getMember()) + "#" + user.getDiscriminator(), Utils.getAvatar(user, event.getMember()));
                        chn.sendMessageEmbeds(eb.build()).queue();
                    }
                    case NO_CHANGES -> replySelectMenu(event, Translation.COMMANDS__SELECT_MENU__NO_CHANGES.toString());
                    case FAILURE -> replySelectMenu(event, Translation.COMMANDS__SELECT_MENU__FAILURE.toString());
                }
            });
        }
    }

    private void replySelectMenu(@NotNull SelectMenuInteractionEvent event, @NotNull String msg) {
        event.getHook().sendMessage(msg).queue();
        event.editSelectMenu(event.getSelectMenu().asDisabled()).queue();
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("dictionary") && event.getFocusedOption().getName().equals("language")) {
            event.replyChoices(Arrays.stream(Dictionary.Type.values())
                    .map(Enum::toString)
                    .filter(value -> value.startsWith(event.getFocusedOption().getValue().toUpperCase()))
                    .map(value -> new Command.Choice(value, value))
                    .toList()
            ).queue();
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (event.getModalId().equals("feedback-modal")) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Feedback - " + Objects.requireNonNull(event.getValue("subject")).getAsString());
            eb.setDescription(Objects.requireNonNull(event.getValue("description")).getAsString());
            eb.setTimestamp(Instant.now());
            eb.setColor(Color.BLUE);
            eb.setFooter(event.getUser().getAsTag());

            TextChannel chn = plugin.getJda().getTextChannelById(plugin.getConfig().getLong("logs-channel-id"));
            if (chn != null) {
                chn.sendMessageEmbeds(eb.build()).queue();
                event.reply(Translation.COMMANDS__FEEDBACK__SUCCESS.toString()).setEphemeral(true).queue();
            } else {
                event.reply(Translation.COMMANDS__FEEDBACK__FAILURE.toString()).setEphemeral(true).queue();
            }
        }
    }
}
