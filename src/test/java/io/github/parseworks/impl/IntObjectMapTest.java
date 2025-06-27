package io.github.parseworks.impl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the IntObjectMap class.
 * These tests verify that the specialized map implementation correctly maps integer keys to object values.
 */
public class IntObjectMapTest {

    @Test
    public void testPutAndGet() {
        IntObjectMap<String> map = new IntObjectMap<>();
        
        // Test putting and getting a single value
        map.put(1, "one");
        assertEquals("one", map.get(1));
        
        // Test getting a non-existent key
        assertNull(map.get(2));
        
        // Test overwriting an existing key
        map.put(1, "ONE");
        assertEquals("ONE", map.get(1));
    }
    
    @Test
    public void testContainsKey() {
        IntObjectMap<String> map = new IntObjectMap<>();
        
        // Test with empty map
        assertFalse(map.containsKey(1));
        
        // Test after adding a key
        map.put(1, "one");
        assertTrue(map.containsKey(1));
        assertFalse(map.containsKey(2));
    }
    
    @Test
    public void testRemove() {
        IntObjectMap<String> map = new IntObjectMap<>();
        
        // Add some values
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        
        // Remove a value in the middle
        map.remove(2);
        
        // Verify the value was removed
        assertNull(map.get(2));
        assertFalse(map.containsKey(2));
        
        // Verify other values are still there
        assertEquals("one", map.get(1));
        assertEquals("three", map.get(3));
        
        // Remove a value at the beginning
        map.remove(1);
        assertNull(map.get(1));
        assertEquals("three", map.get(3));
        
        // Remove a value at the end
        map.remove(3);
        assertNull(map.get(3));
        
        // Remove a non-existent key (should not throw exception)
        map.remove(4);
    }
    
    @Test
    public void testGrow() {
        IntObjectMap<Integer> map = new IntObjectMap<>();
        
        // Add more elements than the default capacity (16)
        for (int i = 0; i < 20; i++) {
            map.put(i, i * 10);
        }
        
        // Verify all elements are still accessible
        for (int i = 0; i < 20; i++) {
            assertEquals(i * 10, map.get(i));
        }
    }
    
    @Test
    public void testNullValues() {
        IntObjectMap<String> map = new IntObjectMap<>();
        
        // Test putting and getting null values
        map.put(1, null);
        assertNull(map.get(1));
        assertTrue(map.containsKey(1));
        
        // Test overwriting null with a value
        map.put(1, "one");
        assertEquals("one", map.get(1));
        
        // Test overwriting a value with null
        map.put(1, null);
        assertNull(map.get(1));
        assertTrue(map.containsKey(1));
    }
    
    @Test
    public void testNegativeKeys() {
        IntObjectMap<String> map = new IntObjectMap<>();
        
        // Test with negative keys
        map.put(-1, "minus one");
        map.put(-2, "minus two");
        
        assertEquals("minus one", map.get(-1));
        assertEquals("minus two", map.get(-2));
        
        // Test removing negative keys
        map.remove(-1);
        assertNull(map.get(-1));
        assertEquals("minus two", map.get(-2));
    }
    
    @Test
    public void testZeroKey() {
        IntObjectMap<String> map = new IntObjectMap<>();
        
        // Test with zero key
        map.put(0, "zero");
        assertEquals("zero", map.get(0));
        assertTrue(map.containsKey(0));
        
        // Test removing zero key
        map.remove(0);
        assertNull(map.get(0));
        assertFalse(map.containsKey(0));
    }
    
    @Test
    public void testLargeKeys() {
        IntObjectMap<String> map = new IntObjectMap<>();
        
        // Test with large integer keys
        map.put(Integer.MAX_VALUE, "max");
        map.put(Integer.MIN_VALUE, "min");
        
        assertEquals("max", map.get(Integer.MAX_VALUE));
        assertEquals("min", map.get(Integer.MIN_VALUE));
    }
}