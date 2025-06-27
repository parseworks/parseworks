package io.github.parseworks.impl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Pair record.
 * These tests verify that the Pair record correctly stores and retrieves two elements.
 */
public class PairTest {

    @Test
    public void testPairCreation() {
        Pair<String, Integer> pair = new Pair<>("test", 42);
        
        // Test accessors
        assertEquals("test", pair.left());
        assertEquals(42, pair.right());
    }
    
    @Test
    public void testEquality() {
        Pair<String, Integer> pair1 = new Pair<>("test", 42);
        Pair<String, Integer> pair2 = new Pair<>("test", 42);
        Pair<String, Integer> pair3 = new Pair<>("different", 42);
        Pair<String, Integer> pair4 = new Pair<>("test", 43);
        
        // Test equals method
        assertEquals(pair1, pair2);
        assertNotEquals(pair1, pair3);
        assertNotEquals(pair1, pair4);
        
        // Test hashCode method
        assertEquals(pair1.hashCode(), pair2.hashCode());
    }
    
    @Test
    public void testToString() {
        Pair<String, Integer> pair = new Pair<>("test", 42);
        
        // Test toString method
        String toString = pair.toString();
        assertTrue(toString.contains("test"));
        assertTrue(toString.contains("42"));
    }
    
    @Test
    public void testWithNullValues() {
        Pair<String, Integer> pair1 = new Pair<>(null, 42);
        Pair<String, Integer> pair2 = new Pair<>("test", null);
        Pair<String, Integer> pair3 = new Pair<>(null, null);
        
        // Test accessors with null values
        assertNull(pair1.left());
        assertEquals(42, pair1.right());
        
        assertEquals("test", pair2.left());
        assertNull(pair2.right());
        
        assertNull(pair3.left());
        assertNull(pair3.right());
        
        // Test equality with null values
        Pair<String, Integer> pair1Copy = new Pair<>(null, 42);
        assertEquals(pair1, pair1Copy);
        assertNotEquals(pair1, pair2);
    }
}