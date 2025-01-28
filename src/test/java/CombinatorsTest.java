import io.github.parseworks.Combinators;
import io.github.parseworks.Input;
import io.github.parseworks.Parser;
import io.github.parseworks.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CombinatorsTest {

    @Test
    public void eofSucceedsOnEmptyInput() {
        final Input<Character> input = Input.of("");
        final Parser<Character, Void> parser = Combinators.eof();

        var result = parser.parse(input);
        assertTrue(result.isSuccess());
    }

    @Test
    public void anySucceedsOnNonEmptyInput() {
        final Input<Character> input = Input.of("abc");
        final Parser<Character, Character> parser = Combinators.any(Character.class);

        Result<Character, Character> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertTrue(result.getOrThrow() == 'a');
    }

    @Test
    public void anyFailsOnEmptyInput() {
        final Input<Character> input = Input.of("");
        final Parser<Character, Character> parser = Combinators.any(Character.class);

        Result<Character, Character> result = parser.parse(input);
        assertFalse(result.isSuccess());
    }
}
