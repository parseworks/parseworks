package io.github.parseworks.html;

import io.github.parseworks.Input;
import io.github.parseworks.Lists;
import io.github.parseworks.Parser;
import io.github.parseworks.Result;
import io.github.parseworks.parsers.Lexical;

import java.util.*;

import static io.github.parseworks.parsers.Combinators.*;

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
        Lexical.chr(c -> c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f');

    private static final Parser<Character, Void> WHITESPACE =
        SPACE.zeroOrMore().map(chars -> null);

    // Helper function to convert List<Character> to String
    private static String charsToString(List<Character> chars) {
        return Lists.join(chars);
    }

    private static final Parser<Character, String> NAME_IDENTIFIER = 
        Lexical.chr(c -> c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != '\f' && c != '/' && c != '>')
            .oneOrMore()
            .map(SimpleHtmlParser::charsToString);

    private static final Parser<Character, String> ATTR_IDENTIFIER = 
        Lexical.chr(c -> c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != '\f' && c != '=' && c != '/' && c != '>')
            .oneOrMore()
            .map(SimpleHtmlParser::charsToString);

    private static final Parser<Character, String> QUOTED_STRING = 
        oneOf(
            Lexical.chr('\'').skipThen(Lexical.chr(c -> c != '\'').zeroOrMore()).thenSkip(Lexical.chr('\'')),
            Lexical.chr('"').skipThen(Lexical.chr(c -> c != '"').zeroOrMore()).thenSkip(Lexical.chr('"'))
        ).map(SimpleHtmlParser::charsToString);

    private static final Parser<Character,Map<String,String>> COMMENT_BODY = any(Character.class).zeroOrMoreUntil(Lexical.string("--")).map(
            data ->{
                Map<String,String> result = new HashMap<>();
                result.put("data",charsToString(data));
                return result;
            }
    );

    private static final Parser<Character, Character> TAG_START =
        Lexical.chr('<').peek(not(oneOf('!','#','/')));

    record KV(String k, String v){};


    // Element parsers
    public static final Parser<Character, Element> element = Parser.ref();
    private static final Parser<Character, Element> tagBody = Parser.ref();
    private static final Parser<Character, Element> endTagBody = Parser.ref();
    private static final Parser<Character, Element> declarationTagBody = Parser.ref();
    private static final Parser<Character, Map<String, String>> attributeList = Parser.ref();
    private static final Parser<Character, KV> attribute = Parser.ref();



    static {
        // Initialize recursive parsers

        // Attribute parser - parse a single attribute (name=value or just name)
        attribute.set(
            WHITESPACE.skipThen(
                ATTR_IDENTIFIER.then(
                    WHITESPACE.skipThen(
                        Lexical.chr('=')
                            .skipThen(WHITESPACE)
                            .skipThen(oneOf(
                                QUOTED_STRING,
                                Lexical.chr(c -> c != '>' && c != '"' && c != '\'' && c != ' ' && c != '\t' && c != '\n' && c != '\r')
                                    .oneOrMore() // avoid empty attr value
                                    .map(Lists::join)
                            ))
                            .optional()
                    )
                ).map(name -> valOpt -> new KV(name, valOpt.orElse("")))
            )
        );

        // Attribute list parser - parse multiple attributes
        attributeList.set(
            attribute.zeroOrMore().map(kvs -> {
                Map<String,String> m = new HashMap<>(Math.max(4, kvs.size()*2));
                for (KV kv : kvs) m.put(kv.k, kv.v);
                return m;
            })
        );

        // Tag parser - parse a start tag with optional attributes
        tagBody.set(
            NAME_IDENTIFIER.then(
                attributeList.thenSkip(
                    oneOf(
                        Lexical.string(">"),
                        Lexical.string("/>")
                    )
                )
            ).map(name -> attrs -> new StartTag(name, attrs))
        );

        // End tag parser - parse an end tag
        endTagBody.set(
            NAME_IDENTIFIER.thenSkip(
                Lexical.string(">")
            ).map(EndTag::new)
        );

        // Comment parser - parse an HTML comment
        declarationTagBody.set(
            NAME_IDENTIFIER.then(
                COMMENT_BODY
                    .thenSkip(Lexical.chr('>'))
                    .or(attributeList.thenSkip(
                        oneOf(
                            Lexical.string(">"),
                            Lexical.string("/>")
                        )
                    )
                )
            ).map(name -> attributeList ->
                    new Declaration(name,attributeList)
            )
        );


        Parser<Character, Element> rawText =
            Lexical.chr(c -> c != '<')
                .oneOrMore() // avoid empty text nodes
                .map(chars -> new TextData(charsToString(chars)));

        Parser<Character, Element> anyTag =
            Lexical.chr('<').skipThen(
                oneOf(
                    // order: the next char after '<' decides which one is cheap to try first
                    Lexical.chr('!').skipThen(declarationTagBody),
                    Lexical.chr('/').skipThen(endTagBody),
                    // fallback to start tag
                    tagBody
                )
            );

        // element tries either a tag (when '<') or raw text, without wasting work
        element.set(oneOf(anyTag, rawText));

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
    public static List<Element> parseAll(String input) {
        List<Element> elements = new ArrayList<>();
        Input<Character> currentInput = Input.of(input);

        while (!currentInput.isEof()) {
            Result<Character, Element> result = element.parse(currentInput);
            if (!result.matches()) {
                // Skip one character and try again
                currentInput = currentInput.next();
                continue;
            }

            elements.add(result.value());
            currentInput = result.input();
        }

        return Collections.unmodifiableList(elements);
    }
}
