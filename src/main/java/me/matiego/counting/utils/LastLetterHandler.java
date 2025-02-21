package me.matiego.counting.utils;

import me.matiego.counting.Dictionary;
import me.matiego.counting.Main;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class LastLetterHandler implements ChannelHandler {
    public abstract @NotNull List<Character> getAlphabet();
    public abstract @NotNull List<Character> getIllegalEndCharacters();
    public abstract @NotNull Dictionary.Type getType();

    @Override
    public @Nullable String check(@NotNull Message message, @NotNull List<Message> history) {
        User user = message.getAuthor();

        String msgContent = message.getContentDisplay().toLowerCase();
        if (!history.isEmpty()) {
            String lastContent = history.getFirst().getContentDisplay().toLowerCase();
            if (lastContent.isEmpty() || msgContent.isEmpty() || lastContent.charAt(lastContent.length() - 1) != msgContent.charAt(0)) {
                DiscordUtils.sendPrivateMessage(user, "**Ups!** Twoje słowo nie zaczyna się ostatnim znakiem poprzedniego słowa.");
                return null;
            }
        }

        if (msgContent.length() < 3) {
            DiscordUtils.sendPrivateMessage(user, "**Ups!** Twoje słowo jest za krótkie.");
            return null;
        }

        List<Character> illegalEndCharacters = getIllegalEndCharacters();
        char lastChar = msgContent.charAt(msgContent.length() - 1);
        if (illegalEndCharacters.contains(lastChar)) {
            DiscordUtils.sendPrivateMessage(user, "**Ups!** Twoje słowo nie może się kończyć jednym z następujących znaków: %s".formatted(String.join(", ", illegalEndCharacters.stream().map(String::valueOf).toList())));
            return null;
        }

        List<Character> alphabet = getAlphabet();
        for (int i = 0; i < msgContent.length(); i++) {
            if (!alphabet.contains(msgContent.charAt(i))) {
                DiscordUtils.sendPrivateMessage(user, "**Ups!** Twoje słowo zawiera niedozwolony znak: `%s`".formatted(msgContent.charAt(i)));
                return null;
            }
        }

        Dictionary dictionary = Main.getInstance().getDictionary();
        Dictionary.Type type = getType();

        if (!dictionary.isWordInDictionary(type, msgContent)) {
            DiscordUtils.sendPrivateMessage(user, "**Ups!** To słowo nie występuję w słowniku. Jeśli uważasz to za błąd, skontaktuj się z administratorem bota.");
            return null;
        }

        boolean success = false;
        switch (dictionary.markWordAsUsed(getType(), message.getGuild().getIdLong(), msgContent)) {
            case SUCCESS -> success = true;
            case NO_CHANGES -> DiscordUtils.sendPrivateMessage(user, "**Ups!** To słowo zostało już użyte albo jest zablokowane na tym serwerze.");
            case FAILURE -> DiscordUtils.sendPrivateMessage(user, "**Ups!** Napotkano niespodziewany błąd przy ładowaniu słownika. Spróbuj później.");
        }
        return success ? msgContent : null;
    }

}
