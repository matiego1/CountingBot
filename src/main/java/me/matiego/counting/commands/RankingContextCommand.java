package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.UserRanking;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.DiscordUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.context.UserContextInteraction;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.Objects;

public class RankingContextCommand extends CommandHandler {
    /**
     * Returns the slash command.
     *
     * @return the slash command
     */
    @Override
    public @NotNull CommandData getCommand() {
        return Commands.user("get number of sent messages")
                .setNameLocalizations(DiscordUtils.getAllLocalizations(Translation.COMMANDS__RANKING_CONTEXT__NAME.toString()))
                .setGuildOnly(true);
    }

    @Override
    public void onUserContextInteraction(@NotNull UserContextInteraction event) {
        if (!event.getName().equals("get number of sent messages")) return;
        event.deferReply(true).queue();

        User target = event.getTarget();
        User user = event.getUser();
        InteractionHook hook = event.getHook();

        UserRanking.Data data = Main.getInstance().getUserRanking().get(target, Objects.requireNonNull(event.getGuild()).getIdLong());
        if (data == null) {
            hook.sendMessage(Translation.COMMANDS__RANKING_CONTEXT__EMPTY.toString()).queue();
            return;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTimestamp(Instant.now());
        eb.setFooter(DiscordUtils.getMemberAsTag(user, event.getMember()), DiscordUtils.getAvatar(user, event.getMember()));
        eb.setColor(Color.YELLOW);
        eb.setDescription(Translation.COMMANDS__RANKING_CONTEXT__MESSAGE.getFormatted(target.getAsMention(), data.getScore(), data.getRank()));
        hook.sendMessageEmbeds(eb.build())
                .addActionRow(
                        Button.success("not-ephemeral", Translation.COMMANDS__RANKING_CONTEXT__BUTTON.toString())
                ).queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteraction event) {
        if (!event.getComponentId().equals("not-ephemeral")) return;

        MessageChannel chn = event.getChannel();
        Message message = event.getMessage();
        event.deferReply(true).queue();
        event.editButton(event.getButton().asDisabled()).queue();

        if (Main.getInstance().getStorage().getChannel(chn.getIdLong()) == null && chn.canTalk()) {
            chn.sendMessage(message.getContentDisplay()).setEmbeds(message.getEmbeds()).queue();
            event.getHook().sendMessage(Translation.COMMANDS__RANKING_CONTEXT__SUCCESS.toString()).queue();
        } else {
            event.getHook().sendMessage(Translation.COMMANDS__RANKING_CONTEXT__FAILURE.toString()).queue();
        }
    }
}
