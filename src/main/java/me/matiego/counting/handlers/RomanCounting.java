package me.matiego.counting.handlers;

import me.matiego.counting.utils.ChannelHandler;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class RomanCounting implements ChannelHandler {

    public RomanCounting() {
        romanToInt.put("I", 1);
        romanToInt.put("V", 5);
        romanToInt.put("X", 10);
        romanToInt.put("L", 50);
        romanToInt.put("C", 100);
        romanToInt.put("D", 500);
        romanToInt.put("M", 1000);
    }
    private final HashMap<String, Integer> romanToInt = new HashMap<>();
    @SuppressWarnings("SpellCheckingInspection")
    private int romanToInt(@NotNull String roman) {
        roman = roman.replace("IV", "IIII");
        roman = roman.replace("IX", "VIIII");
        roman = roman.replace("XL", "XXXX");
        roman = roman.replace("XC", "LXXXX");
        roman = roman.replace("CD", "CCCC");
        roman = roman.replace("CM", "DCCCC");

        int result = 0;
        for (int i = 0; i < roman.length(); i++) {
            Integer integer = romanToInt.get(String.valueOf(roman.charAt(i)));
            if (integer == null) {
                return 0;
            }
            result += integer;
        }
        return result;
    }

    private final Pattern ROMAN_NUMBER = Pattern.compile("M{0,3}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})");

    /**
     * Checks if the sent message is correct.
     *
     * @param message the message sent by the user.
     * @param history the last messages from the channel - see {@link #getAmountOfMessages()}
     * @return {@code null} if the message is not correct, otherwise a new content of this message
     */
    @Override
    public @Nullable String check(@NotNull Message message, @NotNull List<Message> history) {
        String content = message.getContentDisplay().toUpperCase();
        if (!ROMAN_NUMBER.matcher(content).matches()) return null;
        if (history.isEmpty()) return content.equals("I") ? "I" : null;
        int a = romanToInt(history.get(0).getContentDisplay()), b = romanToInt(content);
        if (a <= 0 || b <= 0 || a >= 3999) return content.equals("I") ? "I" : null;
        return a + 1 == b ? content : null;
    }
}
