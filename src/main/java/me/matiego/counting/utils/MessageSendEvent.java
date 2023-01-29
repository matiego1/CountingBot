package me.matiego.counting.utils;

import me.matiego.counting.ChannelData;
import net.dv8tion.jda.api.entities.UserSnowflake;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MessageSendEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    private final ChannelData channel;
    private final UserSnowflake user;
    private final String messageContent;

    public MessageSendEvent(@NotNull ChannelData channel, @NotNull UserSnowflake user, @NotNull String messageContent) {
        super(true);
        this.channel = channel;
        this.user = user;
        this.messageContent = messageContent;
    }

    public @NotNull ChannelData getChannel() {
        return channel;
    }

    public long getUserId() {
        return user.getIdLong();
    }

    public @NotNull String getMessageContent() {
        return messageContent;
    }

    private boolean cancelled = false;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
