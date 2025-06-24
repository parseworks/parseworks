package io.github.parseworks.plugin.examples;

import io.github.parseworks.Parser;
import io.github.parseworks.Result;
import io.github.parseworks.plugin.ParserPluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link JsonParserProvider} class.
 */
public class JsonParserProviderTest {

    private ParserPluginManager manager;
    private JsonParserProvider jsonProvider;

    @BeforeEach
    public void setUp() {
        manager = ParserPluginManager.getInstance();
        jsonProvider = new JsonParserProvider();
        manager.registerPlugin(jsonProvider);
    }

    @AfterEach
    public void tearDown() {
        manager.unregisterPlugin(jsonProvider.getId());
    }

    @Test
    public void testGetJsonParser() {
        Optional<Parser<Character, Object>> parser = manager.getParser("jsonParser");
        assertTrue(parser.isPresent(), "JSON parser should be available");
    }

    @Test
    public void testParseJsonString() {
        Optional<Parser<Character, Object>> parser = manager.getParser("jsonParser");
        assertTrue(parser.isPresent(), "JSON parser should be available");

        Result<Character, Object> result = parser.get().parse("\"hello\"");
        assertTrue(result.isSuccess(), "Parsing JSON string should succeed");
        assertEquals("hello", result.get(), "Parsed value should be the string without quotes");
    }

    @Test
    public void testParseJsonNumber() {
        Optional<Parser<Character, Object>> parser = manager.getParser("jsonParser");
        assertTrue(parser.isPresent(), "JSON parser should be available");

        Result<Character, Object> result = parser.get().parse("42.5");
        assertTrue(result.isSuccess(), "Parsing JSON number should succeed");
        assertEquals(42.5, result.get(), "Parsed value should be the number");
    }

    @Test
    public void testParseJsonBoolean() {
        Optional<Parser<Character, Object>> parser = manager.getParser("jsonParser");
        assertTrue(parser.isPresent(), "JSON parser should be available");

        Result<Character, Object> result = parser.get().parse("true");
        assertTrue(result.isSuccess(), "Parsing JSON boolean should succeed");
        assertEquals(Boolean.TRUE, result.get(), "Parsed value should be true");

        result = parser.get().parse("false");
        assertTrue(result.isSuccess(), "Parsing JSON boolean should succeed");
        assertEquals(Boolean.FALSE, result.get(), "Parsed value should be false");
    }

    @Test
    public void testParseJsonNull() {
        Optional<Parser<Character, Object>> parser = manager.getParser("jsonParser");
        assertTrue(parser.isPresent(), "JSON parser should be available");

        Result<Character, Object> result = parser.get().parse("null");
        assertTrue(result.isSuccess(), "Parsing JSON null should succeed");
        assertNull(result.get(), "Parsed value should be null");
    }
}
