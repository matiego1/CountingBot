package me.matiego.counting.utils;

import me.matiego.counting.Dictionary;
import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class LastLetterHandler implements ChannelHandler {
    public abstract @NotNull List<Character> getAlphabet();
    public abstract @NotNull List<Character> getIllegalEndCharacters();
    public abstract @NotNull Dictionary.Type getType();


    /**
     * Checks if sent a message is correct.
     *
     * @param message the message sent by the user.
     * @param history the last messages from the channel - see {@link #getAmountOfMessages()}
     * @return {@code null} if the message is not correct, otherwise a new content of this message
     */
    @Override
    public @Nullable String check(@NotNull Message message, @NotNull List<Message> history) {
        User user = message.getAuthor();

        String msgContent = message.getContentDisplay().toLowerCase();
        if (!history.isEmpty()) {
            String lastContent = history.getFirst().getContentDisplay().toLowerCase();
            if (lastContent.isEmpty() || msgContent.isEmpty() || lastContent.charAt(lastContent.length() - 1) != msgContent.charAt(0)) {
                DiscordUtils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__INCORRECT_START_CHAR.toString());
                return null;
            }
        }

        if (msgContent.length() <= 3) {
            DiscordUtils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__TOO_SHORT.toString());
            return null;
        }

        List<Character> illegalEndCharacters = getIllegalEndCharacters();
        char lastChar = msgContent.charAt(msgContent.length() - 1);
        if (illegalEndCharacters.contains(lastChar)) {
            DiscordUtils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__ILLEGAL_END_CHAR.getFormatted(String.join(", ", illegalEndCharacters.stream().map(String::valueOf).toList())));
            return null;
        }

        List<Character> alphabet = getAlphabet();
        for (int i = 0; i < msgContent.length(); i++) {
            if (!alphabet.contains(msgContent.charAt(i))) {
                DiscordUtils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__ILLEGAL_CHAR.getFormatted(msgContent.charAt(i)));
                return null;
            }
        }

        Dictionary dictionary = Main.getInstance().getDictionary();
        Dictionary.Type type = getType();

        if (!dictionary.isWordInDictionary(type, msgContent)) {
            DiscordUtils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__NOT_IN_DICTIONARY.toString());
            return null;
        }

        boolean success = false;
        switch (dictionary.markWordAsUsed(getType(), message.getGuild().getIdLong(), msgContent)) {
            case SUCCESS -> success = true;
            case NO_CHANGES -> DiscordUtils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__INCORRECT_WORD.toString());
            case FAILURE -> DiscordUtils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__FAILURE.toString());
        }
        return success ? msgContent : null;
    }

}
