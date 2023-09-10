package me.matiego.counting.handlers;

import me.matiego.counting.Dictionary;
import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.ChannelHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.Arrays;
import java.util.List;

public class MinecraftItem implements ChannelHandler {
    /**
     * Returns the amount of messages retrieved from the channel history.
     *
     * @return the amount of messages.
     */
    @Override
    public @Range(from = 0, to = 3) int getAmountOfMessages() {
        return 0;
    }

    /**
     * Checks if the sent message is correct.
     *
     * @param message the message sent by the user.
     * @param history the last messages from the channel - see {@link #getAmountOfMessages()}
     * @return {@code null} if the message is not correct, otherwise a new content of this message
     */
    @Override
    public @Nullable String check(@NotNull Message message, @NotNull List<Message> history) {
        User user = message.getAuthor();
        String content = message.getContentDisplay().toLowerCase().replace("_", " ");

        if (doesItemNotExist(content)) {
            Utils.sendPrivateMessage(user, Translation.HANDLERS__MINECRAFT_ITEM__ITEM_DOES_NOT_EXIST.toString());
            return null;
        }

        boolean success = false;
        switch (Main.getInstance().getDictionary().markWordAsUsed(Dictionary.Type.MINECRAFT_ITEM, message.getGuild().getIdLong(), content)) {
            case SUCCESS -> success = true;
            case NO_CHANGES -> Utils.sendPrivateMessage(user, Translation.HANDLERS__MINECRAFT_ITEM__ALREADY_EXISTS.toString());
            case FAILURE -> Utils.sendPrivateMessage(user, Translation.HANDLERS__MINECRAFT_ITEM__FAILURE.toString());
        }
        return success ? content : null;
    }

    private boolean doesItemNotExist(@NotNull String name) {
        return Arrays.stream(Material.values())
                .map(Enum::name)
                .filter(item -> {
                    try {
                        return !Material.class.getField(item).isAnnotationPresent(Deprecated.class);
                    } catch (Exception e) {
                        return true;
                    }
                })
                .map(String::toLowerCase)
                .map(item -> item.replace("_", " "))
                .noneMatch(item -> item.equals(name));
    }
}
