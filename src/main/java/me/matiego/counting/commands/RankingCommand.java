package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.UserRanking;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class RankingCommand extends CommandHandler {
    public RankingCommand(@NotNull Main plugin) {
        this.plugin = plugin;
    }
    private final Main plugin;

    /**
     * Returns the slash command.
     *
     * @return the slash command
     */
    @Override
    public @NotNull CommandData getCommand() {
        return createSlashCommand("ranking", true)
                .addOptions(
                        EPHEMERAL_OPTION,
                        createOption(
                                "amount",
                                OptionType.INTEGER,
                                false,
                                Translation.COMMANDS__RANKING__OPTIONS__AMOUNT__NAME,
                                Translation.COMMANDS__RANKING__OPTIONS__AMOUNT__DESCRIPTION
                        ).setRequiredRange(5, 30)
                );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        boolean ephemeral = event.getOption("ephemeral", "False", OptionMapping::getAsString).equals("True");
        if (plugin.getStorage().getChannel(event.getChannel().getIdLong()) != null) ephemeral = true;
        event.deferReply(ephemeral).queue();

        User user = event.getUser();
        InteractionHook hook = event.getHook();

        int option = event.getOption("amount", 10, OptionMapping::getAsInt);

        Utils.async(() -> {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTimestamp(Instant.now());
            eb.setFooter(Utils.getMemberAsTag(user, event.getMember()), Utils.getAvatar(user, event.getMember()));
            eb.setColor(Color.YELLOW);

            List<UserRanking.Data> top = plugin.getUserRanking().getTop(Objects.requireNonNull(event.getGuild()).getIdLong(), option);

            if (top.size() <= 1) {
                hook.sendMessage(Translation.COMMANDS__RANKING__EMPTY.toString()).queue();
                plugin.getCommandHandler().putSlowdown(event.getUser(), event.getName(), 3 * Utils.SECOND);
                return;
            }

            int total = 0, total_guild = 0;
            StringBuilder builder = new StringBuilder();

            for (UserRanking.Data data : top) {
                if (data.getUser().getIdLong() == 0) {
                    total = data.getScore();
                    total_guild = data.getRank();
                    continue;
                }

                String place = switch (data.getRank()) {
                    case 1 -> ":first_place:";
                    case 2 -> ":second_place:";
                    case 3 -> ":third_place:";
                    default -> data.getRank() + ".";
                };
                builder.append(Translation.COMMANDS__RANKING__ROW.getFormatted("**" + place + "**", data.getUser().getAsMention(), data.getScore())).append("\n");
            }

            String description = Utils.checkLength(Translation.COMMANDS__RANKING__HEADER.getFormatted(total, total_guild) + builder, MessageEmbed.DESCRIPTION_MAX_LENGTH);
            if (description.endsWith("...")) {
                description = description.substring(0, description.lastIndexOf("\n") + 1) + "...";
            }

            eb.setDescription(description);
            eb.setTitle(Translation.COMMANDS__RANKING__TITLE.getFormatted(top.size() - 1));

            hook.sendMessageEmbeds(eb.build()).queue();
            plugin.getCommandHandler().putSlowdown(event.getUser(), event.getName(), 5 * Utils.SECOND);
        });
    }
}
