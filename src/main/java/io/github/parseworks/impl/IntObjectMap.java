package io.github.parseworks.impl;

import java.util.Arrays;

/**
 * Specialized map from {@code int} keys to object values to avoid boxing.
 *
 * @param <V> value type
 */
public class IntObjectMap<V> {
    private static final int DEFAULT_CAPACITY = 16;
    private int[] keys;
    private Object[] values;
    private int size;

    /**
     * Constructs an empty IntObjectMap with the default initial capacity (16).
     */
    public IntObjectMap() {
        this.keys = new int[DEFAULT_CAPACITY];
        this.values = new Object[DEFAULT_CAPACITY];
        this.size = 0;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old value is replaced.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     */
    public void put(int key, V value) {
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                values[i] = value;
                return;
            }
        }

        if (size == keys.length) {
            grow();
        }

        keys[size] = key;
        values[size] = value;
        size++;
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     */
    @SuppressWarnings("unchecked")
    public V get(int key) {
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                return (V) values[i];
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if this map contains a mapping for the
     * specified key.
     *
     * @param key the key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified
     *         key, {@code false} otherwise
     */
    public boolean containsKey(int key) {
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     * Shifts any subsequent elements to the left.
     *
     * @param key the key whose mapping is to be removed from the map
     */
    public void remove(int key) {
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                if (i < size - 1) {
                    System.arraycopy(keys, i + 1, keys, i, size - i - 1);
                    System.arraycopy(values, i + 1, values, i, size - i - 1);
                }
                size--;
                return;
            }
        }
    }

    /**
     * Increases the capacity of this map instance, if necessary.
     * Capacity is doubled when the map is full.
     */
    private void grow() {
        int newCapacity = keys.length * 2;
        keys = Arrays.copyOf(keys, newCapacity);
        values = Arrays.copyOf(values, newCapacity);
    }
}