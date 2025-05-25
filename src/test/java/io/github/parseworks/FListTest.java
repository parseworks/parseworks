package io.github.parseworks;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FListTest {

    @Test
    public void testPush() {
        FList<Integer> list = new FList<>();
        list = list.prepend(1);
        assertEquals(1, list.size());
        assertEquals(1, list.head());
    }

    @Test
    public void testTail() {
        FList<Integer> list = new FList<>(1, new FList<>(Arrays.asList(2, 3, 4)));
        FList<Integer> tail = list.tail();
        assertEquals(3, tail.size());
        assertEquals(2, tail.head());
    }

    @Test
    public void testSubList() {
        FList<Integer> list = new FList<>(Arrays.asList(1, 2, 3, 4, 5));
        FList<Integer> subList = list.subList(1, 4);
        assertEquals(3, subList.size());
        assertEquals(2, subList.head());
    }

    @Test
    public void testMap() {
        FList<Integer> list = new FList<>(Arrays.asList(1, 2, 3));
        FList<String> mapped = list.map(Object::toString);
        assertEquals(3, mapped.size());
        assertEquals("1", mapped.head());
    }

    @Test
    public void testFilter() {
        FList<Integer> list = new FList<>(Arrays.asList(1, 2, 3, 4, 5));
        FList<Integer> filtered = list.filter(n -> n % 2 == 0);
        assertEquals(2, filtered.size());
        assertEquals(2, filtered.head());
    }

    @Test
    public void testFoldLeft() {
        FList<Integer> list = new FList<>(Arrays.asList(1, 2, 3, 4));
        Integer sum = list.foldLeft(0, Integer::sum);
        assertEquals(10, sum);
    }

    @Test
    public void testFoldRight() {
        FList<Integer> list = new FList<>(Arrays.asList(1, 2, 3, 4));
        Integer sum = list.foldRight(0, Integer::sum);
        assertEquals(10, sum);
    }
}