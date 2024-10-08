package me.matiego.counting.utils;

import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.List;

public interface ChannelHandler {

    /**
     * Returns the number of messages retrieved from the channel history.
     * @return the number of messages.
     */
    default @Range(from = 0, to = 3) int getAmountOfMessages() {
        return 1;
    }

    /**
     * Checks if sent a message is correct.
     * @param message the message sent by the user.
     * @param history the last messages from the channel - see {@link #getAmountOfMessages()}
     * @return {@code null} if the message is not correct, otherwise a new content of this message
     */
    @Nullable String check(@NotNull Message message, @NotNull List<Message> history);

}
