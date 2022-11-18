package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.FixedSizeMap;
import me.matiego.counting.utils.ICommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.ModalInteraction;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.context.MessageContextInteraction;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

public class DeleteMessageCommand implements ICommandHandler {

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
        if (Main.getInstance().getStorage().getChannel(union.getIdLong()) == null) {
            event.reply(Translation.COMMANDS__DELETE_MESSAGE__FAILURE__NO_PERMISSION.toString()).setEphemeral(true).queue();
            return;
        }
        messages.put(event.getUser().getIdLong(), event.getTarget().getIdLong());
        event.replyModal(
                Modal.create("delete-msg-modal", Translation.COMMANDS__DELETE_MESSAGE__MODAL__NAME.toString())
                        .addActionRows(
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

        ModalMapping mapping = event.getValue("admin-key");
        if (mapping == null) return;
        String key = mapping.getAsString();
        if (key.equalsIgnoreCase("null")) {
            event.reply(Translation.GENERAL__INCORRECT_ADMIN_KEY.toString()).setEphemeral(true).queue();
            return;
        }
        if (!Main.getInstance().getConfig().getString("admin-key", "null").equals(key)) {
            event.reply(Translation.GENERAL__INCORRECT_ADMIN_KEY.toString()).setEphemeral(true).queue();
            return;
        }

        Long messageId = messages.remove(event.getUser().getIdLong());
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
                },
                failure -> event.getHook().sendMessage(Translation.COMMANDS__DELETE_MESSAGE__FAILURE__RETRIEVE_MESSAGE.toString()).queue()
        );
    }
}
