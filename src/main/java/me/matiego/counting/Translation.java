package me.matiego.counting;

import org.jetbrains.annotations.NotNull;

import java.util.IllegalFormatException;

/**
 * Translation of some messages.
 */
@SuppressWarnings("unused")
public enum Translation {
    /*--- Generals ---*/
    GENERAL__NOT_SENT("**Oops!** An error occurred while sending your message. Please try again later."),
    GENERAL__CLOSE_EMBED("Counting channel closed!"),
    GENERAL__OPEN_EMBED__TITLE("**This counting channel is now open!**"),
    GENERAL__OPEN_EMBED__DESCRIPTION("**Feel free to play with us!**\n\nChannel type: `%s`\nDescription: `%s`"),
    GENERAL__UNSUPPORTED_CHANNEL_TYPE("This channel's type is not supported."),
    GENERAL__UNKNOWN_LANGUAGE("Unknown language. Try again."),
    GENERAL__UNKNOWN_CHANNEL_TYPE("Unknown channel type. Try again."),
    GENERAL__STATUS("counting channels"),
    GENERAL__DO_NOT_SPAM("Do not spam on the counting channel! You can only send %s messages with less than %s seconds between each."),
    /*--- Commands ---*/
    COMMANDS__UNKNOWN("Unknown command."),
    COMMANDS__COOLDOWN("You can use this command again in %s seconds."),
    COMMANDS__DISABLED_CHANNEL("You cannot use this command here."),
    COMMANDS__ERROR("An error occurred. Try again."),
    //ping
    COMMANDS__PING__NAME("ping"),
    COMMANDS__PING__DESCRIPTION("Shows the current ping of the bot"),
    COMMANDS__PING__OPTION__NAME("ephemeral"),
    COMMANDS__PING__OPTION__DESCRIPTION("whether this message should only be visible to you"),
    //about
    COMMANDS__ABOUT__NAME("about"),
    COMMANDS__ABOUT__DESCRIPTION("Shows some basic information about this bot."),
    COMMANDS__ABOUT__OPTION__NAME("ephemeral"),
    COMMANDS__ABOUT__OPTION__DESCRIPTION("whether this message should only be visible to you"),
    COMMANDS__ABOUT__MESSAGE("""
            **__Counting bot__**
            Counts in different ways and have a lot of fun!
            
            **Author**: `Matiego#8675`
            **Source code**: https://github.com/matiego1/CountingBot
            **Version**: `%s`
            """),
    //feedback
    COMMANDS__FEEDBACK__NAME("feedback"),
    COMMANDS__FEEDBACK__DESCRIPTION("Report a bug, suggest a change or share your impressions"),
    COMMANDS__FEEDBACK__TITLE("Send feedback"),
    COMMANDS__FEEDBACK__SUBJECT("Subject"),
    COMMANDS__FEEDBACK__SUBJECT_PLACEHOLDER("e.g. bug report, suggestion"),
    COMMANDS__FEEDBACK__MODAL_DESCRIPTION("Description"),
    COMMANDS__FEEDBACK__SUCCESS("Thank you for your feedback!"),
    COMMANDS__FEEDBACK__FAILURE("An error occurred. Try again."),
    COMMANDS__FEEDBACK__OPEN_CHANNELS("Opened %s channels out of %s!"),
    //counting
    COMMANDS__COUNTING__NAME("counting"),
    COMMANDS__COUNTING__DESCRIPTION("Manages the counting channels"),
    COMMANDS__COUNTING__OPTIONS__ADD__NAME("add"),
    COMMANDS__COUNTING__OPTIONS__ADD__DESCRIPTION("Opens a new counting channel"),
    COMMANDS__COUNTING__OPTIONS__REMOVE__NAME("remove"),
    COMMANDS__COUNTING__OPTIONS__REMOVE__DESCRIPTION("Closes the counting channel"),
    COMMANDS__COUNTING__OPTIONS__LIST__NAME("list"),
    COMMANDS__COUNTING__OPTIONS__LIST__DESCRIPTION("Shows all opened counting channel in this guild"),
    COMMANDS__COUNTING__ADD("**__Select the type of the new counting channel:__**"),
    COMMANDS__COUNTING__REMOVE__SUCCESS("The channel has been successfully closed!"),
    COMMANDS__COUNTING__REMOVE__NO_CHANGES("This channel has been already closed."),
    COMMANDS__COUNTING__REMOVE__FAILURE("An error occurred. Try again."),
    COMMANDS__COUNTING__LIST__LIST("**__Open Counting Channels:__**"),
    COMMANDS__COUNTING__LIST__EMPTY_LIST("No counting channel has been opened yet. Open a new one with `/counting add`"),
    //dictionary
    COMMANDS__DICTIONARY__NAME("dictionary"),
    COMMANDS__DICTIONARY__DESCRIPTION("Manages dictionaries"),
    COMMANDS__DICTIONARY__TYPES__POLISH("Polish"),
    COMMANDS__DICTIONARY__TYPES__ENGLISH("English"),
    COMMANDS__DICTIONARY__TYPES__GERMAN("German"),
    COMMANDS__DICTIONARY__TYPES__SPANISH("Spanish"),
    COMMANDS__DICTIONARY__OPTIONS__ADD__NAME("add"),
    COMMANDS__DICTIONARY__OPTIONS__ADD__DESCRIPTION("Adds a word to the dictionary"),
    COMMANDS__DICTIONARY__OPTIONS__REMOVE__NAME("remove"),
    COMMANDS__DICTIONARY__OPTIONS__REMOVE__DESCRIPTION("Removes a word from the dictionary"),
    COMMANDS__DICTIONARY__OPTIONS__LOAD__NAME("load"),
    COMMANDS__DICTIONARY__OPTIONS__LOAD__DESCRIPTION("Loads a file into the dictionary"),
    COMMANDS__DICTIONARY__OPTIONS__LANGUAGE__NAME("language"),
    COMMANDS__DICTIONARY__OPTIONS__LANGUAGE__DESCRIPTION("The dictionary's type"),
    COMMANDS__DICTIONARY__OPTIONS__WORD__NAME("word"),
    COMMANDS__DICTIONARY__OPTIONS__WORD__DESCRIPTION("A word to add/remove"),
    COMMANDS__DICTIONARY__OPTIONS__ADMIN_KEY__NAME("admin-key"),
    COMMANDS__DICTIONARY__OPTIONS__ADMIN_KEY__DESCRIPTION("The secret administrator key"),
    COMMANDS__DICTIONARY__OPTIONS__FILE__NAME("file"),
    COMMANDS__DICTIONARY__OPTIONS__FILE__DESCRIPTION("The dictionary file"),
    COMMANDS__DICTIONARY__ADD__SUCCESS("This word has been successfully added to the dictionary in %s ms."),
    COMMANDS__DICTIONARY__ADD__FAILURE("An error occurred. Try again."),
    COMMANDS__DICTIONARY__REMOVE__SUCCESS("This word has been successfully removed from the dictionary in %s ms."),
    COMMANDS__DICTIONARY__REMOVE__FAILURE("An error occurred. Try again."),
    COMMANDS__DICTIONARY__LOAD__INCORRECT_KEY("Incorrect administrator key!\nThe admin-key can be found in the configuration file. This is to prevent uploading random files by people who do not have access to them."),
    COMMANDS__DICTIONARY__LOAD__SUCCESS("Success! (%s ms)"),
    COMMANDS__DICTIONARY__LOAD__NO_CHANGES("This file does not exist."),
    COMMANDS__DICTIONARY__LOAD__FAILURE("An error occurred. Try again."),
    //select menu
    COMMANDS__SELECT_MENU__SUCCESS("The channel has been successfully opened!"),
    COMMANDS__SELECT_MENU__NO_CHANGES("This channel is already opened!"),
    COMMANDS__SELECT_MENU__FAILURE("An error occurred. Try again."),
    //delete message
    COMMANDS__DELETE_MESSAGE__NAME("delete this message"),
    COMMANDS__DELETE_MESSAGE__FAILURE("You cannot delete this message."),
    COMMANDS__DELETE_MESSAGE__SUCCESS("Success!"),
    /*--- Handlers ---*/
    HANDLERS__LAST_LETTER__INCORRECT_START_CHAR("**Oops!** Your message does not start with the last character of the previous message."),
    HANDLERS__LAST_LETTER__TOO_SHORT("**Oops!** Your message is too short."),
    HANDLERS__LAST_LETTER__ILLEGAL_CHAR("**Oops!** Your message contains illegal character: `%s`"),
    HANDLERS__LAST_LETTER__ILLEGAL_END_CHAR("**Oops!** Your message cannot end with one of the following characters: %s"),
    HANDLERS__LAST_LETTER__INCORRECT_WORD("**Oops!** This word does not exists in the dictionary or has already been used!"),
    HANDLERS__LAST_LETTER__FAILURE("**Oops!** An error occurred while loading dictionary. Try again."),
    /*--- Channel types ---*/
    TYPE__COUNTING__NAME("Counting"),
    TYPE__COUNTING__DESCRIPTION("Write next numbers"),
    TYPE__BINARY_COUNTING__NAME("Binary Counting"),
    TYPE__BINARY_COUNTING__DESCRIPTION("Write next numbers in binary"),
    TYPE__HEXADECIMAL_COUNTING__NAME("Hexadecimal Counting"),
    TYPE__HEXADECIMAL_COUNTING__DESCRIPTION("Write next hexadecimal numbers"),
    TYPE__PRIME_COUNTING__NAME("Prime Counting"),
    TYPE__PRIME_COUNTING__DESCRIPTION("Write next prime numbers"),
    TYPE__SEMIPRIME_COUNTING__NAME("Semiprime counting"),
    TYPE__SEMIPRIME_COUNTING__DESCRIPTION("Write next semiprime numbers"),
    TYPE__SPHENIC_COUNTING__NAME("Sphenic counting"),
    TYPE__SPHENIC_COUNTING__DESCRIPTION("Write next sphenic numbers"),
    TYPE__FIBONACCI_SEQUENCE__NAME("Fibonacci sequence"),
    TYPE__FIBONACCI_SEQUENCE__DESCRIPTION("Write next numbers of the fibonacci sequence"),
    TYPE__LUCAS_NUMBERS__NAME("Lucas numbers"),
    TYPE__LUCAS_NUMBERS__DESCRIPTION("Write next numbers of the lucas sequence"),
    TYPE__TRIANGULAR_NUMBERS__NAME("Triangular numbers"),
    TYPE__TRIANGULAR_NUMBERS__DESCRIPTION("Write next triangular numbers"),
    TYPE__PALINDROMIC_NUMBERS__NAME("Palindromic numbers"),
    TYPE__PALINDROMIC_NUMBERS__DESCRIPTION("Write next palindromic numbers"),
    TYPE__ALPHABET__NAME("Alphabet"),
    TYPE__ALPHABET__DESCRIPTION("Write next letters in english alphabet"),
    TYPE__POLISH_LAST_LETTER__NAME("Polish last letter"),
    TYPE__POLISH_LAST_LETTER__DESCRIPTION("Write a word that starts with the last letter of the previous one (Polish)"),
    TYPE__ENGLISH_LAST_LETTER__NAME("English last letter"),
    TYPE__ENGLISH_LAST_LETTER__DESCRIPTION("Write a word that starts with the last letter of the previous one (English)"),
    TYPE__GERMAN_LAST_LETTER__NAME("German last letter"),
    TYPE__GERMAN_LAST_LETTER__DESCRIPTION("Write a word that starts with the last letter of the previous one (German)"),
    TYPE__SPANISH_LAST_LETTER__NAME("Spanish last letter"),
    TYPE__SPANISH_LAST_LETTER__DESCRIPTION("Write a word that starts with the last letter of the previous one (Spanish)");

    private final String def;

    Translation(@NotNull String def) {
        this.def = def;
    }

    @Override
    public @NotNull String toString() {
        return Main.getInstance().getConfig().getString("translations." + name().replace("__", ".").toLowerCase(), def).replace("\\n", "\n");
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

    /**
     * Returns the default value for this translation.
     * @return the default translation
     */
    public @NotNull String getDefault() {
        return def;
    }
}
