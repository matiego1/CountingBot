package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.UserRanking;
import me.matiego.counting.utils.ICommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
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
        return Commands.slash("ranking", Translation.COMMANDS__RANKING__DESCRIPTION.getDefault())
                .setNameLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__RANKING__NAME.toString()))
                .setDescriptionLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__RANKING__DESCRIPTION.toString()))
                .setGuildOnly(true)
                .addOptions(
                        new OptionData(OptionType.STRING, "ephemeral", Translation.COMMANDS__RANKING__OPTIONS__EPHEMERAL__DESCRIPTION.getDefault(), false)
                                .setNameLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__RANKING__OPTIONS__EPHEMERAL__NAME.toString()))
                                .setDescriptionLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__RANKING__OPTIONS__EPHEMERAL__DESCRIPTION.toString()))
                                .addChoice(Translation.COMMANDS__RANKING__OPTIONS__EPHEMERAL__TRUE.toString(), "True")
                                .addChoice(Translation.COMMANDS__RANKING__OPTIONS__EPHEMERAL__FALSE.toString(), "False"),
                        new OptionData(OptionType.INTEGER, "amount", Translation.COMMANDS__RANKING__OPTIONS__AMOUNT__DESCRIPTION.getDefault(), false)
                                .setNameLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__RANKING__OPTIONS__AMOUNT__NAME.toString()))
                                .setDescriptionLocalizations(Utils.getAllLocalizations(Translation.COMMANDS__RANKING__OPTIONS__AMOUNT__DESCRIPTION.toString()))
                                .setRequiredRange(1, 15)
                );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        boolean ephemeral = event.getOption("ephemeral", "False", OptionMapping::getAsString).equals("True");
        if (Main.getInstance().getStorage().getChannel(event.getChannel().getIdLong()) != null) ephemeral = true;
        event.deferReply(ephemeral).queue();

        User user = event.getUser();
        InteractionHook hook = event.getHook();

        int option = event.getOption("amount", 10, OptionMapping::getAsInt);

        Utils.async(() -> {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(Translation.COMMANDS__RANKING__TITLE.getFormatted(option));
            eb.setTimestamp(Instant.now());
            eb.setFooter(Utils.getMemberAsTag(user, event.getMember()), Utils.getAvatar(user, event.getMember()));
            eb.setColor(Color.YELLOW);

            List<UserRanking.Data> top = Main.getInstance().getUserRanking().getTop(Objects.requireNonNull(event.getGuild()).getIdLong(), option);
            StringBuilder builder = new StringBuilder();

            for (UserRanking.Data data : top) {
                String place = switch (data.getRank()) {
                    case 1 -> ":first_place:";
                    case 2 -> ":second_place:";
                    case 3 -> ":third_place:";
                    default -> data.getRank() + ".";
                };
                builder.append(Translation.COMMANDS__RANKING__ROW.getFormatted("**" + place + "**", data.getUser().getAsMention(), data.getScore())).append("\n");
            }

            String description = builder.toString();
            if (description.isBlank()) {
                hook.sendMessage(Translation.COMMANDS__RANKING__EMPTY.toString()).queue();
                return;
            }
            description = Utils.checkLength(description, MessageEmbed.DESCRIPTION_MAX_LENGTH);
            if (description.endsWith("...")) {
                description = description.substring(0, description.lastIndexOf("\n") + 1) + "...";
            }
            eb.setDescription(description);
            hook.sendMessageEmbeds(eb.build()).queue();
        });
    }
}
