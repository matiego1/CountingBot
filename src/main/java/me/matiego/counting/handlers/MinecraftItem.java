package me.matiego.counting.handlers;

import me.matiego.counting.Dictionary;
import me.matiego.counting.Main;
import me.matiego.counting.utils.ChannelHandler;
import me.matiego.counting.utils.DiscordUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.List;

public class MinecraftItem implements ChannelHandler {
    @Override
    public @Range(from = 0, to = 3) int getAmountOfMessages() {
        return 0;
    }

    @Override
    public @Nullable String check(@NotNull Message message, @NotNull List<Message> history) {
        User user = message.getAuthor();
        String content = message.getContentDisplay().toLowerCase().replace("_", " ");

        if (!Main.getInstance().getDictionary().isWordInDictionary(Dictionary.Type.MINECRAFT_ITEM, content)) {
            DiscordUtils.sendPrivateMessage(user, "**Ups!** Ten przedmiot nie istnieje.");
            return null;
        }

        boolean success = false;
        switch (Main.getInstance().getDictionary().markWordAsUsed(Dictionary.Type.MINECRAFT_ITEM, message.getGuild().getIdLong(), content)) {
            case SUCCESS -> success = true;
            case NO_CHANGES -> DiscordUtils.sendPrivateMessage(user, "**Ups!** Ten przedmiot już został użyty albo jest on zablokowany na tym serwerze.");
            case FAILURE -> DiscordUtils.sendPrivateMessage(user, "**Ups!** Napotkano niespodziewany błąd przy ładowaniu słownika. Spróbuj później.");
        }
        return success ? content : null;
    }

    //<editor-fold desc="Generate minecraft item dictionary" defaultstate="collapsed">
//    public static void main(String[] args) throws IOException {
//        System.out.println("Generating...");
//        try (PrintWriter writer = new PrintWriter(new File(Main.PATH, "minecraft-dict.txt"), StandardCharsets.UTF_8)) {
//            Arrays.stream(Material.values())
//                    .map(Enum::name)
//                    .filter(item -> {
//                        try {
//                            return !Material.class.getField(item).isAnnotationPresent(Deprecated.class);
//                        } catch (Exception e) {
//                            return true;
//                        }
//                    })
//                    .map(String::toLowerCase)
//                    .map(item -> item.replace("_", " "))
//                    .forEach(i -> writer.print(i + "\n")); // force using unix line endings
//        }
//        System.out.println("Generated!");
//    }
    //</editor-fold>
}
