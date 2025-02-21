package me.matiego.counting.utils;

import me.matiego.counting.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;

public class Logs {
    private static final Main plugin;

    static {
        plugin = Main.getInstance();
    }

    public static void info(@NotNull String message) {
        plugin.getLogger().info(message);
        discord("INFO", message, null);
    }

    public static @NotNull CompletableFuture<Void> infoWithBlock(@NotNull String message) {
        plugin.getLogger().info(message);
        CompletableFuture<Message> future = discord("INFO", message, null);
        if (future == null) return CompletableFuture.completedFuture(null);
        return future.thenAccept(m -> {});
    }

    public static void infoLocal(@NotNull String message) {
        plugin.getLogger().info(message);
    }

    public static void warning(@NotNull String message) {
        warning(message, null);
    }

    public static void warning(@NotNull String message, @Nullable Throwable throwable) {
        plugin.getLogger().warning(message);

        MessageEmbed embed = null;
        if (throwable != null) {
            StringWriter stringWriter = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stringWriter));
            for (String line : stringWriter.toString().split("\n")) plugin.getLogger().warning(line);

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.YELLOW);
            eb.setDescription(DiscordUtils.checkLength(stringWriter.toString(), MessageEmbed.DESCRIPTION_MAX_LENGTH));
            embed = eb.build();
        }

        discord("__WARNING__", message, embed);
    }

    public static void error(@NotNull String message) {
        error(message, null);
    }

    public static void error(@NotNull String message, @Nullable Throwable throwable) {
        plugin.getLogger().severe(message);

        MessageEmbed embed = null;
        if (throwable != null) {
            StringWriter stringWriter = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stringWriter));
            for (String line : stringWriter.toString().split("\n")) plugin.getLogger().severe(line);

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.RED);
            eb.setDescription(DiscordUtils.checkLength(stringWriter.toString(), MessageEmbed.DESCRIPTION_MAX_LENGTH));
            embed = eb.build();
        }

        discord("__ERROR__", message, embed);
    }

    private static @Nullable CompletableFuture<Message> discord(@NotNull String type, @NotNull String message, @Nullable MessageEmbed embed) {
        TextChannel chn = getConsoleChannel();
        if (chn == null) return null;
        MessageCreateAction action = chn.sendMessage(DiscordUtils.checkLength("**[<t:" + (Utils.now() / 1000) + ":T> " + type + "]:** " + message, Message.MAX_CONTENT_LENGTH));
        if (embed != null) {
            action.setEmbeds(embed);
        }
        return action.submit();
    }

    private static @Nullable TextChannel getConsoleChannel() {
        JDA jda = plugin.getJda();
        if (jda == null) return null;
        return jda.getTextChannelById(plugin.getConfig().getLong("logs-channel-id"));
    }
}
