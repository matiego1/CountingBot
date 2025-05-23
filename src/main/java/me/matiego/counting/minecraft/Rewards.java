package me.matiego.counting.minecraft;

import me.matiego.counting.Main;
import me.matiego.counting.utils.Pair;
import me.matiego.counting.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.simpleyaml.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.Map;

public class Rewards {
    public Rewards(@NotNull Main instance) {
        this.instance = instance;
    }

    private final Main instance;
    private final Map<Pair<Long, Long>, Long> cache = Collections.synchronizedMap(Utils.createLimitedSizeMap(5000));

    public double getReward(long guildId, long userId, long channelId, @NotNull String channelType, long previousMessageDate) {
        if (!instance.getApiRequests().isEnabled()) return 0;

        FileConfiguration config = instance.getConfig();
        if (config.getLongList("minecraft.disabled-ids").contains(guildId)) return 0;
        if (config.getLongList("minecraft.disabled-ids").contains(userId)) return 0;
        if (config.getLongList("minecraft.disabled-ids").contains(channelId)) return 0;

        long now = Utils.now();
        long last = getLast(channelId, userId);
        if (now - last <= getInterval(config)) return 0;

        double reward = getChannelReward(config, channelType);
        if (now - previousMessageDate >= getOldMessage(config)) {
            reward *= getOldMessageMultiplier(config);
        }
        reward = Math.max(0, Math.min(1000 * 1000 * 1000, reward));

        if (!setLast(channelId, userId, now)) return 0;
        return reward;
    }

    public long getInterval(@NotNull FileConfiguration config) {
        return Math.max(60, config.getLong("minecraft.interval", 600)) * 1000L;
    }

    public double getChannelReward(@NotNull FileConfiguration config, @NotNull String channelType) {
        return Math.max(0, Utils.round(config.getDouble("minecraft.types." + channelType.toLowerCase()), 2));
    }

    public long getOldMessage(@NotNull FileConfiguration config) {
        return Math.max(60, config.getLong("minecraft.old-message", Long.MAX_VALUE)) * 1000L;
    }

    public double getOldMessageMultiplier(@NotNull FileConfiguration config) {
        return Math.max(1, config.getDouble("minecraft.old-message-multiplier"));
    }

    public long getLast(long channelId, long userId) {
        Long cached = cache.get(new Pair<>(channelId, userId));
        if (cached != null) return cached;

        // TODO: mysql
        return 0;
    }

    public boolean setLast(long channelId, long userId, long time) {
        cache.put(new Pair<>(channelId, userId), time);

        // TODO: mysql
        return true;
    }
}
