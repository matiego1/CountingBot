package me.matiego.counting.handlers;

import me.matiego.counting.Dictionary;
import me.matiego.counting.LogicalExpressionsParser;
import me.matiego.counting.Main;
import me.matiego.counting.utils.ChannelHandler;
import me.matiego.counting.utils.DiscordUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.List;

public class Tautologies implements ChannelHandler {
    @Override
    public @Range(from = 0, to = 3) int getAmountOfMessages() {
        return 0;
    }

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
                DiscordUtils.sendPrivateMessage(user, "**Ups!** Twoje wyrażenie nie jest tautologią. Dowiedz się dlaczego: [tutaj](https://matifilip.w.staszic.waw.pl/)");
                return null;
            }
        } catch (Exception e) {
            DiscordUtils.sendPrivateMessage(user, "**Ups!** Twoje wyrażenie nie jest poprawne. Dowiedz się dlaczego: [tutaj](https://matifilip.w.staszic.waw.pl/)");
            return null;
        }

        boolean success = false;
        switch (Main.getInstance().getDictionary().markWordAsUsed(Dictionary.Type.TAUTOLOGIES, message.getGuild().getIdLong(), content)) {
            case SUCCESS -> success = true;
            case NO_CHANGES -> DiscordUtils.sendPrivateMessage(user, "**Ups!** Twoje wyrażenie już zostało użyte albo jest zablokowane na tym serwerze.");
            case FAILURE -> DiscordUtils.sendPrivateMessage(user, "**Ups!** Napotkano niespodziewany błąd przy ładowaniu słownika. Spróbuj później.");
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
