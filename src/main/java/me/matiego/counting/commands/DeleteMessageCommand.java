package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.ICommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.context.MessageContextInteraction;
import org.jetbrains.annotations.NotNull;

public class DeleteMessageCommand implements ICommandHandler {
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
            event.reply(Translation.COMMANDS__DELETE_MESSAGE__FAILURE.toString()).setEphemeral(true).queue();
            return;
        }
        if (Main.getInstance().getStorage().getChannel(union.getIdLong()) == null) {
            event.reply(Translation.COMMANDS__DELETE_MESSAGE__FAILURE.toString()).setEphemeral(true).queue();
            return;
        }
        if (event.getUser().getIdLong() != Main.getInstance().getConfig().getLong("admin-user-id")) {
            event.reply(Translation.COMMANDS__DELETE_MESSAGE__FAILURE.toString()).setEphemeral(true).queue();
            return;
        }
        event.getTarget().delete().queue();
        event.reply(Translation.COMMANDS__DELETE_MESSAGE__SUCCESS.toString()).setEphemeral(true).queue();
    }
}
