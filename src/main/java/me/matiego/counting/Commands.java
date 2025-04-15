package me.matiego.counting;

import me.matiego.counting.utils.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.context.MessageContextInteraction;
import net.dv8tion.jda.api.interactions.commands.context.UserContextInteraction;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectInteraction;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Commands extends ListenerAdapter {
    public Commands(@NotNull JDA jda, @NotNull CommandHandler... handlers) {
        List<CommandData> commandsData = new ArrayList<>();
        for (CommandHandler handler : handlers) {
            CommandData data = handler.getCommand();
            commands.put(getCommandName(data), handler);
            commandsData.add(data);
        }
        jda.updateCommands().addCommands(commandsData).queue();
    }

    private final HashMap<String, Long> cooldown = Utils.createLimitedSizeMap(1000);
    private final HashMap<String, CommandHandler> commands = new HashMap<>();

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        User user = event.getUser();
        String command = event.getName();

        Logs.info(DiscordUtils.getAsTag(user) + " [" + user.getId() + "]: /" + event.getFullCommandName());

        //check permissions
        if (!DiscordUtils.hasRequiredPermissions(event.getChannel())) {
            event.reply("Nie mam wszystkich wymaganych uprawnień na tym kanale.").setEphemeral(true).queue();
            return;
        }

        //check cooldown
        String cooldownKey = new Pair<>(user.getId(), command).toString();
        long cooldownTime = cooldown.getOrDefault(cooldownKey, 0L);
        long now = Utils.now();
        if (cooldownTime >= now) {
            event.reply("Tej komendy możesz użyć ponownie za %s sekund.".formatted((cooldownTime - now) / 1000)).setEphemeral(true).queue();
            return;
        }

        //get handler
        CommandHandler handler = commands.get(command);
        if (handler == null) {
            event.reply("Nieznana komenda. Spróbuj ponownie.").setEphemeral(true).queue();
            return;
        }

        //execute command
        try {
            cooldown.put(cooldownKey, Utils.now() + (15 * 1000L));
            handler.onSlashCommandInteraction(event.getInteraction())
                    .thenAccept(time -> cooldown.put(cooldownKey, Utils.now() + (time * 1000L)));
        } catch (Exception e) {
            Logs.error("Failed to execute the slash command.", e);
            cooldown.put(cooldownKey, Utils.now() + (5 * 1000L));
            event.reply("Napotkano niespodziewany błąd. Spróbuj później.").setEphemeral(true).queue(s -> {}, f -> {});
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        StringSelectInteraction interaction = event.getInteraction();
        for (CommandHandler handler : commands.values()) {
            try {
                handler.onStringSelectInteraction(interaction);
            } catch (Exception ignored) {}
            if (interaction.isAcknowledged()) return;
        }
        event.reply("Nieznana komenda. Spróbuj ponownie.").setEphemeral(true).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        ModalInteraction interaction = event.getInteraction();
        for (CommandHandler handler : commands.values()) {
            try {
                handler.onModalInteraction(interaction);
            } catch (Exception ignored) {}
            if (interaction.isAcknowledged()) return;
        }
        event.reply("Nieznana komenda. Spróbuj ponownie.").setEphemeral(true).queue();
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
        MessageContextInteraction interaction = event.getInteraction();
        for (CommandHandler handler : commands.values()) {
            try {
                handler.onMessageContextInteraction(interaction);
            } catch (Exception ignored) {}
            if (interaction.isAcknowledged()) return;
        }
        event.reply("Nieznana komenda. Spróbuj ponownie.").setEphemeral(true).queue();
    }

    @Override
    public void onUserContextInteraction(@NotNull UserContextInteractionEvent event) {
        UserContextInteraction interaction = event.getInteraction();
        for (CommandHandler handler : commands.values()) {
            try {
                handler.onUserContextInteraction(interaction);
            } catch (Exception ignored) {}
            if (interaction.isAcknowledged()) return;
        }
        event.reply("Nieznana komenda. Spróbuj ponownie.").setEphemeral(true).queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        ButtonInteraction interaction = event.getInteraction();
        for (CommandHandler handler : commands.values()) {
            try {
                handler.onButtonInteraction(interaction);
            } catch (Exception ignored) {}
            if (interaction.isAcknowledged()) return;
        }
        event.reply("Nieznana komenda. Spróbuj ponownie.").setEphemeral(true).queue();
    }

    private @NotNull String getCommandName(@NotNull CommandData data) {
        if (data.getType() == Command.Type.SLASH) {
            return data.getName();
        }
        return data.getType().name() + "#" + data.getName();
    }
}
