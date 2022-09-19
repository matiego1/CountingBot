package me.matiego.counting.counting.utils;

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
}
