package me.matiego.counting.handlers;

import me.matiego.counting.Dictionary;
import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.ChannelHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpanishLastLetter implements ChannelHandler {

    private final List<String> ALPHABET = List.of("a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z,é,ü,ú,í,ó,á,ç,ñ".split(","));
    private final List<String> NOT_AT_END = List.of("é,ü,ú,í,ó,á,ç,ñ".split(","));

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

        String msgContent = message.getContentDisplay().toLowerCase();
        if (!history.isEmpty()) {
            String lastContent = history.get(0).getContentDisplay().toLowerCase();
            if (lastContent.isEmpty() || msgContent.isEmpty() || lastContent.charAt(lastContent.length() - 1) != msgContent.charAt(0)) {
                Utils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__INCORRECT_START_CHAR.toString());
                return null;
            }
        }
        if (msgContent.length() <= 3) {
            Utils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__TOO_SHORT.toString());
            return null;
        }
        if (NOT_AT_END.contains(String.valueOf(msgContent.charAt(msgContent.length() - 1)))) {
            Utils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__ILLEGAL_END_CHAR.getFormatted(String.join(", ", NOT_AT_END)));
            return null;
        }
        for (int i = 0; i < msgContent.length(); i++) {
            if (!ALPHABET.contains(String.valueOf(msgContent.charAt(i)))) {
                Utils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__ILLEGAL_CHAR.getFormatted(msgContent.charAt(i)));
                return null;
            }
        }

        boolean success = false;
        switch (Main.getInstance().getDictionary().useWord(Dictionary.Type.SPANISH, msgContent)) {
            case SUCCESS -> success = true;
            case NO_CHANGES -> Utils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__INCORRECT_WORD.toString());
            case FAILURE -> Utils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__FAILURE.toString());
        }
        return success ? msgContent : null;
    }
}
