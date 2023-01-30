package me.matiego.counting.utils;

import me.matiego.counting.ChannelData;
import net.dv8tion.jda.api.entities.UserSnowflake;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class CountingMessageSendEvent extends Event implements Cancellable {
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
    private final Long previousMessage;
    private String displayName;

    public CountingMessageSendEvent(@NotNull ChannelData channel, @NotNull UserSnowflake user, @NotNull String displayName) {
        this(channel, user, displayName, null);
    }
    public CountingMessageSendEvent(@NotNull ChannelData channel, @NotNull UserSnowflake user, @NotNull String displayName, @Nullable Long previousMessageId) {
        super(true);
        this.channel = channel;
        this.user = user;
        this.displayName = displayName;
        this.previousMessage = previousMessageId;
    }

    public @NotNull ChannelData getChannel() {
        return channel;
    }

    public long getUserId() {
        return user.getIdLong();
    }

    public @NotNull String getDisplayName() {
        return displayName;
    }

    public @Nullable Long getPreviousMessageId() {
        return previousMessage;
    }

    public void setDisplayName(@NotNull String displayName) {
        this.displayName = displayName;
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
