package me.matiego.counting.utils;

import java.util.Objects;

@SuppressWarnings("unused")
public class Pair<F, S> {

    public Pair(F F, S S) {
        this.F = F;
        this.S = S;
    }

    private F F;
    private S S;

    public F getFirst() {
        return F;
    }

    public void setFirst(F F) {
        this.F = F;
    }

    public S getSecond() {
        return S;
    }

    public void setSecond(S S) {
        this.S = S;
    }

    @Override
    public String toString() {
        return "(" + getFirst() + ", " + getSecond() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair<?, ?> pair)) return false;
        return Objects.equals(F, pair.F) && Objects.equals(S, pair.S);
    }

    @Override
    public int hashCode() {
        return Objects.hash(F, S);
    }
}
