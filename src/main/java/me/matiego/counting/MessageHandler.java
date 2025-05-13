package me.matiego.counting;

import me.matiego.counting.utils.*;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MessageHandler extends ListenerAdapter {
    public MessageHandler(@NotNull Main instance) {
        this.instance = instance;
    }

    private final Main instance;
    private final HashMap<String, Pair<Integer, Long>> cooldown = Utils.createLimitedSizeMap(1000);

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        long time = Utils.now();
        Tasks.async(() -> {
            MessageChannelUnion channel = event.getChannel();
            if (!DiscordUtils.isSupportedChannel(channel)) return;

            ChannelData data = instance.getStorage().getChannel(channel.getIdLong());
            if (data == null) return;

            User user = event.getAuthor();
            Message message = event.getMessage();
            if (user.isBot()) {
                if (event.getJDA().getSelfUser().getId().equals(user.getId())) return;
                if (message.isWebhookMessage()) return;
                message.delete().queue();
                return;
            }

            int minTime = instance.getConfig().getInt("anti-spam.time", 0), maxCount = instance.getConfig().getInt("anti-spam.count", Integer.MAX_VALUE);
            if (checkCooldown(user, channel.getIdLong(), time, minTime * 1000, maxCount)) {
                message.delete().queue();
                DiscordUtils.sendPrivateMessage(user, "Nie spamuj na kanałach do liczenia! Możesz wysłać tylko %s wiadomości w odstępie mniejszym niż %s sekund między każdą.".formatted(maxCount, minTime));
                return;
            }

            ChannelHandler handler = data.getHandler();
            int amount = handler.getAmountOfMessages();
            if (amount == 0) {
                withRetrievedHistory(event, data, new ArrayList<>(), time);
            } else {
                channel.getHistory().retrievePast(amount + 1).queue(
                        h -> withRetrievedHistory(event, data, h, time),
                        f -> message.delete().queue()
                );
            }
        });
    }

    private void withRetrievedHistory(@NotNull MessageReceivedEvent event, @NotNull ChannelData data, @NotNull List<Message> history, long time) {
        if (!history.isEmpty()) {
            history.removeFirst();
        }

        Message message = event.getMessage();
        message.delete().queue(s -> {}, f -> {
            if (f instanceof ErrorResponseException e && e.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) return;
            Logs.error("Failed to delete a message!", f);
        });

        String correctMsg = data.getHandler().check(
                message,
                history.stream()
                        .filter(Message::isWebhookMessage)
                        .toList()
        );
        if (correctMsg == null) return;

        long guildId = event.getGuild().getIdLong();
        User user = event.getAuthor();
        Member member = event.getMember();
        MessageChannelUnion chn = event.getChannel();
        String userName = DiscordUtils.getName(user, member);

        UUID minecraftAccount = null;
        double reward = 0;
        if (instance.getApiRequests().isEnabled()) {
            minecraftAccount = instance.getMcAccounts().getMinecraftAccount(user);
            if (minecraftAccount != null) {
                reward = instance.getMcRewards().getReward(guildId, user.getIdLong(), chn.getIdLong(), data.getType().name().toLowerCase(), getLastMessageDate(history));
                if (reward > 0) {
                    userName = "[" + Utils.doubleToString(reward) + "$] " + userName;
                }
            }
        }

        boolean success = switch (chn.getType()) {
            case TEXT -> DiscordUtils.sendWebhook(data.getWebhookUrl(), DiscordUtils.getAvatar(user, member), userName, correctMsg);
            case GUILD_PUBLIC_THREAD -> DiscordUtils.sendWebhookToThread(chn.getIdLong(), data.getWebhookUrl(), DiscordUtils.getAvatar(user, member), userName, correctMsg);
            default -> false;
        };

        time = Utils.now() - time;
        if (time >= 1000) {
            Logs.warning("The message verification time exceeded 1 second! (Time: " + time + "ms; Channel: " + chn.getName() + "; ID: " + event.getChannel().getId() + ")");
        }

        if (success) {
            if (!instance.getUserRanking().add(user, guildId)) {
                DiscordUtils.sendPrivateMessage(user, "**Ups!** Napotkano niespodziewany błąd przy zwiększaniu twojego wyniku w rankingu. Poproś administratora bota o jego zwiększenie.");
            }
            if (reward > 0) {
                instance.getApiRequests().giveReward(minecraftAccount, reward)
                        .whenComplete((v, e) -> {
                            if (e != null) {
                                DiscordUtils.sendPrivateMessage(user, "**Ups!** Napotkano błąd przy dawaniu nagrody za liczenie: `" + e.getMessage() + "`");
                            }
                        });
            }
        } else {
            DiscordUtils.sendPrivateMessage(user, "**Ups!** Napotkano niespodziewany błąd przy wysyłaniu twojej wiadomości. Spróbuj później.");
        }

        Logs.debug("Handled message in the channel " + data.getType() + " (`" + data.getChannelId() + "`) sent by user " + userName + " (`" + user.getIdLong() + "`).");
    }

    private long getLastMessageDate(@NotNull List<Message> history) {
        if (history.isEmpty()) return Utils.now();
        // https://discord.com/developers/docs/reference#snowflakes-snowflake-id-format-structure-left-to-right
        return (history.getFirst().getIdLong() >> 22) + 1420070400000L;
    }

    public synchronized boolean checkCooldown(@NotNull UserSnowflake user, long chn, long now, int minTime, int maxCount) {
        String key = new Pair<>(user, chn).toString();
        Pair<Integer, Long> last = cooldown.getOrDefault(key, new Pair<>(0, now));
        if (now - last.getSecond() > minTime) {
            cooldown.put(key, new Pair<>(0, now));
            return false;
        }
        cooldown.put(key, new Pair<>(last.getFirst() + 1, now));
        return last.getFirst() + 1 > maxCount;
    }
}
