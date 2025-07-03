package io.github.parseworks.html;

import io.github.parseworks.*;

import java.util.HashMap;
import java.util.Map;

import static io.github.parseworks.Combinators.*;

/**
 * SimpleHtmlParser is a simplified parser for HTML/XML documents using parseworks.
 * This is a conversion of the JavaCC-based TagParser.
 */
public class SimpleHtmlParser {

    // Element types
    public static class Element {

        private String data;

        public String getData() {
            return data;
        }
    }

    public static class TextData extends Element {
        private final String text;

        public TextData(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return "TextData[" + text + "]";
        }
    }

    public static class StartTag extends Element {
        private final String name;
        private final Map<String, String> attributes;

        public StartTag(String name, Map<String, String> attributes) {
            this.name = name;
            this.attributes = attributes != null ? attributes : new HashMap<>();
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        @Override
        public String toString() {
            return "StartTag[" + name + ", " + attributes + "]";
        }
    }

    public static class Declaration extends Element {
        private final String name;
        private final Map<String, String> attributes;

        public Declaration(String name, Map<String, String> attributes) {
            this.name = name;
            this.attributes = attributes != null ? attributes : new HashMap<>();
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public String getAttributeValue(String name) {
            return attributes.get(name);
        }

        @Override
        public String toString() {
            return "Declaration[" + name + ", " + attributes + "]";
        }
    }

    public static class EndTag extends Element {
        private final String name;

        public EndTag(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "EndTag[" + name + "]";
        }
    }

    // Token parsers
    private static final Parser<Character, Character> SPACE = 
        chr(c -> c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f');

    private static final Parser<Character, String> WHITESPACE = 
        SPACE.zeroOrMany().map(chars -> {
            StringBuilder sb = new StringBuilder();
            for (Character c : chars) {
                sb.append(c);
            }
            return sb.toString();
        });

    // Helper function to convert FList<Character> to String
    private static String charsToString(FList<Character> chars) {
        StringBuilder sb = new StringBuilder();
        for (Character c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static final Parser<Character, String> NAME_IDENTIFIER = 
        chr(c -> c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != '\f' && c != '/' && c != '>')
            .many()
            .map(SimpleHtmlParser::charsToString);

    private static final Parser<Character, String> ATTR_IDENTIFIER = 
        chr(c -> c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != '\f' && c != '=' && c != '/' && c != '>')
            .many()
            .map(SimpleHtmlParser::charsToString);

    private static final Parser<Character, String> QUOTED_STRING = 
        oneOf(
            chr('\'').skipThen(chr(c -> c != '\'').zeroOrMany()).thenSkip(chr('\'')),
            chr('"').skipThen(chr(c -> c != '"').zeroOrMany()).thenSkip(chr('"'))
        ).map(SimpleHtmlParser::charsToString);

    private static final Parser<Character,Map<String,String>> COMMENT_BODY = any(Character.class).zeroOrManyUntil(string("--")).map(
            data ->{
                Map<String,String> result = new HashMap<>();
                result.put("data",charsToString(data));
                return result;
            }
    );

    private static final Parser<Character, Character> TAG_START =
        chr('<').peek(not(oneOf('!','#','/')));


    // Element parsers
    public static final Parser<Character, Element> element = Parser.ref();
    private static final Parser<Character, Element> tag = Parser.ref();
    private static final Parser<Character, Element> endTag = Parser.ref();
    private static final Parser<Character, Element> declarationTag = Parser.ref();
    private static final Parser<Character, Map<String, String>> attributeList = Parser.ref();
    private static final Parser<Character, Map<String, String>> attribute = Parser.ref();

    static {
        // Initialize recursive parsers

        // Attribute parser - parse a single attribute (name=value or just name)
        attribute.set(
            WHITESPACE.skipThen(
                ATTR_IDENTIFIER.then(
                    WHITESPACE.skipThen(
                        chr('=').skipThen(
                            WHITESPACE.skipThen(
                                oneOf(
                                    QUOTED_STRING,
                                    chr(c -> c != '>' && c != '"' && c != '\'' && c != ' ' && c != '\t' && c != '\n' && c != '\r')
                                        .many()
                                        .map(SimpleHtmlParser::charsToString)
                                )
                            )
                        ).optional()
                    )
                ).map(name -> valueOpt -> {
                    Map<String, String> result = new HashMap<>();
                    if (valueOpt.isPresent()) {
                        result.put(name, valueOpt.orElse(""));
                    } else {
                        result.put(name, "");
                    }
                    return result;
                })
            )
        );

        // Attribute list parser - parse multiple attributes
        attributeList.set(
            attribute.zeroOrMany().map(attrMaps -> {
                Map<String, String> result = new HashMap<>();
                for (Map<String, String> map : attrMaps) {
                    result.putAll(map);
                }
                return result;
            })
        );

        // Tag parser - parse a start tag with optional attributes
        tag.set(
            TAG_START.skipThen(
                NAME_IDENTIFIER.then(
                    attributeList.thenSkip(
                        oneOf(
                            string(">"),
                            string("/>")
                        )
                    )
                ).map(name -> attrs -> new StartTag(name, attrs))
            )
        );

        // End tag parser - parse an end tag
        endTag.set(
            string("</").skipThen(
                NAME_IDENTIFIER.thenSkip(
                    string(">")
                )
            ).map(EndTag::new)
        );

        // Comment parser - parse an HTML comment
        declarationTag.set(
            string("<!").skipThen(
                NAME_IDENTIFIER.then(
                    COMMENT_BODY
                        .thenSkip(chr('>'))
                        .or(attributeList.thenSkip(
                            oneOf(
                                string(">"),
                                string("/>")
                            )
                        )
                    )
                ).map(name -> attributeList ->
                        new Declaration(name,attributeList)
                )
            )
        );

        // Raw text parser - parse text content
        Parser<Character, Element> rawText = 
            chr(c -> c != '<')
                .many()
                .map(chars -> new TextData(charsToString(chars)));

        // Element parser - parse any HTML element
        element.set(
            oneOf(
                declarationTag,
                tag,
                endTag,
                rawText
            )
        );
    }

    /**
     * Parse an HTML document.
     * 
     * @param input the HTML document to parse
     * @return the result of parsing
     */
    public static Result<Character, Element> parse(String input) {
        return element.parse(Input.of(input));
    }

    /**
     * Parse an HTML document and return all elements.
     * 
     * @param input the HTML document to parse
     * @return the list of elements
     */
    public static FList<Element> parseAll(String input) {
        FList<Element> elements = new FList<>();
        Input<Character> currentInput = Input.of(input);

        while (!currentInput.isEof()) {
            Result<Character, Element> result = element.parse(currentInput);
            if (result.isError()) {
                // Skip one character and try again
                currentInput = currentInput.next();
                continue;
            }

            elements = elements.append(result.get());
            currentInput = result.input();
        }

        return elements;
    }
}
