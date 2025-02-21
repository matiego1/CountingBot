package me.matiego.counting.handlers;

import me.matiego.counting.utils.ChannelHandler;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HexadecimalCounting implements ChannelHandler {
    @Override
    public @Nullable String check(@NotNull Message message, @NotNull List<Message> history) {
        if (history.isEmpty()) return message.getContentDisplay().equals("1") ? "1" : null;

        long a, b;
        try {
            a = Long.parseLong(history.getFirst().getContentDisplay(), 16);
        } catch (NumberFormatException e) {
            return message.getContentDisplay().equals("1") ? "1" : null;
        }
        try {
            b = Long.parseLong(message.getContentDisplay(), 16);
        } catch (NumberFormatException ignored) {
            return null;
        }

        return a + 1 == b ? Long.toHexString(b).toUpperCase() : null;
    }
}
