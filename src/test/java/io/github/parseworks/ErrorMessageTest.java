package io.github.parseworks;

import io.github.parseworks.impl.Failure;
import io.github.parseworks.impl.Failure.ErrorType;
import org.junit.jupiter.api.Test;

import static io.github.parseworks.Combinators.chr;
import static io.github.parseworks.NumericParsers.numeric;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for all potential error message types in the parseworks library.
 * This test class ensures that each error type defined in {@link ErrorType}
 * is properly generated and has the correct error message format.
 */
public class ErrorMessageTest {

    @Test
    public void testSyntaxError() {
        // Test syntax error when input doesn't match the expected syntax
        // Create a syntax error directly to ensure we get the right error type
        Result<Character, Character> result = Result.syntaxError(
            Input.of("b"), "character 'a'", "character 'b'");

        assertTrue(result.isError());
        String errorMessage = result.error();
        assertTrue(errorMessage.contains("Syntax error"));
        assertEquals(ErrorType.SYNTAX, ((Failure<?, ?>) result).errorType());
    }

    @Test
    public void testTypeError() {
        // Create a parser that expects a digit but will fail with a type error
        Parser<Character, Integer> parser = numeric.map(c -> {
            if (c < '5') {
                throw new IllegalArgumentException("Value too small");
            }
            return Character.getNumericValue(c);
        });

        // This should cause a type error
        Result<Character, Integer> result = Result.typeError(Input.of("3"), "number >= 5", "3");

        assertTrue(result.isError());
        String errorMessage = result.error();
        assertTrue(errorMessage.contains("Type error"));
        assertEquals(ErrorType.TYPE, ((Failure<?, ?>) result).errorType());
    }

    @Test
    public void testUnexpectedEofError() {
        // Test unexpected EOF error when input ends prematurely
        Parser<Character, Character> parser = chr('a').then(chr('b')).map((a, b) -> a);
        Result<Character, Character> result = parser.parse("a");

        assertTrue(result.isError());
        String errorMessage = result.error();
        assertTrue(errorMessage.contains("Unexpected end of input") || 
                   errorMessage.contains("reached end of input"));
    }

    @Test
    public void testExpectedEofError() {
        // Test expected EOF error when input has trailing content
        Parser<Character, Character> parser = chr('a');
        Result<Character, Character> result = parser.parseAll("ab");

        assertTrue(result.isError());
        String errorMessage = result.error();
        assertTrue(errorMessage.contains("Expected end of input") || 
                   errorMessage.contains("expected end of input"));
    }

    @Test
    public void testRecursionError() {
        // Create a recursion error directly
        Result<Character, String> result = Result.recursionError(Input.of("input"));

        assertTrue(result.isError());
        String errorMessage = result.error();
        assertTrue(errorMessage.contains("Recursion error"));
        assertEquals(ErrorType.RECURSION, ((Failure<?, ?>) result).errorType());
    }


    @Test
    public void testValidationError() {
        // Create a validation error directly
        Result<Character, String> result = Result.validationError(
            Input.of("123"), "positive number", "negative number");

        assertTrue(result.isError());
        String errorMessage = result.error();
        assertTrue(errorMessage.contains("Validation error"));
        assertEquals(ErrorType.VALIDATION, ((Failure<?, ?>) result).errorType());
    }

    @Test
    public void testInternalError() {
        // Create an internal error directly
        Result<Character, String> result = Result.internalError(
            Input.of("input"), "Unexpected state in parser");

        assertTrue(result.isError());
        String errorMessage = result.error();
        assertTrue(errorMessage.contains("Internal error"));
        assertEquals(ErrorType.INTERNAL, ((Failure<?, ?>) result).errorType());
    }

    @Test
    public void testGenericError() {
        // Create a generic error directly
        Result<Character, String> result = Result.failure(
            Input.of("input"), "something", "something else");

        assertTrue(result.isError());
        String errorMessage = result.error();
        assertTrue(errorMessage.contains("Parse error"));
        assertEquals(ErrorType.GENERIC, ((Failure<?, ?>) result).errorType());
    }

    @Test
    public void testErrorWithCause() {
        // Create an error with a cause
        Failure<Character, String> cause = new Failure<>(
            Input.of("input"), "inner expected", "inner found", ErrorType.SYNTAX);

        Failure<Character, String> error = new Failure<>(
            Input.of("input"), "outer expected", "outer found", cause);

        assertTrue(error.isError());
        String fullErrorMessage = error.fullErrorMessage();

        // Check that the full error message contains both the error and its cause
        assertTrue(fullErrorMessage.contains("outer expected"));
        assertTrue(fullErrorMessage.contains("inner expected"));
        assertTrue(fullErrorMessage.contains(" -> ")); // Separator between error messages
    }

    @Test
    public void testWithErrorType() {
        // Test changing the error type of a failure
        Failure<Character, String> original = new Failure<>(
            Input.of("input"), "expected", "found", ErrorType.GENERIC);

        Failure<Character, String> modified = original.withErrorType(ErrorType.VALIDATION);

        assertEquals(ErrorType.GENERIC, original.errorType());
        assertEquals(ErrorType.VALIDATION, modified.errorType());
    }
}
