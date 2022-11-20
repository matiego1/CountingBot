package me.matiego.counting;

import me.matiego.counting.utils.*;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.ModalInteraction;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.context.MessageContextInteraction;
import net.dv8tion.jda.api.interactions.commands.context.UserContextInteraction;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenuInteraction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CommandHandler extends ListenerAdapter {
    public CommandHandler(@NotNull List<ICommandHandler> handlers) {
        List<CommandData> commandsData = new ArrayList<>();
        handlers.forEach(handler -> {
            CommandData data = handler.getCommand();
            if (data.getType() == Command.Type.SLASH) {
                commands.put(data.getName(), handler);
            } else {
                commands.put("#" + data.getName(), handler);
            }
            commandsData.add(data);
        });
        Main.getInstance().getJda().updateCommands().addCommands(commandsData).queue();
    }


    private final FixedSizeMap<String, Long> cooldown = new FixedSizeMap<>(1000);
    private final HashMap<String, ICommandHandler> commands = new HashMap<>();

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        User user = event.getUser();
        String command = event.getName();
        //check permissions
        if (!Utils.hasRequiredPermissions(event.getChannel())) {
            event.reply(Translation.COMMANDS__DELETE_MESSAGE__FAILURE__NO_PERMISSION.toString()).setEphemeral(true).queue();
            return;
        }
        //check cooldown
        long time = System.currentTimeMillis();
        long cooldownTime = cooldown.getOrDefault(new Pair<>(user.getId(), command).toString(), 0L);
        if (cooldownTime >= time) {
            event.reply(Translation.COMMANDS__COOLDOWN.getFormatted((cooldownTime - time) / Utils.SECOND)).setEphemeral(true).queue();
            return;
        }
        //get handler
        ICommandHandler handler = commands.get(command);
        if (handler == null) {
            event.reply(Translation.COMMANDS__UNKNOWN.toString()).setEphemeral(true).queue();
            return;
        }
        //execute command
        try {
            handler.onSlashCommandInteraction(event.getInteraction());
        } catch (Exception e) {
            event.reply(Translation.COMMANDS__ERROR.toString()).setEphemeral(true).queue(success -> {}, failure -> {});
            Logs.error("An error occurred while executing a command.", e);
        }
    }

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {
        SelectMenuInteraction interaction = event.getInteraction();
        for (ICommandHandler handler : commands.values()) {
            try {
                handler.onSelectMenuInteraction(interaction);
            } catch (Exception ignored) {}
            if (interaction.isAcknowledged()) return;
        }
        event.reply(Translation.COMMANDS__UNKNOWN.toString()).setEphemeral(true).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        ModalInteraction interaction = event.getInteraction();
        for (ICommandHandler handler : commands.values()) {
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
        for (ICommandHandler handler : commands.values()) {
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
        for (ICommandHandler handler : commands.values()) {
            try {
                handler.onUserContextInteraction(interaction);
            } catch (Exception ignored) {}
            if (interaction.isAcknowledged()) return;
        }
        event.reply(Translation.COMMANDS__UNKNOWN.toString()).setEphemeral(true).queue();
    }

    public synchronized void putSlowdown(@NotNull UserSnowflake user, @NotNull String command, long time) {
        cooldown.put(new Pair<>(user.getId(), command).toString(), System.currentTimeMillis() + time);
    }
}
