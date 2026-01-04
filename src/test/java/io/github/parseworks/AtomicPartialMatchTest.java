package io.github.parseworks;

import io.github.parseworks.impl.result.PartialMatch;
import org.junit.jupiter.api.Test;

import static io.github.parseworks.parsers.Combinators.attempt;
import static io.github.parseworks.parsers.Lexical.string;
import static org.junit.jupiter.api.Assertions.*;

public class AtomicPartialMatchTest {

    @Test
    public void testAttemptWithPartialMatch() {
        // string("abc") will produce a PartialMatch on input "abx"
        Parser<Character, String> p = attempt(string("abc"));
        Input<Character> input = Input.of("abx");
        
        Result<Character, String> result = p.apply(input);
        
        assertFalse(result.matches());
        assertEquals(ResultType.PARTIAL, result.type(), "Attempt parser should return a partial match");
        assertEquals(0, result.input().position(), "Attempt parser should backtrack to the start");
        assertTrue(result instanceof PartialMatch, "Result should be an instance of PartialMatch");
    }
}
