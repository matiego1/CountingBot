package me.matiego.counting;

import lombok.Getter;
import me.matiego.counting.handlers.*;
import me.matiego.counting.utils.ChannelHandler;
import me.matiego.counting.utils.DiscordUtils;
import me.matiego.counting.utils.Logs;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Getter
public class ChannelData {
    public ChannelData(long channelId, long guildId, @NotNull Type type, long webhookId, @NotNull String webhookUrl) {
        this.channelId = channelId;
        this.guildId = guildId;
        this.type = type;
        this.webhookId = webhookId;
        this.webhookUrl = webhookUrl;
    }

    public ChannelData(long channelId, long guildId, @NotNull Type type, @NotNull Webhook webhook) {
        this(channelId, guildId, type, webhook.getIdLong(), webhook.getUrl());
    }

    private final long channelId;
    private final long guildId;
    private final Type type;
    private final long webhookId;
    private final String webhookUrl;

    public @NotNull ChannelHandler getHandler() {
        return type.getHandler();
    }

    public enum Type {
        //normal
        COUNTING("Liczenie", "Kolejne liczby naturalne", "U+1F9EE", new Counting()),
        BINARY_COUNTING("Liczenie binarne", "Kolejne liczby naturalne w systemie binarnym", "U+1F4BB", new BinaryCounting()),
        HEXADECIMAL_COUNTING("Liczenie szesnastkowe", "Kolejne liczby naturalne w systemie szesnastkowym", "U+1F524", new HexadecimalCounting()),
        ROMAN_COUNTING("Liczby rzymskie", "Kolejne liczb rzymskie", "U+1F531", new RomanCounting()),
        //primes
        PRIME_COUNTING("Liczby pierwsze", "Kolejne liczby pierwsze", "U+1F4DF", new PrimeCounting()),
        SEMIPRIME_COUNTING("Liczby półpierwsze", "Kolejne liczby półpierwsze", "U+1F319", new SemiprimeCounting()),
        SPHENIC_COUNTING("Liczby sfeniczne", "Kolejne liczby sfeniczne", "U+26AA", new SphenicCounting()),
        //sequences
        FIBONACCI_SEQUENCE("Ciąg fibonacciego", "Kolejne wyrazy ciągu fibonacciego", "U+1F69C", new FibonacciSequence()),
        LUCAS_SEQUENCE("Ciąg Lucasa", "Kolejne wyrazy ciągu Lucasa", "U+1F471", new LucasSequence()),
        //others
        TRIANGULAR_COUNTING("Liczby trójkątne", "Kolejne liczby trójkątne", "U+1F4D0", new TriangularCounting()),
        PRONIC_COUNTING("Liczby proniczne", "Kolejne liczby proniczne", "U+1F4CF", new PronicCounting()),
        PALINDROMIC_COUNTING("Liczby palindromiczne", "Kolejne liczby palindromiczne", "U+1FA9E", new PalindromicCounting()),
        FACTORIAL_COUNTING("Silnie", "Kolejne silnie", "U+2757", new FactorialCounting()),
        ALPHABET("Alfabet", "Kolejne litery alfabetu angielskiego", "U+1F18E", new Alphabet()),
        TAUTOLOGIES("Tautologie", "Wyrażenia logiczne, które są tautologiami. Dopuszczalna składnia opisana jest [tutaj](https://matifilip.w.staszic.waw.pl/).", "U+2696", new Tautologies()),
        MINECRAFT_ITEM("Przedmioty z Minecraft", "Nazwy przedmiotów z Minecraft", "U+1F30D", new MinecraftItem()),
        //last letter
        POLISH_LAST_LETTER("Ostatnia litera (polski)", "Słowo zaczynające się ostatnią literą poprzedniego po polsku", "U+1F524", new PolishLastLetter()),
        ENGLISH_LAST_LETTER("Ostatnia litera (angielski)", "Słowo zaczynające się ostatnią literą poprzedniego po angielsku", "U+1F445", new EnglishLastLetter()),
        GERMAN_LAST_LETTER("Ostatnia litera (niemiecki)", "Słowo zaczynające się ostatnią literą poprzedniego po niemiecku", "U+1F7E5", new GermanLastLetter()),
        SPANISH_LAST_LETTER("Ostatnia litera (hiszpański)", "Słowo zaczynające się ostatnią literą poprzedniego po hiszpańsku", "U+1F7E8", new SpanishLastLetter());

        private final String name;
        @Getter private final String description;
        private final String emojiUnicode;
        @Getter private final ChannelHandler handler;
        Type(@NotNull String name, @NotNull String description, @NotNull String emojiUnicode, @NotNull ChannelHandler handler) {
            this.name = name;
            this.description = description;
            this.emojiUnicode = emojiUnicode;
            this.handler = handler;
        }

        @Override
        public @NotNull String toString() {
            return name;
        }
    }

    public static @NotNull List<SelectOption> getSelectMenuOptions() {
        return Arrays.stream(Type.values())
                .map(value -> SelectOption.of(value.toString(), value.toString())
                        .withDescription(value.description)
                        .withEmoji(Emoji.fromUnicode(value.emojiUnicode)))
                .toList();
    }

    public @NotNull CompletableFuture<Boolean> block(@NotNull JDA jda) {
        try {
            GuildMessageChannel channel = DiscordUtils.getSupportedChannelById(jda, getChannelId());
            return switch (channel) {
                case TextChannel chn -> chn.upsertPermissionOverride(channel.getGuild().getSelfMember()).grant(Permission.MESSAGE_SEND).submit()
                        .thenCompose(v -> chn.upsertPermissionOverride(channel.getGuild().getPublicRole()).deny(Permission.MESSAGE_SEND).submit())
                        .handle((v, e) -> e == null);
                case ThreadChannel chn -> {
                    CompletableFuture<Boolean> result = new CompletableFuture<>();
                    chn.getManager().setLocked(true).queue(
                            s -> result.complete(true),
                            f -> result.complete(false)
                    );
                    yield result;
                }
                case null, default -> CompletableFuture.completedFuture(false);
            };
        } catch (Exception e) {
            Logs.error("Failed to block the counting channel. (ID: `" + getChannelId() + "`)", e);
        }
        return CompletableFuture.completedFuture(false);
    }

    public @NotNull CompletableFuture<Boolean> unblock(@NotNull JDA jda) {
        try {
            GuildMessageChannel channel = DiscordUtils.getSupportedChannelById(jda, getChannelId());
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            switch (channel) {
                case TextChannel chn -> chn.getManager().sync().queue(
                        s -> result.complete(true),
                        f -> chn.upsertPermissionOverride(chn.getGuild().getPublicRole()).clear(Permission.MESSAGE_SEND).submit()
                                .handle((v, e) -> result.complete(e == null))
                );
                case ThreadChannel chn -> chn.getManager().setLocked(false).queue(
                        s -> result.complete(true),
                        f -> result.complete(false)
                );
                case null, default -> result.complete(false);
            }
            return result;
        } catch (Exception e) {
            Logs.error("Failed to unblock the counting channel. (ID: `" + getChannelId() + "`)", e);
        }
        return CompletableFuture.completedFuture(false);
    }
}
