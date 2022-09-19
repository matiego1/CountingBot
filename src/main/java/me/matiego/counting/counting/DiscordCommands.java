package me.matiego.counting.counting;

import me.matiego.counting.counting.utils.Pair;
import me.matiego.counting.counting.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DiscordCommands extends ListenerAdapter {
    //TODO: permissions
    public DiscordCommands(@NotNull Main plugin) {
        this.plugin = plugin;
    }

    private final Main plugin;

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("ping")) {
            long time = System.currentTimeMillis();
            event.reply("Pong!").setEphemeral(event.getOption("ephemeral", true, OptionMapping::getAsBoolean)).flatMap(v -> event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time)).queue();
        } else if (event.getName().equals("counting")) {
            event.deferReply(true).queue();
            InteractionHook hook = event.getHook();
            Utils.async(() -> {
                if (event.getChannel().getType() != net.dv8tion.jda.api.entities.ChannelType.TEXT) {
                    hook.sendMessage("This channel's type is not supported.").queue();
                    return;
                }
                switch (Objects.requireNonNullElse(event.getSubcommandName(), "null")) {
                    case "add" ->
                            hook.sendMessage("**__Select the type of the new counting channel:__**")
                                    .addActionRow(
                                            SelectMenu.create("counting-type")
                                                    .addOptions(ChannelType.getSelectMenuOptions())
                                                    .setRequiredRange(1, 1)
                                                    .build())
                                    .queue();
                    case "remove" -> {
                        switch (plugin.getStorage().removeChannel(event.getChannel().getIdLong())) {
                            case SUCCESS -> {
                                hook.sendMessage("The channel has been successfully closed!").queue();
                                EmbedBuilder eb = new EmbedBuilder();
                                eb.setTitle("Counting channel closed!");
                                eb.setColor(Color.RED);
                                eb.setTimestamp(Instant.now());
                                eb.setFooter(Utils.getName(event.getUser(), event.getMember()) + "#" + event.getUser().getDiscriminator(), Utils.getAvatar(event.getUser(), event.getMember()));
                                event.getChannel().sendMessageEmbeds(eb.build()).queue();
                            }
                            case NO_CHANGES -> hook.sendMessage("This channel has been already closed.").queue();
                            case FAILURE -> hook.sendMessage("An error occurred. Try again.").queue();
                        }
                    }
                    case "list" -> {
                        StringBuilder msg = new StringBuilder("**__Open Counting Channels:__**\n");
                        int emptyMsgLength = msg.length();
                        JDA jda = event.getJDA();
                        for (Pair<Long, ChannelType> pair : plugin.getStorage().getChannels()) {
                            GuildChannel chn = jda.getGuildChannelById(pair.getFirst());
                            msg.append(chn == null ? "`" + pair.getFirst() + "`" : chn.getAsMention()).append(": ").append(pair.getSecond()).append("\n");
                        }
                        if (emptyMsgLength == msg.length()) {
                            hook.sendMessage("No counting channel has been opened yet. Open a new one with `/counting add`").queue();
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
                hook.sendMessage("Unknown dictionary type. Try again.").queue();
                return;
            }
            Utils.async(() -> {
                switch (Objects.requireNonNullElse(event.getSubcommandName(), "null")) {
                    case "add" -> {
                        if (plugin.getDictionary().addWord(type, event.getOption("word", "null", OptionMapping::getAsString))) {
                            hook.sendMessage("This word has been successfully added to the dictionary!").queue();
                        } else {
                            hook.sendMessage("An error occurred. Try again.").queue();
                        }
                    }
                    case "remove" -> {
                        if (plugin.getDictionary().removeWord(type, event.getOption("word", "null", OptionMapping::getAsString))) {
                            hook.sendMessage("This word has been successfully removed from the dictionary!").queue();
                        } else {
                            hook.sendMessage("An error occurred. Try again.").queue();
                        }
                    }
                    case "load" -> {
                        if (!event.getOption("admin-key", "null", OptionMapping::getAsString).equalsIgnoreCase(plugin.getConfig().getString("admin-key"))) {
                            hook.sendMessage("Incorrect administrator key!").queue();
                            return;
                        }
                        hook.sendMessage(
                                switch (plugin.getDictionary().loadDictionaryFromFile(new File(plugin.getDataFolder() + File.separator + event.getOption("file", "null", OptionMapping::getAsString)), type)) {
                                    case SUCCESS -> "Success!";
                                    case NO_CHANGES -> "This file does not exist.";
                                    case FAILURE -> "An error occurred.";
                                }).queue();
                    }
                }
            });
        }
    }

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {
        if (event.getComponentId().equals("counting-type")) {
            event.deferReply(true).queue();
            Utils.async(() -> {
                ChannelType type = Arrays.stream(ChannelType.values())
                        .filter(value -> value.toString().equals(event.getValues().get(0)))
                        .findFirst()
                        .orElse(null);
                if (type == null) {
                    replySelectMenu(event, "Unknown channel type. Try again.");
                    return;
                }

                MessageChannelUnion channelUnion = event.getChannel();
                if (channelUnion.getType() != net.dv8tion.jda.api.entities.ChannelType.TEXT) {
                    replySelectMenu(event, "An error occurred.");
                    return;
                }
                TextChannel chn = channelUnion.asTextChannel();

                List<Webhook> webhooks = chn.retrieveWebhooks().complete();
                String url = webhooks.isEmpty() ? chn.createWebhook("Counting bot").complete().getUrl() : webhooks.get(0).getUrl();

                switch (plugin.getStorage().addChannel(chn.getIdLong(), type, url)) {
                    case SUCCESS -> {
                        replySelectMenu(event, "The channel has been successfully opened!");
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle("**This counting channel is now open!**");
                        eb.setDescription("**Feel free to play with us!**\n\nChannel type: `" + type + "`\nDescription: `" + type.getDescription() + "`");
                        eb.setColor(Color.GREEN);
                        eb.setTimestamp(Instant.now());
                        eb.setFooter(Utils.getName(event.getUser(), event.getMember()) + "#" + event.getUser().getDiscriminator(), Utils.getAvatar(event.getUser(), event.getMember()));
                        chn.sendMessageEmbeds(eb.build()).queue();
                    }
                    case NO_CHANGES -> replySelectMenu(event, "This channel is already opened!");
                    case FAILURE -> replySelectMenu(event, "An error occurred. Try again.");
                }
            });
        }
    }

    private void replySelectMenu(@NotNull SelectMenuInteractionEvent event, @NotNull String msg) {
        event.getHook().sendMessage(msg).queue();
        event.editSelectMenu(event.getSelectMenu().asDisabled()).queue();
    }

    //TODO: doesn't work
    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        System.out.println("1" + event.getName() + event.getFocusedOption().getName());
        if (event.getName().equals("dictionary") && event.getFocusedOption().getName().equals("language")) {
            System.out.println("2");
            event.replyChoices(Arrays.stream(Dictionary.Type.values())
                    .map(Enum::toString)
                    .map(value -> new Command.Choice(value, value))
                    .toList()
            ).queue();
        }
    }
}
