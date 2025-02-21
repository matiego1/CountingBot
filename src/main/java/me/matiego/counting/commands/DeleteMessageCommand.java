package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.DiscordUtils;
import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class DeleteMessageCommand extends CommandHandler {
    public DeleteMessageCommand(@NotNull Main instance) {
        this.instance = instance;
    }
    private final Main instance;

    @Override
    public @NotNull CommandData getCommand() {
        return createSlashCommand(
                "delete-message",
                "Delete message",
                true,
                Permission.MESSAGE_MANAGE
        ).addOptions(
                ADMIN_KEY_OPTION,
                createOption(
                        "channel",
                        "Channel ID",
                        OptionType.STRING,
                        true
                ),
                createOption(
                        "message",
                        "Message ID",
                        OptionType.STRING,
                        true
                )
        );
    }

    @Override
    public @NotNull CompletableFuture<Integer> onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        InteractionHook hook = event.getHook();
        event.deferReply(true).queue();

        String adminKey = event.getOption("admin-key", OptionMapping::getAsString);
        if (!Utils.checkAdminKey(adminKey, event.getUser())) {
            hook.sendMessage("Incorrect administrator key.").queue();
            return CompletableFuture.completedFuture(3);
        }

        long channelId;
        long messageId;
        try {
            channelId = event.getOption("channel", 0L, OptionMapping::getAsLong);
            messageId = event.getOption("message", 0L, OptionMapping::getAsLong);
        } catch (Exception e) {
            hook.sendMessage("Incorrect IDs.").queue();
            return CompletableFuture.completedFuture(3);
        }

        MessageChannel chn = event.getJDA().getChannelById(MessageChannel.class, channelId);
        if (chn == null) {
            hook.sendMessage("Channel with provided ID either doesn't exist or isn't the message channel.").queue();
            return CompletableFuture.completedFuture(3);
        }

        try {
            chn.retrieveMessageById(messageId).queue(
                    message -> {
                        if (message.getAuthor().getIdLong() != event.getJDA().getSelfUser().getIdLong() && instance.getStorage().getChannel(chn.getIdLong()) == null) {
                            hook.sendMessage("You cannot delete this message. It isn't bot's message and it isn't in the counting channel.").queue();
                            return;
                        }

                        message.delete().queue();
                        event.getHook().sendMessage("Successfully deleted this message.").queue();

                        Logs.info(DiscordUtils.checkLength(DiscordUtils.getAsTag(event.getUser()) + " has deleted `" + DiscordUtils.getAsTag(message.getAuthor()) + "`'s message in channel " + message.getChannel().getAsMention() + " (ID: `" + message.getChannelId() + "`). Message's content: ```\n" + message.getContentDisplay().replace("```", "\\`\\`\\`"), Message.MAX_CONTENT_LENGTH - 5) + "\n```");
                    },
                    failure -> event.getHook().sendMessage("Failed to retrieve the message.").queue()
            );
        } catch (Exception e) {
            hook.sendMessage("Failed to retrieve the message.").queue();
        }
        return CompletableFuture.completedFuture(3);
    }
}
