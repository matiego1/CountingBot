package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.UserRanking;
import me.matiego.counting.utils.ICommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.context.UserContextInteraction;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.Objects;

public class RankingContextCommand implements ICommandHandler {
    /**
     * Returns the slash command.
     *
     * @return the slash command
     */
    @Override
    public @NotNull CommandData getCommand() {
        return Commands.user("get number of sent messages")
                .setNameLocalizations(Utils.getAllLocalizations("ranking"))
                .setGuildOnly(true);
    }

    @Override
    public void onUserContextInteraction(@NotNull UserContextInteraction event) {
        if (!event.getName().equals("get number of sent messages")) return;
        event.deferReply(true).queue();

        User target = event.getTarget();
        User user = event.getUser();
        long guild = Objects.requireNonNull(event.getGuild()).getIdLong();
        UserRanking ranking = Main.getInstance().getUserRanking();

        UserRanking.Data data = ranking.get(target, guild);
        if (data == null) {
            event.getHook().sendMessage("This user never sent any message or an error occurred.").queue();
            return;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTimestamp(Instant.now());
        eb.setFooter(Utils.getMemberAsTag(user, event.getMember()), Utils.getAvatar(user, event.getMember()));
        eb.setColor(Color.YELLOW);
        eb.setDescription(target.getAsMention() + " has sent `" + data.getScore() + "` message(s) - `" + data.getRank() + "` place in ranking.");
        event.getHook().sendMessageEmbeds(eb.build())
                .addActionRow(
                        Button.success("not-ephemeral", "Send me as not ephemeral")
                ).queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteraction event) {
        if (!event.getComponentId().equals("not-ephemeral")) return;
        Message message = event.getMessage();
        event.reply(message.getContentDisplay()).setEmbeds(message.getEmbeds()).queue();
    }
}
