package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.DiscordUtils;
import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.context.MessageContextInteraction;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class DeleteMessageContextCommand extends CommandHandler {
    public DeleteMessageContextCommand(@NotNull Main instance) {
        this.instance = instance;
    }
    private final Main instance;

    private final HashMap<Long, Long> messages = Utils.createLimitedSizeMap(1000);

    @Override
    public @NotNull CommandData getCommand() {
        return Commands.message("delete this message")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
                .setGuildOnly(true);
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteraction event) {
        if (!event.getName().equals("delete this message")) return;

        MessageChannelUnion union = event.getChannel();
        if (union == null) {
            event.reply("Unknown channel.").setEphemeral(true).queue();
            return;
        }

        messages.put(event.getUser().getIdLong(), event.getTarget().getIdLong());
        event.replyModal(
                Modal.create("delete-msg-modal", "Confirm your permissions!")
                        .addComponents(
                                ActionRow.of(TextInput.create("admin-key", "Enter an administrator key:", TextInputStyle.SHORT)
                                        .setRequired(true)
                                        .build())
                        )
                        .build()
        ).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteraction event) {
        if (!event.getModalId().equals("delete-msg-modal")) return;

        event.deferReply(true).queue();
        InteractionHook hook = event.getHook();

        User user = event.getUser();

        ModalMapping mapping = event.getValue("admin-key");
        if (mapping == null) return;
        String key = mapping.getAsString();
        if (!DiscordUtils.checkAdminKey(key, user)) {
            hook.sendMessage("Incorrect administrator key.").queue();
            return;
        }

        Long messageId = messages.remove(user.getIdLong());
        if (messageId == null) {
            hook.sendMessage("Failed to retrieve the message.").queue();
            return;
        }

        try {
            event.getChannel().retrieveMessageById(messageId).queue(
                    message -> {
                        if (message.getAuthor().getIdLong() != event.getJDA().getSelfUser().getIdLong() && instance.getStorage().getChannel(event.getChannel().getIdLong()) == null) {
                            hook.sendMessage("You cannot delete this message. It isn't bot's message and it isn't in the counting channel.").queue();
                            return;
                        }

                        message.delete().queue();
                        event.getHook().sendMessage("Successfully deleted this message.").queue();

                        Logs.info(DiscordUtils.checkLength(DiscordUtils.getAsTag(user) + " has deleted `" + DiscordUtils.getAsTag(message.getAuthor()) + "`'s message in channel " + message.getChannel().getAsMention() + " (ID: `" + message.getChannelId() + "`). Message's content: ```\n" + message.getContentDisplay().replace("```", "\\`\\`\\`"), Message.MAX_CONTENT_LENGTH - 5) + "\n```");
                    },
                    failure -> event.getHook().sendMessage("Failed to retrieve the message.").queue()
            );
        } catch (Exception e) {
            hook.sendMessage("Failed to retrieve the message.").queue();
        }
    }
}
