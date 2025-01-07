import io.github.parseworks.Input;
import io.github.parseworks.Parser;
import io.github.parseworks.Text;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TextTest {

    @Test
    public void testAlphaNum() {
        Parser<Character, Character> parser = Text.alphaNum();
        assertTrue(parser.parse(Input.of("a")).isSuccess());
        assertTrue(parser.parse(Input.of("1")).isSuccess());
        assertFalse(parser.parse(Input.of("!")).isSuccess());
    }

    @Test
    public void testSpace() {
        Parser<Character, String> parser = Text.space();
        assertTrue(parser.parse(Input.of(" ")).isSuccess());
        assertTrue(parser.parse(Input.of("\t")).isSuccess());
        assertFalse(parser.parse(Input.of("a")).isSuccess());
    }

    @Test
    public void testWord() {
        Parser<Character, String> parser = Text.word();
        assertTrue(parser.parse(Input.of("hello")).isSuccess());
        assertFalse(parser.parse(Input.of("hello1")).isSuccess());
        assertFalse(parser.parse(Input.of("123")).isSuccess());
    }

    @Test
    public void testInteger() {
        Parser<Character, Integer> parser = Text.integer();
        assertEquals(123, parser.parse(Input.of("123")).getOrThrow());
        assertEquals(-123, parser.parse(Input.of("-123")).getOrThrow());
        assertEquals(123, parser.parse(Input.of("+123")).getOrThrow());
        assertFalse(parser.parse(Input.of("abc")).isSuccess());
    }
}