package io.github.parseworks.impl;

import io.github.parseworks.Input;
import io.github.parseworks.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Represents a failure result in a parser combinator.
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 */
public record Failure<I, A>(
        Input<I> input,
        String expected,
        String found,
        Failure<?, ?> cause,
        ErrorType errorType
) implements Result<I, A> {

    /**
     * Categorizes different types of parsing errors for more consistent handling.
     */
    public enum ErrorType {
        /** Syntax error - input doesn't match expected syntax */
        SYNTAX("Syntax error"),

        /** Type error - input has correct syntax but wrong type */
        TYPE("Type error"),

        /** Unexpected EOF - input ended prematurely */
        UNEXPECTED_EOF("Unexpected end of input"),

        /** Expected EOF - input has trailing content */
        EXPECTED_EOF("Expected end of input"),

        /** Recursion error - infinite recursion detected */
        RECURSION("Recursion error"),

        /** Validation error - input parsed but failed validation */
        VALIDATION("Validation error"),

        /** Internal error - unexpected error in parser logic */
        INTERNAL("Internal error"),

        /** Generic error - unspecified error type */
        GENERIC("Parse error");

        private final String label;

        ErrorType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public Failure(Input<I> input, String expected, String found) {
        this(input, expected, found, null, ErrorType.GENERIC);
    }

    public Failure(Input<I> input, String expected, String found, Failure<?, ?> cause) {
        this(input, expected, found, cause, cause != null ? cause.errorType() : ErrorType.GENERIC);
    }

    public Failure(Input<I> input, String expected, String found, ErrorType errorType) {
        this(input, expected, found, null, errorType);
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public boolean isError() {
        return true;
    }

    @Override
    public A get() {
        throw new RuntimeException(fullErrorMessage());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<I, B> cast() {
        return (Result<I, B>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<I, B> map(Function<A, B> mapper) {
        return (Result<I, B>) this;
    }

    @Override
    public String error() {
        String foundInstead = this.found != null ? this.found : input.hasMore() ? String.valueOf(input.current()) : "end of input";
        StringBuilder message = new StringBuilder();

        // Add error type label
        message.append(errorType.getLabel()).append(" at position ").append(input.position()).append(": ");

        // Add parser context if available
        if (expected != null && !expected.isEmpty()) {
            // Make the expected message more descriptive
            switch (expected) {
                case "eof" -> message.append("end of input");
                case "one of" -> message.append("one of the specified options");
                case "inequality" -> message.append("a different value");
                case "equivalence" -> message.append("an equivalent value");
                case "progress" -> message.append("Parser failed to make progress");
                default -> message.append("Expected ").append(expected);
            }
        } else {
            message.append("Expected correct input");
        }

        // Add what was found
        if (input.hasMore()) {
            message.append(" but found ").append(foundInstead);
        } else {
            message.append(" but reached end of input");
        }

        return message.toString();
    }

    public String fullErrorMessage() {
        List<String> messages = new ArrayList<>();
        Failure<?, ?> current = this;
        while (current != null) {
            messages.add(current.error());
            current = current.cause();
        }
        return String.join(" -> ", messages);
    }

    public Failure<?, ?> cause() {
        return cause;
    }

    @Override
    public <B> B handle(Function<Result<I, A>, B> success, Function<Result<I, A>, B> failure) {
        return failure.apply(this);
    }

    /**
     * Creates a new Failure with the same input, expected, and found values,
     * but with a different error type.
     *
     * @param newErrorType the new error type
     * @return a new Failure with the updated error type
     */
    public Failure<I, A> withErrorType(ErrorType newErrorType) {
        return new Failure<>(input, expected, found, cause, newErrorType);
    }
}
