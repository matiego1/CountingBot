package me.matiego.counting;

import me.matiego.counting.utils.*;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A Discord messages handler
 */
public class MessageHandler extends ListenerAdapter {
    public MessageHandler(@NotNull Main plugin) {
        this.plugin = plugin;
    }

    private final Main plugin;
    private final FixedSizeMap<String, Pair<Integer, Long>> map = new FixedSizeMap<>(1000);

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Utils.async(() -> {
            long time = Utils.now();

            if (!event.isFromType(ChannelType.TEXT)) return;

            ChannelData data = plugin.getStorage().getChannel(event.getChannel().getIdLong());
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
            if (!check(user, event.getChannel().getIdLong(), time, minTime * 1000, maxCount)) {
                message.delete().queue();
                Utils.sendPrivateMessage(user, Translation.GENERAL__DO_NOT_SPAM.getFormatted(maxCount, minTime));
                return;
            }

            ChannelHandler handler = data.getHandler();
            int amount = handler.getAmountOfMessages();
            List<Message> history = amount == 0 ? new ArrayList<>() : event.getChannel().getHistory().retrievePast(amount + 1).complete();
            try {
                history.remove(0);
            } catch (IndexOutOfBoundsException ignored) {}
            history = history.stream().filter(Message::isWebhookMessage).toList();

            message.delete().queue();

            String correctMsg = handler.check(message, history);
            if (correctMsg == null) return;

            Member member = event.getMember();
            CountingMessageSendEvent countingMessageSendEvent = new CountingMessageSendEvent(data, user, Utils.getName(user, member), history.isEmpty() ? null : history.get(0).getIdLong());
            Bukkit.getPluginManager().callEvent(countingMessageSendEvent);
            if (countingMessageSendEvent.isCancelled()) return;

            if (Utils.sendWebhook(data.getWebhookUrl(), Utils.getAvatar(user, member), countingMessageSendEvent.getDisplayName(), correctMsg)) {
                countingMessageSendEvent.getOnSuccess().run();
                plugin.getUserRanking().add(user, event.getGuild().getIdLong());
            } else {
                Utils.sendPrivateMessage(user, Translation.GENERAL__NOT_SENT.toString());
            }

            time = Utils.now() - time;
            if (time >= 1000) {
                Logs.warning("The message verification time exceeded 1 second! (Time: " + time + "ms; Channel: " + event.getChannel().getName() + "; ID: " + event.getChannel().getId() + ")");
            }
        });
    }

    public synchronized boolean check(@NotNull UserSnowflake user, long chn, long now, int minTime, int maxCount) {
        String key = new Pair<>(user, chn).toString();
        Pair<Integer, Long> last = map.getOrDefault(key, new Pair<>(0, now));
        if (now - last.getSecond() > minTime) {
            map.put(key, new Pair<>(0, now));
            return true;
        }
        map.put(key, new Pair<>(last.getFirst() + 1, now));
        return last.getFirst() + 1 <= maxCount;
    }
}
