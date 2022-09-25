package me.matiego.counting;

import me.matiego.counting.utils.IChannelHandler;
import me.matiego.counting.utils.Logs;
import me.matiego.counting.utils.Pair;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A Discord messages handler
 */
public class MessageHandler extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        long time = System.currentTimeMillis();
        Utils.async(() -> {
            User user = event.getAuthor();
            if (user.isBot()) return;
            Member member = event.getMember();

            Pair<ChannelType, String> pair = Main.getInstance().getStorage().getChannel(event.getChannel().getIdLong());
            if (pair == null) return;
            IChannelHandler handler = pair.getFirst().getHandler();

            int amount = handler.getAmountOfMessages();
            List<Message> history = amount == 0 ? new ArrayList<>() : event.getChannel().getHistory().retrievePast(amount + 1).complete();
            try {
                history.remove(0);
            } catch (IndexOutOfBoundsException ignored) {}
            history = history.stream().filter(Message::isWebhookMessage).toList();

            Message message = event.getMessage();
            message.delete().queue();

            String correctMsg = handler.check(message, history);
            if (correctMsg == null) return;

            if (!Utils.sendWebhook(pair.getSecond(), Utils.getAvatar(user, member), Utils.getName(user, member), correctMsg)) {
                Utils.sendPrivateMessage(user, Translation.GENERAL__NOT_SENT.toString());
            }
            if (System.currentTimeMillis() - time >= 1000) {
                Logs.warning("The message verification time exceeded 1s. Channel: " + event.getChannel().getName() + "; ID: " + event.getChannel().getId());
            }
        });
    }}
