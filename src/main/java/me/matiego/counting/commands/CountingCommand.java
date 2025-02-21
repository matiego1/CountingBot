package me.matiego.counting.commands;

import me.matiego.counting.ChannelData;
import me.matiego.counting.Main;
import me.matiego.counting.utils.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.attribute.IPostContainer;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumPost;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectInteraction;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.utils.SplitUtil;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class CountingCommand extends CommandHandler {
    private final Main instance;

    public CountingCommand(@NotNull Main instance) {
        this.instance = instance;
    }

    @Override
    public @NotNull SlashCommandData getCommand() {
        return createSlashCommand(
                "counting",
                "Manages the counting channels",
                true,
                Permission.MANAGE_CHANNEL
        ).addSubcommands(
                createSubcommand(
                        "open",
                        "Opens a new counting channel"
                ),
                createSubcommand(
                        "close",
                        "Closes the counting channel"
                ),
                createSubcommand(
                        "list",
                        "Shows a list of opened counting channels in this guild"
                ),
                createSubcommand(
                        "create-forum",
                        "Creates a forum channel with all possible counting channels",
                        createOption(
                                "category",
                                "The category to create a forum channel in",
                                OptionType.CHANNEL,
                                false
                        ).setChannelTypes(ChannelType.CATEGORY)
                )
        );
    }

    @Override
    public @NotNull CompletableFuture<Integer> onSlashCommandInteraction(@NotNull SlashCommandInteraction event) {
        event.deferReply(true).queue();
        InteractionHook hook = event.getHook();

        Utils.async(() -> {
            if (!DiscordUtils.isSupportedChannel(event.getChannel())) {
                hook.sendMessage("This channel type is not supported.").queue();
                return;
            }

            switch (String.valueOf(event.getSubcommandName())) {
                case "open" -> handleOpenSubcommand(hook);
                case "close" -> handleCloseSubcommand(event, hook);
                case "list" -> handleListSubcommand(event, hook);
                case "create-forum" -> handleCreateForumSubcommand(event, hook);
            }
        });
        return CompletableFuture.completedFuture(5);
    }

    private void handleOpenSubcommand(@NotNull InteractionHook hook) {
        hook.sendMessage("**__Select type of a new counting channel:__**")
                .addActionRow(
                        StringSelectMenu.create("counting-type")
                                .addOptions(ChannelData.getSelectMenuOptions())
                                .setRequiredRange(1, 1)
                                .build())
                .queue();
    }

    private void handleCloseSubcommand(@NotNull SlashCommandInteraction event, @NotNull InteractionHook hook) {
        User user = event.getUser();
        switch (instance.getStorage().removeChannel(event.getChannel().getIdLong())) {
            case SUCCESS -> {
                MessageChannelUnion chn = event.getChannel();

                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Kanał do liczenia został zamknięty!");
                eb.setColor(Utils.RED);
                eb.setTimestamp(Instant.now());
                eb.setFooter(DiscordUtils.getMemberAsTag(user, event.getMember()), DiscordUtils.getAvatar(user, event.getMember()));
                chn.sendMessageEmbeds(eb.build()).queue();

                hook.sendMessage("The counting channel has been successfully closed.").queue();
                Logs.info(DiscordUtils.getAsTag(user) + " has closed the counting channel " + chn.getAsMention() + " (ID: `" + chn.getId() + "`)");
            }
            case NO_CHANGES -> hook.sendMessage("This channel has already been closed.").queue();
            case FAILURE -> hook.sendMessage("Failed to close this counting channel.").queue();
        }
    }

    private void handleListSubcommand(@NotNull SlashCommandInteraction event, @NotNull InteractionHook hook) {
        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();
        boolean isAdmin = Utils.checkAdminKey(event.getOption("admin-key", OptionMapping::getAsString), event.getUser());

        List<String> channels = new ArrayList<>();
        for (ChannelData data : instance.getStorage().getChannels()) {
            GuildMessageChannel chn = DiscordUtils.getSupportedChannelById(event.getJDA(), data.getChannelId());
            if (chn != null && chn.getGuild().getIdLong() == guildId) {
                channels.add("**" + (channels.size() + 1) + ".** " + chn.getAsMention() + ": " + data.getType());
            } else if (isAdmin) {
                channels.add("**" + (channels.size() + 1) + ".** " + (chn == null ? "`" + data.getChannelId() + "`" : chn.getAsMention()) + ": " + data.getType());
            }
        }

        if (channels.isEmpty()) {
            hook.sendMessage("No counting channel has been opened yet. Open a new one with command `/counting open`").queue();
            return;
        }

        List<String> chunks = SplitUtil.split(
                "**__Open Counting Channels (%s in total):__**".formatted(channels.size()) + "\n" + String.join("\n", channels),
                Message.MAX_CONTENT_LENGTH,
                SplitUtil.Strategy.NEWLINE
        );
        String message = chunks.getFirst();

        if (chunks.size() > 1) {
            List<String> lines = getMessage(message, channels);
            message = String.join("\n", lines);
        }

        hook.sendMessage(message).queue();
    }

    private @NotNull List<String> getMessage(@NotNull String message, @NotNull List<String> channels) {
        final String moreLine = "... and %s more channel(s)";
        final int maxMoreLineLength = moreLine.length() + 10;

        List<String> lines = new ArrayList<>(Arrays.asList(message.split("\n")));

        int removedCharacters = 0;
        do {
            removedCharacters += lines.removeLast().length();
        } while (removedCharacters < maxMoreLineLength);

        int more = channels.size() - (lines.size() - 1);
        lines.add(moreLine.formatted(more));
        return lines;
    }

    private void handleCreateForumSubcommand(@NotNull SlashCommandInteraction event, @NotNull InteractionHook hook) {
        final long allow = Permission.MESSAGE_HISTORY.getRawValue() | Permission.MESSAGE_SEND_IN_THREADS.getRawValue();
        final long deny = Permission.MESSAGE_SEND.getRawValue();

        GuildChannel chn = event.getOption("category", OptionMapping::getAsChannel);
        Guild guild = Objects.requireNonNull(event.getGuild());

        ChannelAction<ForumChannel> action;
        if (chn instanceof Category category) {
            action = category.createForumChannel("Counting")
                    .syncPermissionOverrides();
        } else {
            action = guild.createForumChannel("Counting");
        }
        action
                .setTopic("Kanały do liczenia. Licz na różne sposoby i baw się dobrze!")
                .setDefaultLayout(ForumChannel.Layout.LIST_VIEW)
                .setDefaultSortOrder(IPostContainer.SortOrder.CREATION_TIME)
                .setDefaultReaction(null)
                .queue(
                        forum -> {
                            forum.upsertPermissionOverride(forum.getGuild().getSelfMember()).grant(deny).submit()
                                    .thenCompose(v -> forum.upsertPermissionOverride(forum.getGuild().getPublicRole()).grant(allow).deny(deny).submit());
                            openForumChannels(hook, forum, event.getUser(), event.getMember());
                        },
                        failure -> hook.sendMessage("Failed to create the forum channel: `%s`. Is the community enabled in this guild?".formatted(failure.getMessage())).queue()
                );

    }

    private void openForumChannels(@NotNull InteractionHook hook, @NotNull ForumChannel forum, @NotNull User user, @Nullable Member member) {
        DiscordUtils.getOrCreateWebhook(forum)
                .whenComplete((webhook, e) -> {
                    if (webhook == null) {
                        hook.sendMessage("Failed to create a webhook.").queue();
                        return;
                    }
                    withWebhook(hook, forum, webhook, user, member);
                });
    }

    private void withWebhook(@NotNull InteractionHook hook, @NotNull ForumChannel forum, @NotNull Webhook webhook, @NotNull User user, @Nullable Member member) {
        ChannelData.Type[] types = ChannelData.Type.values();
        ArrayUtils.reverse(types);
        Arrays.stream(types)
                .map(type -> openForumChannel(forum, type, webhook, user, member))
                .reduce((a, b) -> a.thenCombine(b, Integer::sum))
                .orElse(CompletableFuture.completedFuture(0))
                .thenAccept(success -> hook.sendMessage("Successfully opened %s counting channel(s) out of %s.".formatted(success, types.length)).queue());
    }

    private @NotNull CompletableFuture<Integer> openForumChannel(@NotNull ForumChannel forum, @NotNull ChannelData.Type type, @NotNull Webhook webhook, @NotNull User user, @Nullable Member member) {
        EmbedBuilder eb = DiscordUtils.getOpenChannelEmbed(type, user, member);
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Logs.infoLocal("[DEBUG] tworze");
        forum.createForumPost(
                type.toString(),
                MessageCreateData.fromEmbeds(eb.build())
        ).queue(
                post -> {
                    Logs.infoLocal("[DEBUG] juz");
                    future.complete(withForumPost(post, type, webhook, user));
                },
                failure -> {
                    Logs.warning("Failed to open the forum channel", failure);
                    future.complete(0);
                }
        );
        return future;
    }

    private int withForumPost(@NotNull ForumPost post, @NotNull ChannelData.Type type, @NotNull Webhook webhook, @NotNull User user) {
        post.getMessage().pin().queue();

        ThreadChannel chn = post.getThreadChannel();
        if (instance.getStorage().addChannel(new ChannelData(chn.getIdLong(), chn.getGuild().getIdLong(), type, webhook)) == Response.SUCCESS) {
            DiscordUtils.setSlowmode(instance, chn);

            Logs.info(DiscordUtils.getAsTag(user) + " has opened a new counting channel " + chn.getAsMention() + ". (ID: `" + chn.getId() + "`)");
            return 1;
        } else {
            chn.delete().queue();
        }
        return 0;
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteraction event) {
        User user = event.getUser();
        if (event.getComponentId().equals("counting-type")) {
            event.deferReply(true).queue();
            Utils.async(() -> {
                ChannelData.Type type = Arrays.stream(ChannelData.Type.values())
                        .filter(value -> value.toString().equals(event.getValues().getFirst()))
                        .findFirst()
                        .orElse(null);
                if (type == null) {
                    reply(event, "Unknown counting channel type.");
                    return;
                }

                MessageChannelUnion chn = event.getChannel();
                if (!DiscordUtils.isSupportedChannel(chn)) {
                    reply(event, "This channel type is not supported.");
                    return;
                }

                IWebhookContainer webhookChannel = switch (event.getChannelType()) {
                    case TEXT -> chn.asTextChannel();
                    case GUILD_PUBLIC_THREAD -> chn
                            .asThreadChannel()
                            .getParentChannel()
                            .asForumChannel();
                    default -> null;
                };
                if (webhookChannel == null) {
                    reply(event, "This channel type is not supported.");
                    return;
                }

                DiscordUtils.getOrCreateWebhook(webhookChannel)
                        .whenComplete((webhook, e) -> {
                            if (webhook == null) {
                                reply(event, "Failed to create a webhook.");
                                return;
                            }
                            openChannel(chn.asGuildMessageChannel(), type, webhook, event, user, event.getMember());
                        });
            });
        }
    }

    private void openChannel(@NotNull GuildMessageChannel chn, ChannelData.Type type, Webhook webhook, @NotNull StringSelectInteraction event, @NotNull User user, @Nullable Member member) {
        switch (instance.getStorage().addChannel(new ChannelData(chn.getIdLong(), chn.getGuild().getIdLong(), type, webhook))) {
            case SUCCESS -> {
                EmbedBuilder eb = DiscordUtils.getOpenChannelEmbed(type, user, member);
                chn.sendMessageEmbeds(eb.build()).queue(message -> message.pin().queue());
                DiscordUtils.setSlowmode(instance, chn);

                reply(event, "Successfully opened a new counting channel.");
                Logs.info(DiscordUtils.getAsTag(user) + " has opened a new counting channel " + chn.getAsMention() + " (ID: `" + chn.getId() + "`)");
            }
            case NO_CHANGES -> reply(event, "This counting channel is already opened!");
            case FAILURE -> reply(event, "Failed to open this counting channel.");
        }
    }

    private void reply(@NotNull StringSelectInteraction event, @NotNull String msg) {
        event.getHook().sendMessage(msg).queue();
        event.editSelectMenu(event.getSelectMenu().asDisabled()).queue();
    }
}
