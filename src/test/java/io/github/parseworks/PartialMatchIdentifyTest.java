package io.github.parseworks;

import io.github.parseworks.parsers.Lexical;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PartialMatchIdentifyTest {

    @Test
    public void testStringPartialMatch() {
        Parser<Character, String> abc = Lexical.string("abc");
        Input<Character> input = Input.of("abd"); // matches 'ab', fails on 'c' vs 'd'
        
        Result<Character, String> result = abc.apply(input);
        
        // If it returns PARTIAL, it means it started matching but failed.
        // This is useful for debugging and for 'oneOf' to know it shouldn't try other options if it's a hard failure.
        assertEquals(ResultType.PARTIAL, result.type(), "Lexical.string should return PARTIAL on partial match");
    }

    @Test
    public void testSequencePartialMatch() {
        Parser<Character, Character> a = Lexical.chr('a');
        Parser<Character, Character> b = Lexical.chr('b');
        Parser<Character, Character> c = Lexical.chr('c');
        
        Parser<Character, Character> abc = a.skipThen(b).skipThen(c);
        Input<Character> input = Input.of("abd");
        
        Result<Character, Character> result = abc.apply(input);
        
        // a matches, b matches, c fails. 
        // a.skipThen(b) returns Match at pos 2.
        // (a.skipThen(b)).skipThen(c) calls c.apply(pos 2), which returns NoMatch at pos 2.
        assertEquals(ResultType.PARTIAL, result.type(), "Sequence should return PARTIAL if a later element fails");
    }
    
    @Test
    public void testRepeatPartialMatch() {
        Parser<Character, Character> a = Lexical.chr('a');
        Parser<Character, List<Character>> a3 = a.repeat(3);
        Input<Character> input = Input.of("aab");
        
        Result<Character, List<Character>> result = a3.apply(input);
        
        // Matches 'a' twice, fails on third 'a'.
        assertEquals(ResultType.PARTIAL, result.type(), "Repeat should return PARTIAL if it met min requirement but failed subsequently");
    }
}
