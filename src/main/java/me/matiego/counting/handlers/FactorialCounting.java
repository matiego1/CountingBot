package me.matiego.counting.handlers;

import me.matiego.counting.utils.ChannelHandler;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.math.BigInteger;
import java.util.List;

public class FactorialCounting implements ChannelHandler {
    @Override
    public @Range(from = 0, to = 3) int getAmountOfMessages() {
        return 2;
    }

    @Override
    public @Nullable String check(@NotNull Message message, @NotNull List<Message> history) {
        if (history.isEmpty()) return message.getContentDisplay().equals("1") ? "1" : null;
        if (history.size() == 1) return message.getContentDisplay().equals("1") ? "1" : null;

        BigInteger a, b, c;
        try {
            a = new BigInteger(history.get(1).getContentDisplay());
            b = new BigInteger(history.getFirst().getContentDisplay());
        } catch (NumberFormatException e) {
            return message.getContentDisplay().equals("1") ? "1" : null;
        }
        try {
            c = new BigInteger(message.getContentDisplay());
        } catch (NumberFormatException e) {
            return null;
        }

        return c.equals(b.multiply(b.divide(a).add(BigInteger.ONE))) ? c.toString() : null;
    }
}