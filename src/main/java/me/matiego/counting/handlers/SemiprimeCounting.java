package me.matiego.counting.handlers;

import me.matiego.counting.utils.ChannelHandler;
import me.matiego.counting.Primes;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SemiprimeCounting implements ChannelHandler {
    /**
     * Checks if sent message is correct.
     *
     * @param message the message sent by the user.
     * @param history the last messages from the channel - see {@link #getAmountOfMessages()}
     * @return {@code null} if the message is not correct, otherwise a new content of this message
     */
    @Override
    public @Nullable String check(@NotNull Message message, @NotNull List<Message> history) {
        if (history.isEmpty()) return message.getContentDisplay().equals("4") ? "4" : null;

        long a, b;
        try {
            a = Integer.parseInt(history.get(0).getContentDisplay()) + 1;
        } catch (NumberFormatException e) {
            return message.getContentDisplay().equals("4") ? "4" : null;
        }
        try {
            b = Integer.parseInt(message.getContentDisplay());
        } catch (NumberFormatException ignored) {
            return null;
        }

        while (!Primes.isSemiPrime(a)) a++;
        if (a > Integer.MAX_VALUE) a = 4;

        return a == b ? String.valueOf(b) : null;
    }
}
