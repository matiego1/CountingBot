package me.matiego.counting;

import me.matiego.counting.utils.*;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A Discord messages handler
 */
public class MessageHandler extends ListenerAdapter {
    public MessageHandler(@NotNull Main plugin) {
        this.plugin = plugin;
    }

    private final Main plugin;
    private final HashMap<String, Pair<Integer, Long>> cooldown = Utils.createLimitedSizeMap(1000);

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Utils.async(() -> {
            long time = Utils.now();

            if (!event.isFromType(ChannelType.TEXT) && !event.isFromType(ChannelType.GUILD_PUBLIC_THREAD)) return;

            MessageChannelUnion chn = event.getChannel();
            ChannelData data = plugin.getStorage().getChannel(chn.getIdLong());
            if (data == null) return;

            User user = event.getAuthor();
            Message message = event.getMessage();
            if (user.isBot()) {
                if (event.getJDA().getSelfUser().getId().equals(user.getId())) return;
                if (message.isWebhookMessage()) return;
                message.delete().queue();
                return;
            }

            int minTime = plugin.getConfig().getInt("anti-spam.time"), maxCount = plugin.getConfig().getInt("anti-spam.count");
            if (checkCooldown(user, chn.getIdLong(), time, minTime * 1000, maxCount)) {
                message.delete().queue();
                DiscordUtils.sendPrivateMessage(user, Translation.GENERAL__DO_NOT_SPAM.getFormatted(maxCount, minTime));
                return;
            }

            ChannelHandler handler = data.getHandler();
            int amount = handler.getAmountOfMessages();
            List<Message> history = amount == 0
                    ? new ArrayList<>()
                    : chn.getHistory().retrievePast(amount + 1).complete();
            if (!history.isEmpty()) {
                history.removeFirst();
            }

            message.delete().queue();

            String correctMsg = handler.check(
                    message,
                    history.stream()
                            .filter(Message::isWebhookMessage)
                            .toList()
            );
            if (correctMsg == null) return;

            Member member = event.getMember();
            CountingMessageSendEvent countingMessageSendEvent = new CountingMessageSendEvent(data, user, DiscordUtils.getName(user, member), history.isEmpty() ? null : history.getFirst().getIdLong());
            Bukkit.getPluginManager().callEvent(countingMessageSendEvent);
            if (countingMessageSendEvent.isCancelled()) return;

            boolean success = switch (chn.getType()) {
                case TEXT -> DiscordUtils.sendWebhook(data.getWebhookUrl(), DiscordUtils.getAvatar(user, member), countingMessageSendEvent.getDisplayName(), correctMsg);
                case GUILD_PUBLIC_THREAD -> DiscordUtils.sendWebhookToThread(chn.getIdLong(), data.getWebhookUrl(), DiscordUtils.getAvatar(user, member), countingMessageSendEvent.getDisplayName(), correctMsg);
                default -> false;
            };

            if (success) {
                plugin.getUserRanking().add(user, event.getGuild().getIdLong());
                countingMessageSendEvent.getOnSuccess().run();
            } else {
                DiscordUtils.sendPrivateMessage(user, Translation.GENERAL__NOT_SENT.toString());
            }

            time = Utils.now() - time;
            if (time >= 1000) {
                Logs.warning("The message verification time exceeded 1 second! (Time: " + time + "ms; Channel: " + chn.getName() + "; ID: " + event.getChannel().getId() + ")");
            }
        });
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
