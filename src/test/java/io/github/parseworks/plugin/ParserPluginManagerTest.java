package io.github.parseworks.plugin;

import io.github.parseworks.Parser;
import io.github.parseworks.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link ParserPluginManager} class.
 */
public class ParserPluginManagerTest {

    private ParserPluginManager manager;
    private TestParserProvider testProvider;

    @BeforeEach
    public void setUp() {
        manager = ParserPluginManager.getInstance();
        testProvider = new TestParserProvider();
        manager.registerPlugin(testProvider);
    }

    @AfterEach
    public void tearDown() {
        manager.unregisterPlugin(testProvider.getId());
    }

    @Test
    public void testGetParser() {
        Optional<Parser<Character, String>> parser = manager.getParser("uppercaseParser");
        assertTrue(parser.isPresent());

        Result<Character, String> result = parser.get().parse("hello");
        assertTrue(result.isSuccess());
        assertEquals("HELLO", result.get());
    }

    @Test
    public void testGetParsersOfType() {
        Collection<Parser<Character, String>> parsers = manager.getParsersOfType(Character.class, String.class);
        assertFalse(parsers.isEmpty());

        Parser<Character, String> parser = parsers.iterator().next();
        Result<Character, String> result = parser.parse("hello");
        assertTrue(result.isSuccess());
        assertEquals("HELLO", result.get());
    }

    @Test
    public void testGetAllParsers() {
        Map<String, Parser<?, ?>> parsers = manager.getAllParsers();
        assertFalse(parsers.isEmpty());
        assertTrue(parsers.containsKey("uppercaseParser"));
    }

    @Test
    public void testUnregisterPlugin() {
        assertTrue(manager.unregisterPlugin(testProvider.getId()));
        Optional<Parser<String, String>> parser = manager.getParser("uppercaseParser");
        assertFalse(parser.isPresent());
    }

    /**
     * A test implementation of {@link ParserProvider} that provides a simple parser
     * that converts strings to uppercase.
     */
    private static class TestParserProvider extends AbstractParserProvider {

        public TestParserProvider() {
            // The initialize() method will be called by ParserPluginRegistry.registerPlugin()
        }

        @Override
        public String getId() {
            return "io.github.parseworks.test";
        }

        @Override
        public String getName() {
            return "Test Parser Provider";
        }

        @Override
        public String getVersion() {
            return "1.0.0";
        }

        @Override
        public void initialize() {
            registerParser("uppercaseParser", createUppercaseParser());
        }

        private Parser<Character, String> createUppercaseParser() {
            return new Parser<>(in -> {
                StringBuilder sb = new StringBuilder();

                // Read characters until end of input
                while (!in.isEof()) {
                    sb.append(in.current());
                    in = in.next();
                }

                // Convert to uppercase
                String result = sb.toString().toUpperCase();

                return Result.success(in, result);
            });
        }
    }
}
