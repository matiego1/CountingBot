package me.matiego.counting;

import me.matiego.counting.utils.*;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A Discord messages handler
 */
public class MessageHandler extends ListenerAdapter {

    private final FixedSizeMap<Pair<UserSnowflake, Long>, Pair<Integer, Long>> map = new FixedSizeMap<>(1000);

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        long time = System.currentTimeMillis();
        Utils.async(() -> {
            User user = event.getAuthor();
            if (user.isBot()) return;

            Message message = event.getMessage();
            int minTime = Main.getInstance().getConfig().getInt("anti-spam.time"), maxCount = Main.getInstance().getConfig().getInt("anti-spam.count");
            if (!check(user, event.getChannel().getIdLong(), time, minTime, maxCount)) {
                message.delete().queue();
                Utils.sendPrivateMessage(user, Translation.GENERAL__DO_NOT_SPAM.getFormatted(minTime, maxCount));
                return;
            }

            Pair<ChannelType, String> pair = Main.getInstance().getStorage().getChannel(event.getChannel().getIdLong());
            if (pair == null) return;
            IChannelHandler handler = pair.getFirst().getHandler();

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
            if (!Utils.sendWebhook(pair.getSecond(), Utils.getAvatar(user, member), Utils.getName(user, member), correctMsg)) {
                Utils.sendPrivateMessage(user, Translation.GENERAL__NOT_SENT.toString());
            }
            if (System.currentTimeMillis() - time >= 500) {
                Logs.warning("The message verification time exceeded 500ms. Channel: " + event.getChannel().getName() + "; ID: " + event.getChannel().getId());
            }
        });
    }

    public synchronized boolean check(@NotNull UserSnowflake user, long chn, long now, int minTime, int maxCount) {
        Pair<UserSnowflake, Long> key = new Pair<>(user, chn);
        Pair<Integer, Long> last = map.getOrDefault(key, new Pair<>(0, now));
        if (now - last.getSecond() > minTime) map.remove(key);
        map.put(key, new Pair<>(last.getFirst() + 1, now));
        return last.getFirst() + 1 <= maxCount;
    }
}
