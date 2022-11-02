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

    public RomanCounting() {
        romanToInt.put("I", 1);
        romanToInt.put("V", 5);
        romanToInt.put("X", 10);
        romanToInt.put("L", 50);
        romanToInt.put("C", 100);
        romanToInt.put("D", 500);
        romanToInt.put("M", 1000);
    }
    HashMap<String, Integer> romanToInt = new HashMap<>();
    @SuppressWarnings("SpellCheckingInspection")
    List<Pair<String, String>> toReplace = Arrays.asList(
            new Pair<>("IV", "IIII"),
            new Pair<>("IX", "VIIII"),
            new Pair<>("XL", "XXXX"),
            new Pair<>("XC", "LXXXX"),
            new Pair<>("CD", "CCCC"),
            new Pair<>("CM", "DCCCC")
    );
    private int romanToInt(@NotNull String roman) {
        for (Pair<String, String> pair : toReplace) {
            roman = roman.replace(pair.getFirst(), pair.getSecond());
        }
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

    List<Pair<Integer, String>> intToRoman = Arrays.asList(
            new Pair<>(1000, "M"),
            new Pair<>(900, "CM"),
            new Pair<>(500, "D"),
            new Pair<>(400, "CD"),
            new Pair<>(100, "C"),
            new Pair<>(90, "XC"),
            new Pair<>(50, "L"),
            new Pair<>(40, "XL"),
            new Pair<>(10, "X"),
            new Pair<>(9, "IX"),
            new Pair<>(5, "V"),
            new Pair<>(4, "IV"),
            new Pair<>(1, "I")
    );
    private @NotNull String intToRoman(@Range(from = 1, to = 3999) int integer) {
        StringBuilder roman = new StringBuilder();
        while (integer > 0) {
            for (Pair<Integer, String> pair : intToRoman) {
                if (integer >= pair.getFirst()) {
                    integer -= pair.getFirst();
                    roman.append(pair.getSecond());
                }
            }
        }
        return roman.toString();
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
