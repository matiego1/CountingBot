package me.matiego.counting.handlers;

import me.matiego.counting.utils.ChannelHandler;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Alphabet implements ChannelHandler {
    @Override
    public @Nullable String check(@NotNull Message message, @NotNull List<Message> history) {
        String b = message.getContentDisplay().toUpperCase();

        if (history.isEmpty()) return b.equals("A") ? b : null;
        String a = history.getFirst().getContentDisplay().toUpperCase();

        String next = getNext(a);
        return b.equalsIgnoreCase(next) ? next : null;
    }

    private @NotNull String getNext(@NotNull String message) {
        char[] chars = message.toCharArray();
        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i] == 'Z') {
                chars[i] = 'A';
            } else {
                chars[i]++;
                return new String(chars);
            }
        }

        return "A" + new String(chars);
    }
}
