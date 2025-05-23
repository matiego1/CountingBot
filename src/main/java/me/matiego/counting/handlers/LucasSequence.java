package me.matiego.counting.handlers;

import me.matiego.counting.utils.ChannelHandler;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.math.BigInteger;
import java.util.List;

public class LucasSequence implements ChannelHandler {
    @Override
    public @Range(from = 0, to = 3) int getAmountOfMessages() {
        return 2;
    }

    @Override
    public @Nullable String check(@NotNull Message message, @NotNull List<Message> history) {
        if (history.isEmpty()) return message.getContentDisplay().equals("2") ? "2" : null;
        if (history.size() == 1) return message.getContentDisplay().equals("1") ? "1" : null;

        BigInteger a, b, c;
        try {
            b = new BigInteger(history.get(1).getContentDisplay());
        } catch (NumberFormatException ignored) {
            return message.getContentDisplay().equals("2") ? "2" : null;
        }
        try {
            a = new BigInteger(history.getFirst().getContentDisplay());
        } catch (NumberFormatException ignored) {
            return message.getContentDisplay().equals("1") ? "1" : null;
        }
        try {
            c = new BigInteger(message.getContentDisplay());
        } catch (NumberFormatException ignored) {
            return null;
        }

        return a.add(b).equals(c) ? c.toString() : null;
    }
}
