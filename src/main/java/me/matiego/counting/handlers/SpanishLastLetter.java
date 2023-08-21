package me.matiego.counting.handlers;

import me.matiego.counting.Dictionary;
import me.matiego.counting.utils.LastLetterHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SpanishLastLetter extends LastLetterHandler {
    @Override
    public @NotNull List<Character> getAlphabet() {
        return List.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'é', 'ü', 'ú', 'í', 'ó', 'á', 'ç', 'ñ');
    }

    @Override
    public @NotNull List<Character> getIllegalEndCharacters() {
        return List.of('é', 'ü', 'ú', 'í', 'ó', 'á', 'ç', 'ñ');
    }

    @Override
    public @NotNull Dictionary.Type getType() {
        return Dictionary.Type.SPANISH;
    }
}
