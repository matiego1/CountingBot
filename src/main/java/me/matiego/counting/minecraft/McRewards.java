package me.matiego.counting.minecraft;

import me.matiego.counting.Main;
import org.jetbrains.annotations.NotNull;

public class McRewards {
    public McRewards(@NotNull Main instance) {
        this.instance = instance;
    }

    private final Main instance;

    public double getReward() {
        return 0;
    }
}
