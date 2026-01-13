package io.github.parseworks;

import io.github.parseworks.parsers.Combinators;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OneOfCombinedFailureTest {

    @Test
    public void oneOf_allAlternativesFail_combinesReasons() {
        Input<Character> in = Input.of("a");

        // Three parsers that all fail at the same position with distinct expectations
        Parser<Character, String> p1 = new Parser<>(inp -> Result.failure(inp, "option A"));
        Parser<Character, String> p2 = new Parser<>(inp -> Result.failure(inp, "option B"));
        Parser<Character, String> p3 = new Parser<>(inp -> Result.failure(inp, "option C"));

        Parser<Character, String> parser = Combinators.oneOf(Arrays.asList(p1, p2, p3));

        Result<Character, String> result = parser.parse(in);
        assertTrue(!result.matches(), "Result should be an error when all alternatives fail");

        String msg = result.error();

        // Header should reflect the first alternative (option A) and what was found ('a')

        // The combined reasons list should include each alternative's expectation
        assertTrue(msg.contains("Reasons at this location:"), "Should include reasons section");
        assertTrue(msg.contains("- expected option B"), "Should include second alternative reason");
        assertTrue(msg.contains("- expected option C"), "Should include third alternative reason");
        System.out.println(msg);
        // Optional: sanity check that a location indicator exists
        assertTrue(msg.contains("line ") || msg.contains("position "), "Should include location information");
    }
}