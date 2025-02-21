package me.matiego.counting.commands;

import me.matiego.counting.Main;
import me.matiego.counting.UserRanking;
import me.matiego.counting.utils.CommandHandler;
import me.matiego.counting.utils.DiscordUtils;
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

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RankingCommand extends CommandHandler {
    public RankingCommand(@NotNull Main instance) {
        this.instance = instance;
    }
    private final Main instance;

    @Override
    public @NotNull CommandData getCommand() {
        return createSlashCommand(
                "ranking",
                "Pokazuje ranking użytkowników",
                true
        ).addOptions(
                EPHEMERAL_OPTION,
                createOption(
                        "amount",
                        "Ilość miejsc do pokazania",
                        OptionType.INTEGER,
                        false
                ).setRequiredRange(5, 30)
        );
    }

    @Override
    public @NotNull CompletableFuture<Integer> onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        boolean ephemeral = event.getOption("ephemeral", "False", OptionMapping::getAsString).equals("True");
        if (instance.getStorage().getChannel(event.getChannel().getIdLong()) != null) ephemeral = true;

        event.deferReply(ephemeral).queue();
        InteractionHook hook = event.getHook();

        User user = event.getUser();
        int option = event.getOption("amount", 10, OptionMapping::getAsInt);

        CompletableFuture<Integer> cooldown = new CompletableFuture<>();
        Utils.async(() -> {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTimestamp(Instant.now());
            eb.setFooter(DiscordUtils.getMemberAsTag(user, event.getMember()), DiscordUtils.getAvatar(user, event.getMember()));
            eb.setColor(Utils.YELLOW);

            List<UserRanking.Data> top = instance.getUserRanking().getTop(Objects.requireNonNull(event.getGuild()).getIdLong(), option);

            if (top.size() <= 1) {
                hook.sendMessage("Ranking jest pusty albo napotkano niespodziewany błąd.").queue();
                cooldown.complete(10);
                return;
            }

            int total = 0, total_guild = 0;
            StringBuilder builder = new StringBuilder();

            // TODO: rewrite - add pages
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
                builder.append("**%s** %s - %s message(s)\n".formatted(place, data.getUser().getAsMention(), data.getScore()));
            }

            String description = DiscordUtils.checkLength("**Łącznie `%s` wiadomości, w tym `%s` na tym serwerze**\n\n".formatted(total, total_guild) + builder, MessageEmbed.DESCRIPTION_MAX_LENGTH);
            if (description.endsWith("...")) {
                description = description.substring(0, description.lastIndexOf("\n") + 1) + "...";
            }

            eb.setDescription(description);
            eb.setTitle("**" + (top.size() - 1) + " najlepszych miejsc**");

            hook.sendMessageEmbeds(eb.build()).queue();
            cooldown.complete(10);
        });
        return cooldown;
    }
}
