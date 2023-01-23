package me.matiego.counting.handlers;

import me.matiego.counting.utils.ChannelHandler;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HexadecimalCounting implements ChannelHandler {
    /**
     * Checks if the sent message is correct.
     *
     * @param message the message sent by the user.
     * @param history the last messages from the channel - see {@link #getAmountOfMessages()}
     * @return {@code null} if the message is not correct, otherwise a new content of this message
     */
    @Override
    public @Nullable String check(@NotNull Message message, @NotNull List<Message> history) {
        if (history.isEmpty()) return message.getContentDisplay().equals("1") ? "1" : null;
        int a, b = -1;
        try {
            a = Integer.parseInt(history.get(0).getContentDisplay(), 16);
        } catch (NumberFormatException e) {
            return message.getContentDisplay().equals("1") ? "1" : null;
        }
        try {
            b = Integer.parseInt(message.getContentDisplay(), 16);
        } catch (NumberFormatException ignored) {}
        return a + 1 == b ? Integer.toHexString(b).toUpperCase() : null;
    }
}
