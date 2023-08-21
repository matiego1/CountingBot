package me.matiego.counting.handlers;

import me.matiego.counting.Dictionary;
import me.matiego.counting.utils.LastLetterHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class EnglishLastLetter extends LastLetterHandler {
    @Override
    public @NotNull List<Character> getAlphabet() {
        return List.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z');
    }

    @Override
    public @NotNull List<Character> getIllegalEndCharacters() {
        return Collections.emptyList();
    }

    @Override
    public @NotNull Dictionary.Type getType() {
        return Dictionary.Type.ENGLISH;
    }
}
