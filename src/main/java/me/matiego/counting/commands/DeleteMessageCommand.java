package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.utils.ICommandHandler;
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
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
                .setGuildOnly(true);
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteraction event) {
        if (!event.getName().equals("delete this message")) return;
        MessageChannelUnion union = event.getChannel();
        if (union == null) {
            event.reply("You cannot delete this message.").setEphemeral(true).queue();
            return;
        }
        if (Main.getInstance().getStorage().getChannel(union.getIdLong()) == null) {
            event.reply("You cannot delete this message.").setEphemeral(true).queue();
            return;
        }
        if (event.getUser().getIdLong() != Main.getInstance().getConfig().getLong("admin-user-id")) {
            event.reply("You cannot delete this message.").setEphemeral(true).queue();
            return;
        }
        event.getTarget().delete().queue();
        event.reply("Success!").setEphemeral(true).queue();
    }
}
