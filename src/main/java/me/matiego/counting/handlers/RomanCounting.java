package me.matiego.counting.handlers;

import me.matiego.counting.utils.IChannelHandler;
import me.matiego.counting.utils.Pair;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class RomanCounting implements IChannelHandler {

    //Roman number to integer
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
                return -1;
            }
            result += integer;
        }
        return result;
    }

    //Integer to roman number
    private final List<Pair<Integer, String>> intToRoman = Arrays.asList(
            new Pair<>(1000, "M"),
            new Pair<>(500, "D"),
            new Pair<>(100, "C"),
            new Pair<>(50, "L"),
            new Pair<>(10, "X"),
            new Pair<>(5, "V"),
            new Pair<>(1, "I")
    );
    @SuppressWarnings("SpellCheckingInspection")
    private @NotNull String intToRoman(@Range(from = 1, to = 3999) int integer) {
        StringBuilder roman = new StringBuilder();
        while (integer > 0) {
            for (Pair<Integer, String> pair : intToRoman) {
                if (integer >= pair.getFirst()) {
                    integer -= pair.getFirst();
                    roman.append(pair.getSecond());
                    break;
                }
            }
        }
        String result = roman.toString();
        result = result.replace("VIIII", "IX");
        result = result.replace("IIII", "IV");
        result = result.replace("LXXXX", "XC");
        result = result.replace("XXXX", "XL");
        result = result.replace("DCCCC", "CM");
        result = result.replace("CCCC", "CD");
        return result;
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
        String content = message.getContentDisplay();
        if (history.isEmpty()) return content.equalsIgnoreCase("I") ? "I" : null;
        if (romanToInt(history.get(0).getContentDisplay()) == -1) return content.equalsIgnoreCase("I") ? "I" : null;
        int number = romanToInt(content);
        if (number == -1) return null;
        if (number >= 3999) return content.equalsIgnoreCase("I") ? "I" : null;
        String roman = intToRoman(number + 1);
        return content.equalsIgnoreCase(roman) ? roman : null;
    }
}
