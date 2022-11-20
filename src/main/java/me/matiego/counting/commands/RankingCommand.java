package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.UserRanking;
import me.matiego.counting.utils.ICommandHandler;
import me.matiego.counting.utils.Pair;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class RankingCommand implements ICommandHandler {
    /**
     * Returns the slash command.
     *
     * @return the slash command
     */
    @Override
    public @NotNull CommandData getCommand() {
        return Commands.slash("ranking", "Shows user ranking.")
                .setNameLocalizations(Utils.getAllLocalizations("ranking"))
                .setDescriptionLocalizations(Utils.getAllLocalizations("Shows user ranking."))
                .setGuildOnly(true)
                .addOptions(
                        new OptionData(OptionType.STRING, "ephemeral", "whether this message should only be visible to you", false)
                                .setNameLocalizations(Utils.getAllLocalizations("ephemeral"))
                                .setDescriptionLocalizations(Utils.getAllLocalizations("whether this message should only be visible to you"))
                                .addChoice("YES", "True")
                                .addChoice("NO", "False"),
                        new OptionData(OptionType.INTEGER, "amount", "number of top places")
                                .setNameLocalizations(Utils.getAllLocalizations("amount"))
                                .setDescriptionLocalizations(Utils.getAllLocalizations("number of top places"))
                                .setRequiredRange(1, 15),
                        new OptionData(OptionType.USER, "user", "executes the command as this user [debug-only]")
                );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        boolean ephemeral = event.getOption("ephemeral", "False", OptionMapping::getAsString).equals("True");
        if (Main.getInstance().getStorage().getChannel(event.getChannel().getIdLong()) != null) ephemeral = true;
        event.deferReply(ephemeral).queue();

        User user = event.getUser();
        long guild = Objects.requireNonNull(event.getGuild()).getIdLong();
        UserRanking ranking = Main.getInstance().getUserRanking();

        int option = event.getOption("amount", 10, OptionMapping::getAsInt);
        User userOption = event.getOption("user", user, OptionMapping::getAsUser);

        Utils.async(() -> {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("**Top " + option + " users**");
            eb.setTimestamp(Instant.now());
            eb.setFooter(Utils.getMemberAsTag(user, event.getMember()), Utils.getAvatar(user, event.getMember()));
            eb.setColor(Color.YELLOW);

            List<Pair<Integer, String>> top = ranking.getTop(guild, option).stream()
                    .map(pair -> new Pair<>(pair.getSecond(), "<@" + pair.getFirst() + ">"))
                    .toList();
            StringBuilder builder = new StringBuilder();
            int lastPoints = -1, currentPlace = 0;
            boolean isUserInTop = false;

            for (Pair<Integer, String> pair : top) {
                if (pair.getSecond().contains(userOption.getId())) isUserInTop = true;
                if (pair.getFirst() != lastPoints) {
                    lastPoints = pair.getFirst();
                    currentPlace++;
                }
                builder.append("**");
                builder.append(
                        switch (currentPlace) {
                            case 1 -> ":first_place:";
                            case 2 -> ":second_place:";
                            case 3 -> ":third_place:";
                            default -> currentPlace + ".";
                        }
                );
                builder.append("** ").append(pair.getSecond()).append(" - ").append(pair.getFirst()).append(" messages").append("\n");
            }

            if (!isUserInTop) {
                int pos = ranking.getPosition(userOption, guild) - 1, amount = ranking.get(userOption, guild);
                if (pos > 0 && amount > 0) {
                    if (pos > currentPlace + 1) builder.append("...\n");
                    builder.append("**");
                    builder.append(
                            switch (pos) {
                                case 1 -> ":first_place:";
                                case 2 -> ":second_place:";
                                case 3 -> ":third_place:";
                                default -> pos + ".";
                            }
                    );
                    builder.append("** <@").append(userOption.getId()).append("> - ").append(amount).append(" messages").append("\n");
                }
            }

            eb.setDescription(builder.toString());
            event.getHook().sendMessageEmbeds(eb.build()).queue();
        });
    }
}
