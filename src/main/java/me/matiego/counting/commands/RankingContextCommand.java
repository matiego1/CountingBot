package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.Tasks;
import me.matiego.counting.UserRanking;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.DiscordUtils;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.context.UserContextInteraction;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Objects;

public class RankingContextCommand extends CommandHandler {
    public RankingContextCommand(@NotNull Main instance) {
        this.instance = instance;
    }

    private final Main instance;
    @Override
    public @NotNull CommandData getCommand() {
        return Commands.user("ranking")
                .setContexts(InteractionContextType.GUILD);
    }

    @Override
    public void onUserContextInteraction(@NotNull UserContextInteraction event) {
        if (!event.getName().equals("ranking")) return;
        event.deferReply(true).queue();

        InteractionHook hook = event.getHook();
        User target = event.getTarget();
        User user = event.getUser();

        Tasks.async(() -> {
            UserRanking.Data data = instance.getUserRanking().get(target, Objects.requireNonNull(event.getGuild()).getIdLong());
            if (data == null) {
                hook.sendMessage("Ten użytkownik nie wysłał jeszcze żadnej wiadomości albo napotkano niespodziewany błąd.").queue();
                return;
            }

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTimestamp(Instant.now());
            eb.setFooter(DiscordUtils.getMemberAsTag(user, event.getMember()), DiscordUtils.getAvatar(user, event.getMember()));
            eb.setColor(Utils.YELLOW);
            eb.setDescription("%s wysłał `%s` wiadomości - `%s` miejsce w rankingu.".formatted(target.getAsMention(), data.getScore(), data.getRank()));
            hook.sendMessageEmbeds(eb.build())
                    .addActionRow(
                            Button.success("not-ephemeral", "Pokaż tę wiadomość wszystkim")
                    ).queue();
        });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteraction event) {
        if (!event.getComponentId().equals("not-ephemeral")) return;

        MessageChannel chn = event.getChannel();
        Message message = event.getMessage();
        event.deferReply(true).queue();
        event.editButton(event.getButton().asDisabled()).queue();

        if (instance.getStorage().getChannel(chn.getIdLong()) != null) {
            event.getHook().sendMessage("Nie możesz pokazać tej wiadomości wszystkim na kanale do liczenia.").queue();
            return;
        }

        if (chn.canTalk()) {
            chn.sendMessage(message.getContentDisplay()).setEmbeds(message.getEmbeds()).queue();
            event.getHook().sendMessage("Sukces.").queue();
        } else {
            event.getHook().sendMessage("Napotkano niespodziewany błąd.").queue();
        }
    }
}
