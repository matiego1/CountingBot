package me.matiego.counting.commands;

import me.matiego.counting.ChannelData;
import me.matiego.counting.Main;
import me.matiego.counting.utils.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class FeedbackCommand extends CommandHandler {
    public FeedbackCommand(@NotNull Main instance) {
        this.instance = instance;
    }
    private final Main instance;

    @Override
    public @NotNull SlashCommandData getCommand() {
        return createSlashCommand(
                "feedback",
                "Zgłoś błąd, zaproponuj zmianę lub podziel się swoją opinią o tym bocie",
                false
        );
    }

    @Override
    public @NotNull CompletableFuture<Integer> onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        event.replyModal(
                Modal.create("feedback-modal", "Wyślij opinię")
                        .addComponents(
                                ActionRow.of(TextInput.create("subject", "Tytuł", TextInputStyle.SHORT)
                                        .setRequired(true)
                                        .setPlaceholder("np. zgłoszenie błędu, propozycja, opinia")
                                        .build()),
                                ActionRow.of(TextInput.create("description", "Opis", TextInputStyle.PARAGRAPH)
                                        .setRequired(true)
                                        .build())
                        )
                        .build()
        ).queue();
        return CompletableFuture.completedFuture(2);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteraction event) {
        if (event.getModalId().equals("feedback-modal")) {
            User user = event.getUser();
            String subject = Objects.requireNonNull(event.getValue("subject")).getAsString();
            String description = Objects.requireNonNull(event.getValue("description")).getAsString();

            openChannels: {
                if (!DiscordUtils.checkAdminKey(subject, user)) break openChannels;
                Guild guild = event.getGuild();
                if (guild == null) break openChannels;
                Category category = guild.getCategoryById(description);
                if (category == null) break openChannels;
                event.deferReply(true).queue();
                Utils.async(() -> openChannels(category, event.getHook(), user, event.getMember()));
                return;
            }

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Feedback - " + subject);
            eb.setDescription(description);
            eb.setTimestamp(Instant.now());
            eb.setColor(Color.BLUE);
            eb.setAuthor(DiscordUtils.getAsTag(user), null, user.getEffectiveAvatarUrl());
            eb.setFooter("Received at", event.getJDA().getSelfUser().getEffectiveAvatarUrl());

            TextChannel chn = event.getJDA().getTextChannelById(instance.getConfig().getLong("logs-channel-id"));
            if (chn != null) {
                chn.sendMessageEmbeds(eb.build()).queue(
                        success -> event.reply("Dziękuję za twoją opinię! :)").setEphemeral(true).queue(),
                        failure -> event.reply(DiscordUtils.checkLength("Napotkano niespodziewany błąd przy wysyłaniu twojej opinii:\n```\n" + description, Message.MAX_CONTENT_LENGTH - 10) + "\n```").queue()
                );
            } else {
                event.reply(DiscordUtils.checkLength("Napotkano niespodziewany błąd przy wysyłaniu twojej opinii:\n```\n" + description, Message.MAX_CONTENT_LENGTH - 10) + "\n```").queue();
            }
        }
    }

    private void openChannels(@NotNull Category category, @NotNull InteractionHook hook, @NotNull User user, @Nullable Member member) {
        Arrays.stream(ChannelData.Type.values())
                .map(type -> openChannel(category, type, user, member))
                .reduce((a, b) -> a.thenCombine(b, Integer::sum))
                .orElseGet(() -> CompletableFuture.completedFuture(0))
                .thenAccept(success -> hook.sendMessage("Successfully opened %s counting channel(s) out of %s.".formatted(success, ChannelData.Type.values().length)).queue());
    }

    private @NotNull CompletableFuture<Integer> openChannel(@NotNull Category category, @NotNull ChannelData.Type type, @NotNull User user, @Nullable Member member) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        category.createTextChannel(type.toString()).queue(
                chn -> DiscordUtils.getOrCreateWebhook(chn)
                        .whenComplete((webhook, e) -> {
                            if (webhook == null) {
                                future.complete(0);
                                return;
                            }
                            future.complete(withWebhook(chn, webhook, type, user, member));
                        }),
                failure -> future.complete(0)
        );
        return future;
    }

    private int withWebhook(@NotNull TextChannel chn, @NotNull Webhook webhook, @NotNull ChannelData.Type type, @NotNull User user, @Nullable Member member) {
        if (instance.getStorage().addChannel(new ChannelData(chn.getIdLong(), chn.getGuild().getIdLong(), type, webhook)) == Response.SUCCESS) {
            EmbedBuilder eb = DiscordUtils.getOpenChannelEmbed(type, user, member);
            chn.sendMessageEmbeds(eb.build()).queue(message -> message.pin().queue());
            DiscordUtils.setSlowmode(instance, chn);

            Logs.info(DiscordUtils.getAsTag(user) + " has opened counting channel " + chn.getAsMention() + " (ID: `" + chn.getId() + "`)");
            return 1;
        } else {
            chn.delete().queue();
        }
        return 0;
    }
}
