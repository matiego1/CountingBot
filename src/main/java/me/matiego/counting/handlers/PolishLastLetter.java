package me.matiego.counting.handlers;

import me.matiego.counting.Dictionary;
import me.matiego.counting.utils.LastLetterHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PolishLastLetter extends LastLetterHandler {
    @Override
    public @NotNull List<Character> getAlphabet() {
        return List.of('a', 'ą', 'b', 'c', 'ć', 'd', 'e', 'ę', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'ł', 'm', 'n', 'ń', 'o', 'ó', 'p', 'r', 's', 'ś', 't', 'u', 'w', 'y', 'z', 'ź', 'ż');
    }

    @Override
    public @NotNull List<Character> getIllegalEndCharacters() {
        return List.of('ą', 'ę', 'ń');
    }

    @Override
    public @NotNull Dictionary.Type getType() {
        return Dictionary.Type.POLISH;
    }
}
