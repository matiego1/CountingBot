package me.matiego.counting.handlers;

import me.matiego.counting.utils.ChannelHandler;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Alphabet implements ChannelHandler {
    /**
     * Checks if the sent message is correct.
     *
     * @param message the message sent by the user.
     * @param history the last messages from the channel - see {@link #getAmountOfMessages()}
     * @return {@code null} if the message is not correct, otherwise a new content of this message
     */
    @Override
    public @Nullable String check(@NotNull Message message, @NotNull List<Message> history) {
        String b = message.getContentDisplay().toUpperCase();
        if (b.length() != 1) return null;
        if (history.isEmpty()) return b.equals("A") ? b : null;
        String a = history.get(0).getContentDisplay().toUpperCase();
        if (a.length() != 1) return null;
        int aChar = a.charAt(0);
        if (aChar < (int) 'A' || aChar > (int) 'Z') return null;
        if (a.equals("Z")) return b.equals("A") ? b : null;
        return (aChar + 1 == (int) b.charAt(0)) ? b : null;
    }
}
