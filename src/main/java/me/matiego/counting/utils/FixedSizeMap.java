package me.matiego.counting.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class FixedSizeMap<K, V> {
    public FixedSizeMap(@Range(from = 1, to = Integer.MAX_VALUE) int size) {
        this.size = size;
    }

    private final int size;
    private final LinkedHashMap<K, V> map = new LinkedHashMap<>();

    public void put(@NotNull K key, @NotNull V value) {
        if (map.size() >= size) {
            Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
        map.put(key, value);
    }

    public @Nullable V get(@NotNull K key) {
        return map.get(key);
    }

    public @NotNull V getOrDefault(@NotNull K key, @NotNull V def) {
        V value = get(key);
        return value == null ? def : value;
    }

    public @Nullable V remove(@NotNull K key) {
        return map.remove(key);
    }

}
