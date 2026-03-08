package io.github.parseworks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CharPredicateTest {

    @Test
    public void testIs() {
        CharPredicate p = CharPredicate.is('a');
        assertTrue(p.test('a'));
        assertFalse(p.test('b'));
    }

    @Test
    public void testIsNot() {
        CharPredicate p = CharPredicate.isNot('a');
        assertFalse(p.test('a'));
        assertTrue(p.test('b'));
    }

    @Test
    public void testRange() {
        CharPredicate p = CharPredicate.range('a', 'c');
        assertTrue(p.test('a'));
        assertTrue(p.test('b'));
        assertTrue(p.test('c'));
        assertFalse(p.test('d'));
        assertFalse(p.test('`'));
    }

    @Test
    public void testAnyOf() {
        CharPredicate p = CharPredicate.anyOf("abc");
        assertTrue(p.test('a'));
        assertTrue(p.test('b'));
        assertTrue(p.test('c'));
        assertFalse(p.test('d'));
    }

    @Test
    public void testNoneOf() {
        CharPredicate p = CharPredicate.noneOf("abc");
        assertFalse(p.test('a'));
        assertFalse(p.test('b'));
        assertFalse(p.test('c'));
        assertTrue(p.test('d'));
    }


    @Test
    public void testAnd() {
        CharPredicate p1 = CharPredicate.range('a', 'z');
        CharPredicate p2 = CharPredicate.anyOf("aeiou");
        CharPredicate p = p1.and(p2);
        assertTrue(p.test('a'));
        assertFalse(p.test('b'));
        assertFalse(p.test('A'));
    }

    @Test
    public void testOr() {
        CharPredicate p1 = CharPredicate.digit;
        CharPredicate p2 = CharPredicate.anyOf("abc");
        CharPredicate p = p1.or(p2);
        assertTrue(p.test('0'));
        assertTrue(p.test('a'));
        assertFalse(p.test('d'));
    }

}
