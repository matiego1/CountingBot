package me.matiego.counting.handlers;

import me.matiego.counting.utils.ChannelHandler;
import me.matiego.counting.Primes;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SphenicCounting implements ChannelHandler {
    @Override
    public @Nullable String check(@NotNull Message message, @NotNull List<Message> history) {
        if (history.isEmpty()) return message.getContentDisplay().equals("30") ? "30" : null;

        long a, b;
        try {
            a = Integer.parseInt(history.getFirst().getContentDisplay()) + 1;
        } catch (NumberFormatException e) {
            return message.getContentDisplay().equals("30") ? "30" : null;
        }
        try {
            b = Integer.parseInt(message.getContentDisplay());
        } catch (NumberFormatException ignored) {
            return null;
        }

        while (!Primes.isSphenic(a)) a++;
        if (a > Integer.MAX_VALUE) a = 30;

        return a == b ? String.valueOf(b) : null;
    }
}
