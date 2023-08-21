package me.matiego.counting;

import me.matiego.counting.utils.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
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
    public Commands(@NotNull Main plugin, @NotNull CommandHandler... handlers) {
        List<CommandData> commandsData = new ArrayList<>();
        for (CommandHandler handler : handlers) {
            CommandData data = handler.getCommand();
            commands.put(getCommandName(data), handler);
            commandsData.add(data);
        }
        JDA jda = plugin.getJda();
        if (jda == null) throw new NullPointerException("JDA is null");
        jda.updateCommands().addCommands(commandsData).queue();
    }


    private final FixedSizeMap<String, Long> cooldown = new FixedSizeMap<>(1000);
    private final HashMap<String, CommandHandler> commands = new HashMap<>();

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        User user = event.getUser();
        String command = event.getName();

        Logs.info(Utils.getAsTag(user) + " [" + user.getId() + "]: /" + command);

        //check permissions
        if (!Utils.hasRequiredPermissions(event.getChannel())) {
            event.reply(Translation.GENERAL__NO_PERMISSION.toString()).setEphemeral(true).queue();
            return;
        }

        //check cooldown
        long time = Utils.now();
        long cooldownTime = cooldown.getOrDefault(new Pair<>(user.getId(), command).toString(), 0L);
        if (cooldownTime >= time) {
            event.reply(Translation.COMMANDS__COOLDOWN.getFormatted((cooldownTime - time) / Utils.SECOND)).setEphemeral(true).queue();
            return;
        }

        //get handler
        CommandHandler handler = commands.get(command);
        if (handler == null) {
            event.reply(Translation.COMMANDS__UNKNOWN.toString()).setEphemeral(true).queue();
            return;
        }

        //execute command
        try {
            handler.onSlashCommandInteraction(event.getInteraction());
        } catch (Exception e) {
            Logs.error("An error occurred while executing a command.", e);
            event.reply(Translation.COMMANDS__ERROR.toString()).setEphemeral(true).queue(success -> {}, failure -> {});
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
        event.reply(Translation.COMMANDS__UNKNOWN.toString()).setEphemeral(true).queue();
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
        event.reply(Translation.COMMANDS__UNKNOWN.toString()).setEphemeral(true).queue();
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
        event.reply(Translation.COMMANDS__UNKNOWN.toString()).setEphemeral(true).queue();
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
        event.reply(Translation.COMMANDS__UNKNOWN.toString()).setEphemeral(true).queue();
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
        event.reply(Translation.COMMANDS__UNKNOWN.toString()).setEphemeral(true).queue();
    }

    public synchronized void putSlowdown(@NotNull UserSnowflake user, @NotNull String command, long time) {
        cooldown.put(new Pair<>(user.getId(), command).toString(), Utils.now() + time);
    }

    private @NotNull String getCommandName(@NotNull CommandData data) {
        if (data.getType() == Command.Type.SLASH) {
            return data.getName();
        }
        return data.getType().name() + "#" + data.getName();
    }
}
