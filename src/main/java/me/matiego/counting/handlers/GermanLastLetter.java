package me.matiego.counting.handlers;

import me.matiego.counting.Dictionary;
import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.IChannelHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GermanLastLetter implements IChannelHandler {

    private final List<String> ALPHABET = List.of("a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z,ä,ö,ü,ß".split(","));

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
            if (lastContent.charAt(lastContent.length() - 1) != msgContent.charAt(0)) {
                Utils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__INCORRECT_START_CHAR.toString());
                return null;
            }
        }
        if (msgContent.length() <= 3) {
            Utils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__TOO_SHORT.toString());
            return null;
        }
        if (msgContent.charAt(msgContent.length() - 1) == 'ß') {
            Utils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__ILLEGAL_END_CHAR.getFormatted("ß"));
            return null;
        }
        for (int i = 0; i < msgContent.length(); i++) {
            if (!ALPHABET.contains(String.valueOf(msgContent.charAt(i)))) {
                Utils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__ILLEGAL_CHAR.getFormatted(msgContent.charAt(i)));
                return null;
            }
        }

        boolean success = false;
        switch (Main.getInstance().getDictionary().useWord(Dictionary.Type.GERMAN, msgContent)) {
            case SUCCESS -> success = true;
            case NO_CHANGES -> Utils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__INCORRECT_WORD.toString());
            case FAILURE -> Utils.sendPrivateMessage(user, Translation.HANDLERS__LAST_LETTER__FAILURE.toString());
        }
        return success ? msgContent : null;
    }
}
