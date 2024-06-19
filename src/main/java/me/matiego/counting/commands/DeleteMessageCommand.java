package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.FixedSizeMap;
import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
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

public class DeleteMessageCommand extends CommandHandler {
    public DeleteMessageCommand(@NotNull Main plugin) {
        this.plugin = plugin;
    }
    private final Main plugin;
    private final FixedSizeMap<Long, Long> messages = new FixedSizeMap<>(1000);

    /**
     * Returns the slash command.
     *
     * @return the slash command
     */
    @Override
    public @NotNull CommandData getCommand() {
        return Commands.message("delete this message")
                .setNameLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__DELETE_MESSAGE__NAME.toString()))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
                .setGuildOnly(true);
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteraction event) {
        if (!event.getName().equals("delete this message")) return;
        MessageChannelUnion union = event.getChannel();
        if (union == null) {
            event.reply(Translation.COMMANDS__DELETE_MESSAGE__FAILURE__NO_PERMISSION.toString()).setEphemeral(true).queue();
            return;
        }
        if (plugin.getStorage().getChannel(union.getIdLong()) == null) {
            event.reply(Translation.COMMANDS__DELETE_MESSAGE__FAILURE__NO_PERMISSION.toString()).setEphemeral(true).queue();
            return;
        }
        messages.put(event.getUser().getIdLong(), event.getTarget().getIdLong());
        event.replyModal(
                Modal.create("delete-msg-modal", Translation.COMMANDS__DELETE_MESSAGE__MODAL__NAME.toString())
                        .addComponents(
                                ActionRow.of(TextInput.create("admin-key", Translation.COMMANDS__DELETE_MESSAGE__MODAL__OPTION.toString(), TextInputStyle.SHORT)
                                        .setRequired(true)
                                        .build())
                        )
                        .build()
        ).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteraction event) {
        if (!event.getModalId().equals("delete-msg-modal")) return;
        User user = event.getUser();

        ModalMapping mapping = event.getValue("admin-key");
        if (mapping == null) return;
        String key = mapping.getAsString();
        if (key.equalsIgnoreCase("null")) {
            event.reply(Translation.GENERAL__INCORRECT_ADMIN_KEY.toString()).setEphemeral(true).queue();
            return;
        }
        if (!Utils.checkAdminKey(key, user)) {
            event.reply(Translation.GENERAL__INCORRECT_ADMIN_KEY.toString()).setEphemeral(true).queue();
            return;
        }

        Long messageId = messages.remove(user.getIdLong());
        if (messageId == null) {
            event.reply(Translation.COMMANDS__DELETE_MESSAGE__FAILURE__RETRIEVE_MESSAGE.toString()).setEphemeral(true).queue();
            return;
        }

        MessageChannelUnion union = event.getChannel();
        if (union.getType() != ChannelType.TEXT) {
            event.reply(Translation.COMMANDS__DELETE_MESSAGE__FAILURE__RETRIEVE_MESSAGE.toString()).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();
        union.asTextChannel().retrieveMessageById(messageId).queue(
                message -> {
                    message.delete().queue();
                    event.getHook().sendMessage(Translation.COMMANDS__DELETE_MESSAGE__SUCCESS.toString()).queue();

                    Logs.info(Utils.checkLength("User " + Utils.getAsTag(user) + " deleted `" + Utils.getAsTag(message.getAuthor()) + "`'s message in channel " + message.getChannel().getAsMention() + " (ID: `" + message.getChannelId() + "`). Message's content: ```\n" + message.getContentDisplay().replace("```", "\\`\\`\\`"), Message.MAX_CONTENT_LENGTH - 5) + "\n```");
                },
                failure -> event.getHook().sendMessage(Translation.COMMANDS__DELETE_MESSAGE__FAILURE__RETRIEVE_MESSAGE.toString()).queue()
        );
    }
}
