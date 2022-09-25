package me.matiego.counting;

import me.matiego.counting.handlers.*;
import me.matiego.counting.utils.IChannelHandler;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public enum ChannelType {
    COUNTING(Translation.TYPE__COUNTING__NAME.toString(), Translation.TYPE__COUNTING__DESCRIPTION.toString(), "U+1F9EE", new Counting()),
    BINARY_COUNTING(Translation.TYPE__BINARY_COUNTING__NAME.toString(), Translation.TYPE__BINARY_COUNTING__DESCRIPTION.toString(), "U+1F4BB", new BinaryCounting()),
    PRIME_COUNTING(Translation.TYPE__PRIME_COUNTING__NAME.toString(), Translation.TYPE__PRIME_COUNTING__DESCRIPTION.toString(), "U+1F4DF", new PrimeCounting()),
    SEMIPRIME_COUNTING(Translation.TYPE__SEMIPRIME_COUNTING__NAME.toString(), Translation.TYPE__SEMIPRIME_COUNTING__DESCRIPTION.toString(), "U+1F319", new SemiprimeCounting()),
    SPHENIC_COUNTING(Translation.TYPE__SPHENIC_COUNTING__NAME.toString(), Translation.TYPE__SPHENIC_COUNTING__DESCRIPTION.toString(), "U+26AA", new SphenicCounting()),
    FIBONACCI_SEQUENCE(Translation.TYPE__FIBONACCI_SEQUENCE__NAME.toString(), Translation.TYPE__FIBONACCI_SEQUENCE__DESCRIPTION.toString(), "U+1F69C", new FibonacciSequence()),
    LUCAS_NUMBERS(Translation.TYPE__LUCAS_NUMBERS__NAME.toString(), Translation.TYPE__LUCAS_NUMBERS__DESCRIPTION.toString(), "U+1F471", new LucasNumbers()),
    TRIANGULAR_NUMBERS(Translation.TYPE__TRIANGULAR_NUMBERS__NAME.toString(), Translation.TYPE__TRIANGULAR_NUMBERS__DESCRIPTION.toString(), "U+1F4D0", new TriangularNumbers()),
    PALINDROMIC_NUMBERS(Translation.TYPE__PALINDROMIC_NUMBERS__NAME.toString(), Translation.TYPE__PALINDROMIC_NUMBERS__DESCRIPTION.toString(), "U+1FA9E", new PalindromicNumbers()),
    ALPHABET(Translation.TYPE__ALPHABET__NAME.toString(), Translation.TYPE__ALPHABET__DESCRIPTION.toString(), "U+1F18E", new Alphabet()),
    POLISH_LAST_LETTER(Translation.TYPE__POLISH_LAST_LETTER__NAME.toString(), Translation.TYPE__POLISH_LAST_LETTER__DESCRIPTION.toString(), "U+1F524", new PolishLastLetter()),
    ENGLISH_LAST_LETTER(Translation.TYPE__ENGLISH_LAST_LETTER__NAME.toString(), Translation.TYPE__ENGLISH_LAST_LETTER__DESCRIPTION.toString(), "U+1F445", new EnglishLastLetter()),
    GERMAN_LAST_LETTER(Translation.TYPE__GERMAN_LAST_LETTER__NAME.toString(), Translation.TYPE__GERMAN_LAST_LETTER__DESCRIPTION.toString(), "U+1F7E5", new GermanLastLetter()),
    SPANISH_LAST_LETTER(Translation.TYPE__SPANISH_LAST_LETTER__NAME.toString(), Translation.TYPE__SPANISH_LAST_LETTER__DESCRIPTION.toString(), "U+1F7E8", new SpanishLastLetter());

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
