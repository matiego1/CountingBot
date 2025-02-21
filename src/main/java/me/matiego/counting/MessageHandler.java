package me.matiego.counting;

import me.matiego.counting.utils.*;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MessageHandler extends ListenerAdapter {
    public MessageHandler(@NotNull Main instance) {
        this.instance = instance;
    }

    private final Main instance;
    private final HashMap<String, Pair<Integer, Long>> cooldown = Utils.createLimitedSizeMap(1000);

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Utils.async(() -> {
            long time = Utils.now();

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
        message.delete().queue();

        String correctMsg = data.getHandler().check(
                message,
                history.stream()
                        .filter(Message::isWebhookMessage)
                        .toList()
        );
        if (correctMsg == null) return;

        User user = event.getAuthor();
        Member member = event.getMember();
        CountingMessageSendEvent countingMessageSendEvent = new CountingMessageSendEvent(data, user, DiscordUtils.getName(user, member), history.isEmpty() ? null : history.getFirst().getIdLong());
        Bukkit.getPluginManager().callEvent(countingMessageSendEvent);
        if (countingMessageSendEvent.isCancelled()) return;

        MessageChannelUnion chn = event.getChannel();
        boolean success = switch (chn.getType()) {
            case TEXT -> DiscordUtils.sendWebhook(data.getWebhookUrl(), DiscordUtils.getAvatar(user, member), countingMessageSendEvent.getDisplayName(), correctMsg);
            case GUILD_PUBLIC_THREAD -> DiscordUtils.sendWebhookToThread(chn.getIdLong(), data.getWebhookUrl(), DiscordUtils.getAvatar(user, member), countingMessageSendEvent.getDisplayName(), correctMsg);
            default -> false;
        };

        if (success) {
            if (!instance.getUserRanking().add(user, event.getGuild().getIdLong())) {
                DiscordUtils.sendPrivateMessage(user, "**Ups!** Napotkano niespodziewany błąd przy zwiększaniu twojego wyniku w rankingu. Poproś administratora bota o jego zwiększenie.");
            }
            countingMessageSendEvent.getOnSuccess().run();
        } else {
            DiscordUtils.sendPrivateMessage(user, "**Ups!** Napotkano niespodziewany błąd przy wysyłaniu twojej wiadomości. Spróbuj później.");
        }

        time = Utils.now() - time;
        if (time >= 1000) {
            Logs.warning("The message verification time exceeded 1 second! (Time: " + time + "ms; Channel: " + chn.getName() + "; ID: " + event.getChannel().getId() + ")");
        }
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
