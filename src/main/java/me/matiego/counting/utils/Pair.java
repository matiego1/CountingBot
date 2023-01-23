package me.matiego.counting.utils;

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
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Pair<?, ?> pair)) return false;
        return getFirst().equals(pair.getFirst()) && getSecond().equals(pair.getSecond());
    }
}
