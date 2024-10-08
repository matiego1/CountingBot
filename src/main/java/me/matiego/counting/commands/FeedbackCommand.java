package me.matiego.counting.commands;

import me.matiego.counting.ChannelData;
import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
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

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class FeedbackCommand extends CommandHandler {
    private final Main plugin;

    public FeedbackCommand(@NotNull Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns the slash command.
     *
     * @return the slash command
     */
    @Override
    public @NotNull SlashCommandData getCommand() {
        return createSlashCommand("feedback", false);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        event.replyModal(
                Modal.create("feedback-modal", Translation.COMMANDS__FEEDBACK__TITLE.toString())
                        .addComponents(
                                ActionRow.of(TextInput.create("subject", Translation.COMMANDS__FEEDBACK__SUBJECT.toString(), TextInputStyle.SHORT)
                                        .setRequired(true)
                                        .setPlaceholder(Translation.COMMANDS__FEEDBACK__SUBJECT_PLACEHOLDER.toString())
                                        .build()),
                                ActionRow.of(TextInput.create("description", Translation.COMMANDS__FEEDBACK__MODAL_DESCRIPTION.toString(), TextInputStyle.PARAGRAPH)
                                        .setRequired(true)
                                        .build())
                        )
                        .build()
        ).queue();
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
                Member member = event.getMember();
                Utils.async(() -> openChannels(category, event.getHook(), DiscordUtils.getMemberAsTag(user, member), DiscordUtils.getAvatar(user, member), user));
                return;
            }

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Feedback - " + subject);
            eb.setDescription(description);
            eb.setTimestamp(Instant.now());
            eb.setColor(Color.BLUE);
            eb.setAuthor(DiscordUtils.getAsTag(user), null, user.getEffectiveAvatarUrl());
            eb.setFooter("Received at", event.getJDA().getSelfUser().getEffectiveAvatarUrl());

            JDA jda = plugin.getJda();
            if (jda == null) {
                event.reply(Translation.COMMANDS__FEEDBACK__FAILURE.toString()).setEphemeral(true).queue();
                return;
            }

            TextChannel chn = jda.getTextChannelById(plugin.getConfig().getLong("logs-channel-id"));
            if (chn != null) {
                chn.sendMessageEmbeds(eb.build()).queue();
                event.reply(Translation.COMMANDS__FEEDBACK__SUCCESS.toString()).setEphemeral(true).queue();
            } else {
                event.reply(Translation.COMMANDS__FEEDBACK__FAILURE.toString()).setEphemeral(true).queue();
            }

            plugin.getCommandHandler().putCooldown(user, "feedback", 15 * Utils.SECOND);
        }
    }

    private void openChannels(@NotNull Category category, @NotNull InteractionHook hook, @NotNull String footer, @NotNull String footerUrl, @NotNull User user) {
        int success = 0;
        for (ChannelData.Type type : ChannelData.Type.values()) {
            TextChannel chn = category.createTextChannel(type.toString()).complete();

            List<Webhook> webhooks = chn.retrieveWebhooks().complete();
            Webhook webhook = webhooks.isEmpty() ? chn.createWebhook("Counting bot").complete() : webhooks.getFirst();

            if (plugin.getStorage().addChannel(new ChannelData(chn.getIdLong(), chn.getGuild().getIdLong(), type, webhook)) == Response.SUCCESS) {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle(Translation.GENERAL__OPEN_EMBED__TITLE.toString());
                eb.setDescription(Translation.GENERAL__OPEN_EMBED__DESCRIPTION.getFormatted(type, type.getDescription()));
                eb.setColor(Color.GREEN);
                eb.setTimestamp(Instant.now());
                eb.setFooter(footer, footerUrl);
                chn.sendMessageEmbeds(eb.build()).queue(message -> message.pin().queue());

                Logs.info(DiscordUtils.getAsTag(user) + " opened counting channel " + chn.getAsMention() + " (ID: `" + chn.getId() + "`)");

                success++;
            }
        }
        hook.sendMessage(Translation.COMMANDS__FEEDBACK__OPEN_CHANNELS.getFormatted(success, ChannelData.Type.values().length)).queue();
    }

}
