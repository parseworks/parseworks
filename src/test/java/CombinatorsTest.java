import io.github.parseworks.Combinators;
import io.github.parseworks.Input;
import io.github.parseworks.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CombinatorsTest {

    @Test
    public void eofSucceedsOnEmptyInput() {
        final Input<Character> input = Input.of("");
        final Parser<Character, Void> parser = Combinators.eof();

        var result = parser.parse(input);
        assertTrue(result.isSuccess());
    }
}
