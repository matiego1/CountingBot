package me.matiego.counting;

import me.matiego.counting.handlers.*;
import me.matiego.counting.utils.ChannelHandler;
import me.matiego.counting.utils.Logs;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ChannelData {
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

    public @NotNull ChannelHandler getHandler() {
        return type.getHandler();
    }

    public long getWebhookId() {
        return webhookId;
    }

    public @NotNull String getWebhookUrl() {
        return webhookUrl;
    }

    public enum Type {
        //normal
        COUNTING("U+1F9EE", new Counting()),
        BINARY_COUNTING("U+1F4BB", new BinaryCounting()),
        HEXADECIMAL_COUNTING("U+1F524", new HexadecimalCounting()),
        ROMAN_COUNTING("U+1F531", new RomanCounting()),
        //primes
        PRIME_COUNTING("U+1F4DF", new PrimeCounting()),
        SEMIPRIME_COUNTING("U+1F319", new SemiprimeCounting()),
        SPHENIC_COUNTING("U+26AA", new SphenicCounting()),
        //sequences
        FIBONACCI_SEQUENCE("U+1F69C", new FibonacciSequence()),
        LUCAS_SEQUENCE("U+1F471", new LucasSequence()),
        //others
        TRIANGULAR_COUNTING("U+1F4D0", new TriangularCounting()),
        PRONIC_COUNTING("U+1F4CF", new PronicCounting()),
        PALINDROMIC_COUNTING("U+1FA9E", new PalindromicCounting()),
        FACTORIAL_COUNTING("U+2757", new FactorialCounting()),
        ALPHABET("U+1F18E", new Alphabet()),
        TAUTOLOGIES("U+2696", new Tautologies()),
        MINECRAFT_ITEM("U+1F30D", new MinecraftItem()),
        //last letter
        POLISH_LAST_LETTER("U+1F524", new PolishLastLetter()),
        ENGLISH_LAST_LETTER("U+1F445", new EnglishLastLetter()),
        GERMAN_LAST_LETTER("U+1F7E5", new GermanLastLetter()),
        SPANISH_LAST_LETTER("U+1F7E8", new SpanishLastLetter());

        private final String name;
        private final String description;
        private final String emojiUnicode;
        private final ChannelHandler handler;
        Type(@NotNull String emojiUnicode, @NotNull ChannelHandler handler) {
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
        private @NotNull ChannelHandler getHandler() {
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

    public boolean block(@NotNull JDA jda) {
        try {
            TextChannel chn = jda.getTextChannelById(getChannelId());
            if (chn == null) return false;
            chn.upsertPermissionOverride(chn.getGuild().getSelfMember()).grant(Permission.MESSAGE_SEND).submit()
                    .thenCompose(v -> chn.upsertPermissionOverride(chn.getGuild().getPublicRole()).deny(Permission.MESSAGE_SEND).submit())
                    .get(10, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            Logs.error("An error occurred while blocking the counting channel (ID: `" + getChannelId() + "`)", e);
        }
        return false;
    }

    public boolean unblock(@NotNull JDA jda) {
        try {
            TextChannel chn = jda.getTextChannelById(getChannelId());
            if (chn == null) return false;
            try {
                chn.getManager().sync().submit().get(10, TimeUnit.SECONDS);
            } catch (IllegalStateException e) {
                chn.upsertPermissionOverride(chn.getGuild().getPublicRole()).clear(Permission.MESSAGE_SEND).submit().get(10, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            Logs.error("An error occurred while unblock the counting channel (ID: `" + getChannelId() + "`)", e);
        }
        return false;
    }
}
