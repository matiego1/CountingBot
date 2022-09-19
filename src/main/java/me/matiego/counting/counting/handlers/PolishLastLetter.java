package me.matiego.counting.counting.handlers;

import me.matiego.counting.counting.Dictionary;
import me.matiego.counting.counting.Main;
import me.matiego.counting.counting.utils.IChannelHandler;
import me.matiego.counting.counting.utils.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PolishLastLetter implements IChannelHandler {

    private final List<String> ALPHABET = List.of("a,ą,b,c,ć,d,e,ę,f,g,h,i,j,k,l,ł,m,n,ń,o,ó,p,q,r,s,ś,t,u,v,w,x,y,z,ź,ż".split(","));

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
                Utils.sendPrivateMessage(user, "**Oops!** Your message does not start with the last character of the previous message.");
                return null;
            }
        }
        if (msgContent.length() <= 3) {
            Utils.sendPrivateMessage(user, "**Oops!** Your message is too short.");
            return null;
        }
        char lastChar = msgContent.charAt(msgContent.length() - 1);
        if (lastChar == 'ą' || lastChar == 'ę' || lastChar == 'ń') {
            Utils.sendPrivateMessage(user, "**Oops!** Your message cannot end with one of the following characters: ą, ę, ń");
            return null;
        }
        for (int i = 0; i < msgContent.length(); i++) {
            if (!ALPHABET.contains(String.valueOf(msgContent.charAt(i)))) {
                Utils.sendPrivateMessage(user, "**Oops!** Your message contains illegal character: `" + msgContent.charAt(i) + "`");
                return null;
            }
        }

        boolean success = false;
        switch (Main.getInstance().getDictionary().useWord(Dictionary.Type.POLISH, msgContent)) {
            case SUCCESS -> success = true;
            case NO_CHANGES -> Utils.sendPrivateMessage(user, "**Oops!** This word does not exists in the dictionary or has already been used!");
            case FAILURE -> Utils.sendPrivateMessage(user, "**Oops!** An error occurred while loading dictionary. Try again.");
        }
        return success ? msgContent : null;
    }
}
