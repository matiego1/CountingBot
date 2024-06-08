package me.matiego.counting.handlers;

import me.matiego.counting.utils.ChannelHandler;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PronicCounting implements ChannelHandler {
    @Override
    public @Nullable String check(@NotNull Message message, @NotNull List<Message> history) {
        if (history.isEmpty()) return message.getContentDisplay().equals("2") ? "2" : null;

        long a, b;
        try {
            a = Long.parseLong(history.get(0).getContentDisplay());
        } catch (NumberFormatException e) {
            return message.getContentDisplay().equals("2") ? "2" : null;
        }
        try {
            b = Long.parseLong(message.getContentDisplay());
        } catch (NumberFormatException ignored) {
            return null;
        }

        return getNext(a) == b ? String.valueOf(b) : null;
    }

    private long getNext(long a) {
        long n = (int) ((-1 + Math.sqrt(4 * a + 1)) / 2) + 1;
        return n * (n + 1);
    }
}
