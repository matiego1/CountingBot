package me.matiego.counting.handlers;

import me.matiego.counting.utils.ChannelHandler;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class FactorialCounting implements ChannelHandler {
    private final static int MAX_FACTORIAL = 800;
    private final static List<BigInteger> factorials = new ArrayList<>();


    static {
        BigInteger current = BigInteger.ONE;
        for (int i = 1; i <= MAX_FACTORIAL; i++) {
            current = current.multiply(BigInteger.valueOf(i));
            factorials.add(current);
        }
    }

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

        BigInteger a;
        try {
            a = new BigInteger(history.get(0).getContentDisplay());
        } catch (NumberFormatException e) {
            return message.getContentDisplay().equals("1") ? "1" : null;
        }

        BigInteger b = factorials.stream()
                .filter(factorial -> factorial.compareTo(a) < 0)
                .findFirst()
                .orElse(BigInteger.ONE);

        return a.equals(b) ? b.toString() : null;
    }
}
