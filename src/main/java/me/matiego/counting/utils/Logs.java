package me.matiego.counting.utils;

import me.matiego.counting.Main;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Logs {
    /**
     * Sends a normal message to the console.
     * @param message the message to send
     */
    public static void info(@NotNull String message) {
        Main.getInstance().getLogger().info(message);
    }

    /**
     * Sends a warning to the console.
     * @param message the message to send
     */
    public static void warning(@NotNull String message) {
        Main.getInstance().getLogger().warning(message);
    }

    /**
     * Sends an error to the console.
     * @param message the message to send
     */
    public static void error(@NotNull String message) {
        Main.getInstance().getLogger().severe(message);
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
}
