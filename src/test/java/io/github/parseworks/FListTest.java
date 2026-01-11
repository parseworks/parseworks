package io.github.parseworks;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FListTest {


    @Test
    public void testConstructors() {
        // Default constructor
        FList<Integer> empty = new FList<>();
        assertEquals(0, empty.size());

        // Head and tail constructor
        FList<Integer> tail = new FList<>(Arrays.asList(2, 3));
        FList<Integer> list = new FList<>(1, tail);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));

        // Collection constructor
        FList<String> fromCollection = new FList<>(Arrays.asList("a", "b", "c"));
        assertEquals(3, fromCollection.size());
        assertEquals("a", fromCollection.get(0));
    }

    @Test
    public void testStaticFactoryMethods() {
        // Test of() method
        FList<Integer> list = FList.of(1, 2, 3, 4);
        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(4, list.get(3));

        // Test joinChars() method
        FList<Character> chars = FList.of('h', 'e', 'l', 'l', 'o');
        String joined = FList.join(chars);
        assertEquals("hello", joined);
    }

    @Test
    public void testAppend() {
        FList<Integer> list = new FList<>(Arrays.asList(1, 2, 3));
        FList<Integer> appended = list.append(4);
        assertEquals(4, appended.size());
        assertEquals(4, appended.get(3));

        // Original list should be unchanged
        assertEquals(3, list.size());
    }

    @Test
    public void testAppendAll() {
        FList<Integer> list = new FList<>(Arrays.asList(1, 2));
        FList<Integer> appended = list.appendAll(Arrays.asList(3, 4, 5));
        assertEquals(5, appended.size());
        assertEquals(3, appended.get(2));
        assertEquals(5, appended.get(4));
    }

    @Test
    public void testReverse() {
        FList<Integer> list = new FList<>(Arrays.asList(1, 2, 3, 4));
        FList<Integer> reversed = list.reverse();
        assertEquals(4, reversed.size());
        assertEquals(4, reversed.get(0));
        assertEquals(1, reversed.get(3));
    }

    @Test
    public void testHead() {
        FList<String> list = new FList<>(Arrays.asList("a", "b", "c"));
        assertEquals("a", list.head());
    }

    @Test
    public void testReplaceRemoveMethods() {
        FList<String> list = new FList<>(Arrays.asList("a", "b", "c", "d"));

        // Test replace
        FList<String> replaced = list.replace(1, "x");
        assertEquals(4, replaced.size());
        assertEquals("x", replaced.get(1));

        // Test removeElement
        FList<String> removedElement = list.removeElement("c");
        assertEquals(3, removedElement.size());
        assertEquals("d", removedElement.get(2));

        // Test removeAt
        FList<String> removedAt = list.removeAt(0);
        assertEquals(3, removedAt.size());
        assertEquals("b", removedAt.get(0));
    }

    @Test
    public void testReduceMethods() {
        FList<Integer> list = new FList<>(Arrays.asList(1, 2, 3, 4));

        // Test reduceLeft
        assertEquals(Integer.valueOf(10), list.reduceLeft(Integer::sum).get());

        // Test reduceRight
        assertEquals(Integer.valueOf(10), list.reduceRight(Integer::sum).get());

        // Test the other foldLeft overload
        Integer result = list.foldLeft(1, (acc, val) -> acc * val);
        assertEquals(24, result);
    }

    @Test
    public void testEmptyListOperations() {
        FList<Integer> empty = new FList<>();

        // Test operations on empty list
        assertEquals(0, empty.size());
        assertEquals(empty, empty.filter(x -> true));
        assertEquals(0, empty.map(String::valueOf).size());
        assertEquals(Integer.valueOf(5), empty.foldLeft(5, Integer::sum));
    }


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

    @Test
    public void testImmutability() {
        FList<Integer> list = FList.of(1, 2, 3);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            list.add(4);
        }, "FList should be immutable and throw UnsupportedOperationException on add()");
    }
}