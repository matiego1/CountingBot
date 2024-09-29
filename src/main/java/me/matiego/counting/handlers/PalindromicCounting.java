package me.matiego.counting.handlers;

import me.matiego.counting.utils.ChannelHandler;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.List;

public class PalindromicCounting implements ChannelHandler {
    /**
     * Checks if sent a message is correct.
     *
     * @param message the message sent by the user.
     * @param history the last messages from the channel - see {@link #getAmountOfMessages()}
     * @return {@code null} if the message is not correct, otherwise a new content of this message
     */
    @Override
    public @Nullable String check(@NotNull Message message, @NotNull List<Message> history) {
        if (history.isEmpty()) return message.getContentDisplay().equals("1") ? "1" : null;

        BigInteger a, b;
        try {
            a = new BigInteger(history.getFirst().getContentDisplay());
        } catch (NumberFormatException e) {
            return message.getContentDisplay().equals("1") ? "1" : null;
        }
        try {
            b = new BigInteger(message.getContentDisplay());
        } catch (NumberFormatException ignored) {
            return null;
        }

        return getNext(a.toString()).equals(b.toString()) ? b.toString() : null;
    }

    /**
     * Returns the next palindrome number after the given one. <br>
     * Based on <a href="https://stackoverflow.com/questions/7934519/a-better-algorithm-to-find-the-next-palindrome-of-a-number-string">this</a> article.
     * @param number the number
     * @return the next palindrome number
     */
    public static @NotNull String getNext(@NotNull String number) {
        int len = number.length();
        String left = number.substring(0, len / 2);
        String middle = number.substring(len / 2, len - len / 2);
        String right = number.substring(len - len / 2);

        if (right.compareTo(reverse(left)) < 0) return left + middle + reverse(left);

        String next = new BigInteger(left + middle).add(BigInteger.ONE).toString();
        return next.substring(0, left.length() + middle.length()) + reverse(next).substring(middle.length());
    }

    /**
     * Reverses a string
     * @param string the string
     * @return the reversed string
     */
    private static @NotNull String reverse(@NotNull String string) {
        return new StringBuilder(string).reverse().toString();
    }
}
