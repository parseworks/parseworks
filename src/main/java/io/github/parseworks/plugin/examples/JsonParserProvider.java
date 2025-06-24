package io.github.parseworks.plugin.examples;

import io.github.parseworks.Parser;
import io.github.parseworks.plugin.AbstractParserProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap;

import static io.github.parseworks.Combinators.*;
import static io.github.parseworks.TextParsers.trim;

/**
 * Example parser provider for JSON parsing.
 * <p>
 * This class demonstrates how to implement a parser provider that provides
 * parsers for JSON data. It extends {@link AbstractParserProvider} and registers
 * parsers for JSON objects, arrays, strings, numbers, booleans, and null values.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Get the manager instance
 * ParserPluginManager manager = ParserPluginManager.getInstance();
 * 
 * // Register the plugin
 * manager.registerPlugin(new JsonParserProvider());
 * 
 * // Get a parser by name
 * Optional<Parser<Character, Object>> jsonParser = manager.getParser("jsonParser");
 * 
 * // Use the parser
 * jsonParser.ifPresent(parser -> {
 *     Object result = parser.parse("{\"name\":\"John\",\"age\":30}").get();
 *     // Process the result...
 * });
 * }</pre>
 *
 * @author parseWorks
 * @see AbstractParserProvider
 */
public class JsonParserProvider extends AbstractParserProvider {

    @Override
    public String getId() {
        return "io.github.parseworks.json";
    }

    @Override
    public String getName() {
        return "JSON Parser Provider";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void initialize() {
        // Create and register JSON parsers
        Parser<Character, Object> jsonParser = createJsonParser();
        registerParser("jsonParser", jsonParser);
    }

    /**
     * Creates a parser for JSON data.
     * <p>
     * This method creates a parser that can parse JSON objects, arrays, strings,
     * numbers, booleans, and null values. The parser returns the parsed JSON data
     * as Java objects (Map, List, String, Double, Boolean, or null).
     *
     * @return a parser for JSON data
     */
    private Parser<Character, Object> createJsonParser() {
        // This is a simplified JSON parser for demonstration purposes.
        // A real JSON parser would be more complex and handle all JSON syntax.

        // Create references for recursive parsers
        Parser<Character, Object> jsonValue = Parser.ref();
        Parser<Character, Map<String, Object>> jsonObject = Parser.ref();
        Parser<Character, List<Object>> jsonArray = Parser.ref();

        // Parser for JSON strings
        Parser<Character, String> jsonString = 
            chr('"').skipThen(
                regex("[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*").thenSkip(chr('"'))
            ).map(s -> s.replace("\\\"", "\"")
                      .replace("\\\\", "\\")
                      .replace("\\n", "\n")
                      .replace("\\r", "\r")
                      .replace("\\t", "\t")
                      .replace("\\/", "/")
                      .replace("\\b", "\b")
                      .replace("\\f", "\f"));

        // Parser for JSON numbers
        Parser<Character, Double> jsonNumber = 
            regex("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?")
                .map(Double::parseDouble);

        // Parser for JSON booleans
        Parser<Character, Boolean> jsonTrue = string("true").as(Boolean.TRUE);
        Parser<Character, Boolean> jsonFalse = string("false").as(Boolean.FALSE);
        Parser<Character, Boolean> jsonBoolean = oneOf(jsonTrue, jsonFalse);

        // Parser for JSON null
        Parser<Character, Object> jsonNull = string("null").as(null);

        // Parser for JSON arrays
        jsonArray.set(
            chr('[').skipThen(
                trim(jsonValue).zeroOrManySeparatedBy(trim(chr(',')))
            ).thenSkip(chr(']'))
            .map(flist -> {
                // FList already extends LinkedList, so it's already a List
                return flist;
            })
        );

        // Parser for JSON objects
        jsonObject.set(
            chr('{').skipThen(
                trim(jsonString)
                    .thenSkip(trim(chr(':')))
                    .then(trim(jsonValue))
                    .map(key -> value -> new AbstractMap.SimpleEntry<>(key, value))
                    .zeroOrManySeparatedBy(trim(chr(',')))
            ).thenSkip(chr('}'))
            .map(entries -> {
                Map<String, Object> map = new HashMap<>();
                for (AbstractMap.SimpleEntry<String, Object> entry : entries) {
                    map.put(entry.getKey(), entry.getValue());
                }
                return map;
            })
        );

        // Set the JSON value parser to handle all JSON value types
        // Cast each parser to Parser<Character, Object> to ensure type compatibility
        Parser<Character, Object> stringParser = (Parser<Character, Object>) (Parser<?, ?>) jsonString;
        Parser<Character, Object> numberParser = (Parser<Character, Object>) (Parser<?, ?>) jsonNumber;
        Parser<Character, Object> booleanParser = (Parser<Character, Object>) (Parser<?, ?>) jsonBoolean;
        Parser<Character, Object> objectParser = (Parser<Character, Object>) (Parser<?, ?>) jsonObject;
        Parser<Character, Object> arrayParser = (Parser<Character, Object>) (Parser<?, ?>) jsonArray;

        // Set the JSON value parser to handle all JSON value types
        jsonValue.set(trim(oneOf(
            stringParser,
            numberParser,
            booleanParser,
            jsonNull,
            objectParser,
            arrayParser
        )));

        // Return the JSON value parser
        return jsonValue;
    }
}
