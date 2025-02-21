package me.matiego.counting.utils;

import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.List;

public interface ChannelHandler {
    default @Range(from = 0, to = 3) int getAmountOfMessages() {
        return 1;
    }
    @Nullable String check(@NotNull Message message, @NotNull List<Message> history);
}
