package io.github.parseworks.html;

import io.github.parseworks.FList;
import io.github.parseworks.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleHtmlParserTest {

    @Test
    public void testParseTag() {
        Result<Character, SimpleHtmlParser.Element> result = SimpleHtmlParser.parse("<div>");
        assertTrue(result.matches());
        assertInstanceOf(SimpleHtmlParser.StartTag.class, result.value());
        SimpleHtmlParser.StartTag tag = (SimpleHtmlParser.StartTag) result.value();
        assertEquals("div", tag.getName());
        assertTrue(tag.getAttributes().isEmpty());
    }

    @Test
    public void testParseTagWithAttributes() {
        Result<Character, SimpleHtmlParser.Element> result = SimpleHtmlParser.parse("<div id=\"main\" class=\"container\">");
        assertTrue(result.matches());
        assertInstanceOf(SimpleHtmlParser.StartTag.class, result.value());
        SimpleHtmlParser.StartTag tag = (SimpleHtmlParser.StartTag) result.value();
        assertEquals("div", tag.getName());
        assertEquals(2, tag.getAttributes().size());
        assertEquals("main", tag.getAttributes().get("id"));
        assertEquals("container", tag.getAttributes().get("class"));
    }

    @Test
    public void testParseEndTag() {
        Result<Character, SimpleHtmlParser.Element> result = SimpleHtmlParser.parse("</div>");
        assertTrue(result.matches());
        assertInstanceOf(SimpleHtmlParser.EndTag.class, result.value());
        SimpleHtmlParser.EndTag tag = (SimpleHtmlParser.EndTag) result.value();
        assertEquals("div", tag.getName());
    }

    @Test
    public void testParseComment() {
        Result<Character, SimpleHtmlParser.Element> result = SimpleHtmlParser.parse("<!-- This is a comment -->");
        assertTrue(result.matches());
        assertInstanceOf(SimpleHtmlParser.Declaration.class, result.value());
        SimpleHtmlParser.Declaration comment = (SimpleHtmlParser.Declaration) result.value();
        assertEquals(" This is a comment ", comment.getAttributeValue("data"));
    }

    @Test
    public void testParseText() {
        Result<Character, SimpleHtmlParser.Element> result = SimpleHtmlParser.parse("Hello, world!");
        assertTrue(result.matches());
        assertInstanceOf(SimpleHtmlParser.TextData.class, result.value());
        SimpleHtmlParser.TextData text = (SimpleHtmlParser.TextData) result.value();
        assertEquals("Hello, world!", text.getText());
    }

    @Test
    public void testParseMultipleElements() {
        FList<SimpleHtmlParser.Element> elements = SimpleHtmlParser.parseAll("<div>Hello, world!</div>");
        assertEquals(3, elements.size());

        assertInstanceOf(SimpleHtmlParser.StartTag.class, elements.get(0));
        SimpleHtmlParser.StartTag startTag = (SimpleHtmlParser.StartTag) elements.get(0);
        assertEquals("div", startTag.getName());

        assertInstanceOf(SimpleHtmlParser.TextData.class, elements.get(1));
        SimpleHtmlParser.TextData text = (SimpleHtmlParser.TextData) elements.get(1);
        assertEquals("Hello, world!", text.getText());

        assertInstanceOf(SimpleHtmlParser.EndTag.class, elements.get(2));
        SimpleHtmlParser.EndTag endTag = (SimpleHtmlParser.EndTag) elements.get(2);
        assertEquals("div", endTag.getName());
    }

    @Test
    public void testParseComplexHtml() {
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Test Page</title>
                </head>
                <body>
                    <!-- Header section -->
                    <header id="main-header">
                        <h1>Welcome</h1>
                    </header>
                    <div class="content">
                        <p>This is a <strong>test</strong> paragraph.</p>
                    </div>
                </body>
                </html>""";

        FList<SimpleHtmlParser.Element> elements = SimpleHtmlParser.parseAll(html);
        assertFalse(elements.isEmpty());

        // Count the number of each element type
        int startTagCount = 0;
        int endTagCount = 0;
        int textCount = 0;
        int commentCount = 0;

        System.out.println("[DEBUG_LOG] Parsed elements:");
        for (SimpleHtmlParser.Element element : elements) {
            if (element instanceof SimpleHtmlParser.StartTag tag) {
                startTagCount++;
                System.out.println("[DEBUG_LOG] StartTag: " + tag.getName() + ", attrs: " + tag.getAttributes());
            } else if (element instanceof SimpleHtmlParser.EndTag tag) {
                endTagCount++;
                System.out.println("[DEBUG_LOG] EndTag: " + tag.getName());
            } else if (element instanceof SimpleHtmlParser.TextData text) {
                textCount++;
                String displayText = text.getText().replace("\n", "\\n").replace("\r", "\\r");
                if (displayText.length() > 30) {
                    displayText = displayText.substring(0, 27) + "...";
                }
                System.out.println("[DEBUG_LOG] TextData: " + displayText);
            } else if (element instanceof SimpleHtmlParser.Declaration declaration) {
                if (declaration.getName().equals("--")){
                    commentCount++;
                    System.out.println("[DEBUG_LOG] Comment: " + declaration.getAttributeValue("data"));
                }

            }
        }

        System.out.println("[DEBUG_LOG] Counts - Start tags: " + startTagCount + ", End tags: " + endTagCount + 
                           ", Text: " + textCount + ", Comments: " + commentCount);

        assertTrue(startTagCount > 0, "Should have start tags");
        assertTrue(endTagCount > 0, "Should have end tags");
        assertTrue(textCount > 0, "Should have text data");
        assertEquals(1, commentCount, "Should have one comment");

        // Verify that the number of start and end tags match
        assertEquals(startTagCount, endTagCount, "Number of start and end tags should match");
    }
}
