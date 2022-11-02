package me.matiego.counting.utils;

import me.matiego.counting.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

public class Logs {

    private static final Main plugin;

    static {
        plugin = Main.getInstance();
    }

    /**
     * Sends a normal message to the console.
     * @param message the message to send
     */
    public static void info(@NotNull String message) {
        plugin.getLogger().info(message);
    }

    /**
     * Sends a warning to the console and Discord logs channel.
     * @param message the message to send
     */
    public static void warning(@NotNull String message) {
        plugin.getLogger().warning(message);

        EmbedBuilder eb = new EmbedBuilder();
        eb.setDescription(Utils.checkLength(message, MessageEmbed.DESCRIPTION_MAX_LENGTH));
        eb.setColor(Color.YELLOW);
        eb.setTimestamp(Instant.now());
        eb.setFooter("Warning");
        sendToDiscord(eb.build());
    }

    /**
     * Sends a warning to the console and Discord logs channel.
     * @param message the message to send
     */
    public static void error(@NotNull String message) {
        plugin.getLogger().severe(message);

        EmbedBuilder eb = new EmbedBuilder();
        eb.setDescription(Utils.checkLength(message, MessageEmbed.DESCRIPTION_MAX_LENGTH));
        eb.setColor(Color.RED);
        eb.setTimestamp(Instant.now());
        eb.setFooter("Error");
        sendToDiscord(eb.build());
    }

    /**
     * Sends an error to the console and Discord logs channel.
     * This will also send a throwable stack trace.
     * @param message the message to send
     * @param throwable the throwable whose stack trace is to be sent to the console
     */
    public static void error(@NotNull String message, @NotNull Throwable throwable) {
        plugin.getLogger().severe(message);
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        for (String line : stringWriter.toString().split("\n")) plugin.getLogger().severe(line);

        EmbedBuilder eb = new EmbedBuilder();
        eb.setDescription(Utils.checkLength(message, MessageEmbed.DESCRIPTION_MAX_LENGTH));
        eb.addField("**Stack trace:**", Utils.checkLength(stringWriter.toString(), MessageEmbed.VALUE_MAX_LENGTH), false);
        eb.setColor(Color.RED);
        eb.setTimestamp(Instant.now());
        eb.setFooter("Error");
        sendToDiscord(eb.build());
    }

    /**
     * Sends an embed to Discord logs channel if possible.
     * @param embed the embed to send
     */
    private static void sendToDiscord(@NotNull MessageEmbed embed) {
        JDA jda = plugin.getJda();
        if (jda == null) return;
        TextChannel chn = jda.getTextChannelById(plugin.getConfig().getLong("logs-channel-id"));
        if (chn == null) return;
        chn.sendMessageEmbeds(embed).queue();
    }
}
