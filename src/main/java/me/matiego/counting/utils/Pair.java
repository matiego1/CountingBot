package me.matiego.counting.utils;

@SuppressWarnings("unused")
public class Pair<FIRST, SECOND> {

    public Pair(FIRST first, SECOND second) {
        this.first = first;
        this.second = second;
    }

    private FIRST first;
    private SECOND second;

    public FIRST getFirst() {
        return first;
    }

    public void setFirst(FIRST first) {
        this.first = first;
    }

    public SECOND getSecond() {
        return second;
    }

    public void setSecond(SECOND second) {
        this.second = second;
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
