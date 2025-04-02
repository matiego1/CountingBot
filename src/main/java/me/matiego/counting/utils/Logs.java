package me.matiego.counting.utils;

import me.matiego.counting.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Logs {
    private static final Logger logger = LogManager.getLogger();
    private static final int CONSOLE_THROWABLE_LINES = 3;

    public static void debug(@NotNull String message) {
        logger.debug(message);
    }

    public static void info(@NotNull String message) {
        logger.info(message);
        discord("INFO", message, null);
    }

    public static @NotNull CompletableFuture<Void> infoWithBlock(@NotNull String message) {
        logger.info(message);
        CompletableFuture<Message> future = discord("INFO", message, null);
        if (future == null) return CompletableFuture.completedFuture(null);
        return future.thenAccept(m -> {});
    }

    public static void infoLocal(@NotNull String message) {
        logger.info(message);
    }

    public static void warning(@NotNull String message) {
        warning(message, null);
    }

    public static void warning(@NotNull String message, @Nullable Throwable throwable) {
        logger.warn(message);

        if (throwable == null) {
            discord("__WARNING__", message, null);
            return;
        }

        StringBuilder content = new StringBuilder();
        logThrowable(throwable, line -> {
            logger.warn(line);
            content.append(line).append("\n");
        });

        EmbedBuilder eb = new EmbedBuilder();
        eb.setDescription(DiscordUtils.splitMessage(content.toString(), MessageEmbed.DESCRIPTION_MAX_LENGTH).getFirst());
        eb.setColor(Color.YELLOW);
        discord("__WARNING__", message, eb.build());
    }

    public static void warningLocal(@NotNull String message, @Nullable Throwable throwable) {
        logger.warn(message);

        if (throwable == null) return;
        logThrowable(throwable, logger::warn);
    }

    public static void error(@NotNull String message) {
        error(message, null);
    }

    public static void error(@NotNull String message, @Nullable Throwable throwable) {
        logger.error(message);

        if (throwable == null) {
            discord("__ERROR__", message, null);
            return;
        }

        StringBuilder content = new StringBuilder();
        logThrowable(throwable, line -> {
            logger.error(line);
            content.append(line).append("\n");
        });

        EmbedBuilder eb = new EmbedBuilder();
        eb.setDescription(DiscordUtils.splitMessage(content.toString(), MessageEmbed.DESCRIPTION_MAX_LENGTH).getFirst());
        eb.setColor(Color.RED);
        discord("__ERROR__", message, eb.build());
    }

    public static void errorLocal(@NotNull String message) {
        errorLocal(message, null);
    }

    public static void errorLocal(@NotNull String message, @Nullable Throwable throwable) {
        logger.error(message);

        if (throwable == null) return;
        logThrowable(throwable, logger::error);
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
        Main instance = Main.getInstance();
        if (instance == null) return null;
        JDA jda = instance.getJda();
        if (jda == null) return null;
        return jda.getTextChannelById(instance.getConfig().getLong("logs-channel-id"));
    }

    private static void logThrowable(@NotNull Throwable throwable, @NotNull Consumer<String> logLine) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        int lines = 0;
        for (String line : stringWriter.toString().split("\n")) {
            lines++;
            if (lines - 1 == CONSOLE_THROWABLE_LINES) {
                logLine.accept("\t... continued in logs file");
            }
            line = line.replaceAll("[\r\n]", "");
            if (lines > CONSOLE_THROWABLE_LINES) {
                logger.debug(line);
            } else {
                logLine.accept(line);
            }
        }
    }
}
