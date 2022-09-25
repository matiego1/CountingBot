package me.matiego.counting.utils;

import me.matiego.counting.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

public class Logs {
    /**
     * Sends a normal message to the console.
     * @param message the message to send
     */
    public static void info(@NotNull String message) {
        Main.getInstance().getLogger().info(message);
        discord(message, Color.WHITE, "Information");
    }

    /**
     * Sends a warning to the console.
     * @param message the message to send
     */
    public static void warning(@NotNull String message) {
        Main.getInstance().getLogger().warning(message);
        discord(message, Color.YELLOW, "Warning");
    }

    /**
     * Sends an error to the console.
     * @param message the message to send
     */
    public static void error(@NotNull String message) {
        Main.getInstance().getLogger().severe(message);
        discord(message, Color.RED, "Error");
    }

    /**
     * Sends an error to the console.
     * This will also send a throwable stack trace to the console.
     * @param message the message to send
     * @param throwable the throwable whose stack trace is to be sent to the console
     */
    public static void error(@NotNull String message, @NotNull Throwable throwable) {
        error(message);
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        for (String line : stringWriter.toString().split("\n")) Main.getInstance().getLogger().severe(line);
    }

    private static void discord(@NotNull String message, @NotNull Color color, @NotNull String type) {
        JDA jda = Main.getInstance().getJda();
        if (jda == null) return;
        TextChannel chn = jda.getTextChannelById(Main.getInstance().getConfig().getLong("log-channel-id"));
        if (chn == null) {
            Main.getInstance().getLogger().warning("An error occurred while sending the message to the Discord log channel. Is the provided id correct?");
            return;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setDescription(Utils.checkLength(message, MessageEmbed.DESCRIPTION_MAX_LENGTH));
        eb.setColor(color);
        eb.setTimestamp(Instant.now());
        eb.setFooter(type);
        chn.sendMessageEmbeds(eb.build()).queue();
    }
}
