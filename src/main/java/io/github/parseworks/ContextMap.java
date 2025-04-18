package io.github.parseworks;

import java.util.HashMap;
import java.util.Map;

public class ContextMap<K, V> extends HashMap<K, V> {
    private final Map<K, V> parent;

    // Constructor for root context
    public ContextMap() {
        this(null);
    }

    // Constructor for child context with a parent
    public ContextMap(Map<K, V> parent) {
        super();
        this.parent = parent;
    }

    // Get a value from the current context or parent context
    @Override
    public V get(Object key) {
        V value = super.get(key);
        if (value == null && parent != null) {
            return parent.get(key);
        }
        return value;
    }

    // Check if a key exists in the current context or parent context
    @Override
    public boolean containsKey(Object key) {
        return this.get(key) != null;
    }

    // Get the parent context
    public Map<K, V> getParent() {
        return parent;
    }
}