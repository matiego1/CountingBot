package me.matiego.counting;

import me.matiego.counting.handlers.*;
import me.matiego.counting.utils.IChannelHandler;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class ChannelData {

    @SuppressWarnings("unused")
    private ChannelData() throws IllegalAccessException {
        throw new IllegalAccessException();
    }

    public ChannelData(long chn, long guild, @NotNull Type type, long webhookId, @NotNull String webhookUrl) {
        this.chn = chn;
        this.guild = guild;
        this.type = type;
        this.webhookId = webhookId;
        this.webhookUrl = webhookUrl;
    }

    public ChannelData(long chn, long guild, @NotNull Type type, @NotNull Webhook webhook) {
        this(chn, guild, type, webhook.getIdLong(), webhook.getUrl());
    }

    private final long chn;
    private final long guild;
    private final Type type;
    private final long webhookId;
    private final String webhookUrl;

    public long getChannelId() {
        return chn;
    }

    public long getGuildId() {
        return guild;
    }

    public @NotNull Type getType() {
        return type;
    }

    public @NotNull IChannelHandler getHandler() {
        return type.getHandler();
    }

    public long getWebhookId() {
        return webhookId;
    }

    public @NotNull String getWebhookUrl() {
        return webhookUrl;
    }

    public enum Type {
        COUNTING("U+1F9EE", new Counting()),
        BINARY_COUNTING("U+1F4BB", new BinaryCounting()),
        HEXADECIMAL_COUNTING("U+1F524", new HexadecimalCounting()),
        PRIME_COUNTING("U+1F4DF", new PrimeCounting()),
        SEMIPRIME_COUNTING("U+1F319", new SemiprimeCounting()),
        SPHENIC_COUNTING("U+26AA", new SphenicCounting()),
        FIBONACCI_SEQUENCE("U+1F69C", new FibonacciSequence()),
        LUCAS_NUMBERS("U+1F471", new LucasNumbers()),
        TRIANGULAR_NUMBERS("U+1F4D0", new TriangularNumbers()),
        PALINDROMIC_NUMBERS("U+1FA9E", new PalindromicNumbers()),
        ROMAN_COUNTING("U+1F531", new RomanCounting()),
        ALPHABET("U+1F18E", new Alphabet()),
        POLISH_LAST_LETTER("U+1F524", new PolishLastLetter()),
        ENGLISH_LAST_LETTER("U+1F445", new EnglishLastLetter()),
        GERMAN_LAST_LETTER("U+1F7E5", new GermanLastLetter()),
        SPANISH_LAST_LETTER("U+1F7E8", new SpanishLastLetter());

        private final String name;
        private final String description;
        private final String emojiUnicode;
        private final IChannelHandler handler;
        Type(@NotNull String emojiUnicode, @NotNull IChannelHandler handler) {
            String description, name;
            try {
                name = Translation.valueOf("TYPE__" + name() + "__NAME").toString();
                description = Translation.valueOf("TYPE__" + name() + "__DESCRIPTION").toString();
            } catch (IllegalArgumentException e) {
                name = "TYPE__" + name() + "__NAME";
                description = "TYPE__" + name() + "__DESCRIPTION";
            }
            this.description = description;
            this.name = name;
            this.emojiUnicode = emojiUnicode;
            this.handler = handler;
        }

        @Override
        public @NotNull String toString() {
            return name;
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
        private @NotNull IChannelHandler getHandler() {
            return handler;
        }
    }

    /**
     * Returns a list of the channel types parsed as {@link SelectOption}.
     * @return the list of the channel types
     */
    public static @NotNull List<SelectOption> getSelectMenuOptions() {
        return Arrays.stream(Type.values())
                .map(value -> SelectOption.of(value.toString(), value.toString())
                        .withDescription(value.description)
                        .withEmoji(Emoji.fromUnicode(value.emojiUnicode)))
                .toList();
    }

}
