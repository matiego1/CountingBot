package me.matiego.counting;

import org.jetbrains.annotations.NotNull;

import java.util.IllegalFormatException;

/**
 * Translation of some messages.
 */
public enum Translation {
    /*--- Generals ---*/
    GENERAL__NOT_SENT("**Oops!** An error occurred while sending your message. Please try again later."),
    GENERAL__CLOSE_EMBED("Counting channel closed!"),
    GENERAL__OPEN_EMBED__TITLE("**This counting channel is now open!**"),
    GENERAL__OPEN_EMBED__DESCRIPTION("**Feel free to play with us!**\n\nChannel type: `{title}`\nDescription: `{description}`"),
    GENERAL__UNSUPPORTED_CHANNEL_TYPE("This channel's type is not supported."),
    GENERAL__UNKNOWN_LANGUAGE("Unknown language. Try again."),
    GENERAL__UNKNOWN_CHANNEL_TYPE("Unknown channel type. Try again."),
    /*--- Commands ---*/
    //counting
    COMMANDS__COUNTING__ADD("**__Select the type of the new counting channel:__**"),
    COMMANDS__COUNTING__REMOVE__SUCCESS("The channel has been successfully closed!"),
    COMMANDS__COUNTING__REMOVE__NO_CHANGES("This channel has been already closed."),
    COMMANDS__COUNTING__REMOVE__FAILURE("An error occurred. Try again."),
    COMMANDS__COUNTING__LIST__LIST("**__Open Counting Channels:__**"),
    COMMANDS__COUNTING__LIST__EMPTY_LIST("No counting channel has been opened yet. Open a new one with `/counting add`"),
    //dictionary
    COMMANDS__DICTIONARY__ADD__SUCCESS("This word has been successfully added to the dictionary!"),
    COMMANDS__DICTIONARY__ADD__FAILURE("An error occurred. Try again."),
    COMMANDS__DICTIONARY__REMOVE__SUCCESS("This word has been successfully removed from the dictionary!"),
    COMMANDS__DICTIONARY__REMOVE__FAILURE("An error occurred. Try again."),
    COMMANDS__DICTIONARY__LOAD__INCORRECT_KEY("Incorrect administrator key!\nThe admin-key can be found in the configuration file. This is to prevent uploading random files by people who do not have access to them."),
    COMMANDS__DICTIONARY__LOAD__SUCCESS("Success!"),
    COMMANDS__DICTIONARY__LOAD__NO_CHANGES("This file does not exist."),
    COMMANDS__DICTIONARY__LOAD__FAILURE("An error occurred. Try again."),
    //select menu
    COMMANDS__SELECT_MENU__SUCCESS("The channel has been successfully opened!"),
    COMMANDS__SELECT_MENU__NO_CHANGES("This channel is already opened!"),
    COMMANDS__SELECT_MENU__FAILURE("An error occurred. Try again."),
    /*--- Handlers ---*/
    HANDLERS__LAST_LETTER__INCORRECT_START_CHAR("**Oops!** Your message does not start with the last character of the previous message."),
    HANDLERS__LAST_LETTER__TOO_SHORT("**Oops!** Your message is too short."),
    HANDLERS__LAST_LETTER__ILLEGAL_CHAR("**Oops!** Your message contains illegal character: `%s`"),
    HANDLERS__LAST_LETTER__ILLEGAL_END_CHAR("**Oops!** Your message cannot end with one of the following characters: %s"),
    HANDLERS__LAST_LETTER__INCORRECT_WORD("**Oops!** This word does not exists in the dictionary or has already been used!"),
    HANDLERS__LAST_LETTER__FAILURE("**Oops!** An error occurred while loading dictionary. Try again.");

    private final String def;

    Translation(@NotNull String def) {
        this.def = def;
    }

    @Override
    public @NotNull String toString() {
        return Main.getInstance().getConfig().getString("translations." + name().replace("__", "."), def);
    }

    /**
     * Returns a formatted message.
     * @param args arguments
     * @return the formatted message
     */
    public @NotNull String getFormatted(Object... args) {
        try {
            return String.format(toString(), args);
        } catch (IllegalFormatException e) {
            return toString();
        }
    }
}
