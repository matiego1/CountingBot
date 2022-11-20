package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.UserRanking;
import me.matiego.counting.utils.ICommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.context.UserContextInteraction;
import org.jetbrains.annotations.NotNull;

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
        long guild = Objects.requireNonNull(event.getGuild()).getIdLong();
        UserRanking ranking = Main.getInstance().getUserRanking();

        int pos = ranking.getPosition(target, guild);
        int amount = ranking.get(target, guild);
        if (pos <= 0 || amount <= 0) {
            event.getHook().sendMessage("This user never sent any message, or an error was encountered.").queue();
            return;
        }
        System.out.println("POS" + pos);
        event.getHook().sendMessage(target.getAsMention() + " has sent " + amount + " message(s) - " + pos + " place in ranking.").queue();
    }
}
