package me.matiego.counting.handlers;

import me.matiego.counting.utils.ChannelHandler;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BinaryCounting implements ChannelHandler {
    @Override
    public @Nullable String check(@NotNull Message message, @NotNull List<Message> history) {
        if (history.isEmpty()) return message.getContentDisplay().equals("1") ? "1" : null;

        long a, b;
        try {
            a = Long.parseLong(history.getFirst().getContentDisplay(), 2);
        } catch (NumberFormatException e) {
            return message.getContentDisplay().equals("1") ? "1" : null;
        }
        try {
            b = Long.parseLong(message.getContentDisplay(), 2);
        } catch (NumberFormatException ignored) {
            return null;
        }

        return a + 1 == b ? Long.toBinaryString(b) : null;
    }
}
