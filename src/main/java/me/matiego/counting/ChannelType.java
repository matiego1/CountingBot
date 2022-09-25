package me.matiego.counting;

import me.matiego.counting.handlers.*;
import me.matiego.counting.utils.IChannelHandler;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public enum ChannelType {
    COUNTING("Counting", "Write the next numbers", "U+1F9EE", new Counting()),
    BINARY_COUNTING("Binary Counting", "Write the next numbers in binary", "U+1F4BB", new BinaryCounting()),
    PRIME_COUNTING("Prime Counting", "Write the next prime numbers", "U+1F4DF", new PrimeCounting()),
    SEMIPRIME_COUNTING("Semiprime counting", "Write the next semiprime numbers", "U+1F319", new SemiprimeCounting()),
    SPHENIC_COUNTING("Sphenic counting", "Write the next sphenic numbers", "U+26AA", new SphenicCounting()),
    FIBONACCI_SEQUENCE("Fibonacci sequence", "Write the next numbers of the fibonacci sequence", "U+1F69C", new FibonacciSequence()),
    LUCAS_NUMBERS("Lucas numbers", "Write the next numbers of the lucas sequence", "U+1F471", new LucasNumbers()),
    TRIANGULAR_NUMBERS("Triangular numbers", "Write the next triangular numbers", "U+1F4D0", new TriangularNumbers()),
    PALINDROMIC_NUMBERS("Palindromic numbers", "Write the next palindromic numbers", "U+1FA9E", new PalindromicNumbers()),
    ALPHABET("Alphabet", "Write the next letters in english alphabet", "U+1F18E", new Alphabet()),
    POLISH_LAST_LETTER("Polish last letter", "Write a word that starts with the last letter of the previous one (Polish)", "U+1F524", new PolishLastLetter()),
    ENGLISH_LAST_LETTER("English last letter", "Write a word that starts with the last letter of the previous one (English)", "U+1F445", new EnglishLastLetter()),
    GERMAN_LAST_LETTER("German last letter", "Write a word that starts with the last letter of the previous one (German)", "U+1F7E5", new GermanLastLetter()),
    SPANISH_LAST_LETTER("Spanish last letter", "Write a word that starts with the last letter of the previous one (Spanish)", "U+1F7E8", new SpanishLastLetter());

    private final String name;
    private final String description;
    private final String emojiUnicode;
    private final IChannelHandler handler;
    ChannelType(@NotNull String name, @NotNull String description, @NotNull String emojiUnicode, @NotNull IChannelHandler handler) {
        this.name = name;
        this.description = description;
        this.emojiUnicode = emojiUnicode;
        this.handler = handler;
    }

    @Override
    public @NotNull String toString() {
        return name;
    }

    /**
     * Returns a list of the channel types parsed as {@link SelectOption}.
     * @return the list of the channel types
     */
    public static @NotNull List<SelectOption> getSelectMenuOptions() {
        return Arrays.stream(ChannelType.values())
                .map(value -> SelectOption.of(value.name, value.toString())
                        .withDescription(value.description)
                        .withEmoji(Emoji.fromUnicode(value.emojiUnicode)))
                .toList();
    }

    /**
     * Returns the channel type description.
     * @return the description
     */
    public @NotNull String getDescription() {
        return description;
    }

    /**
     * Returns the channel type handler.
     * @return the handler
     */
    public @NotNull IChannelHandler getHandler() {
        return handler;
    }
}
