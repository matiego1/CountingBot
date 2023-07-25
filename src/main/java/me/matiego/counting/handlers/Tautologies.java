package me.matiego.counting.handlers;

import me.matiego.counting.Dictionary;
import me.matiego.counting.LogicalExpressionsParser;
import me.matiego.counting.Main;
import me.matiego.counting.Translation;
import me.matiego.counting.utils.ChannelHandler;
import me.matiego.counting.utils.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.List;

public class Tautologies implements ChannelHandler {
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
        String content = LogicalExpressionsParser.simplify(message.getContentDisplay()
                .replaceAll("\\s", "")
                .replace("<=>", "⇔")
                .replace("=>", "⇒")
                .replace("v", "∨")
                .replace("^", "∧")
                .replace("+", "⊕"));

        try {
            if (!LogicalExpressionsParser.isTautology(content)) {
                Utils.sendPrivateMessage(user, Translation.HANDLERS__TAUTOLOGIES__NOT_TAUTOLOGY.toString());
                return null;
            }
        } catch (Exception e) {
            Utils.sendPrivateMessage(user, Translation.HANDLERS__TAUTOLOGIES__INCORRECT_EXPRESSION.toString());
            return null;
        }

        boolean success = false;
        switch (Main.getInstance().getDictionary().useWord(Dictionary.Type.TAUTOLOGIES, content)) {
            case SUCCESS -> success = true;
            case NO_CHANGES -> Utils.sendPrivateMessage(user, Translation.HANDLERS__TAUTOLOGIES__ALREADY_EXISTS.toString());
            case FAILURE -> Utils.sendPrivateMessage(user, Translation.HANDLERS__TAUTOLOGIES__FAILURE.toString());
        }
        if (!success) return null;

        return content
                .replace("⇔", " ⇔ ")
                .replace("⇒", " ⇒ ")
                .replace("∨", " ∨ ")
                .replace("∧", " ∧ ")
                .replace("⊕", " ⊕ ");
    }
}
