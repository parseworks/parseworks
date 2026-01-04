package io.github.parseworks.impl.result;

import io.github.parseworks.*;

import java.util.List;
import java.util.function.Function;

/**
 * Represents a failure result in a parser combinator.
 * <p>
 * This class provides detailed error information including:
 * <ul>
 *   <li>The input position where the error occurred</li>
 *   <li>What was expected vs. what was found</li>
 *   <li>The type of the error</li>
 *   <li>A custom error message (if provided)</li>
 *   <li>The cause of the error (for nested errors)</li>
 * </ul>
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 */
public record NoMatch<I, A>(
        Input<I> input,
        String expected,
        Failure<?, ?> cause,
        List<Failure<I, A>> combinedFailures
) implements Failure<I, A> {

    /**
     * Constructs a new NoMatch with no custom message.
     */
    public NoMatch(Input<I> input, String expected) {
        this(input, expected, null, null);
    }

    /**
     * Constructs a new NoMatch with a cause, inheriting the cause's error type,
     * with no custom message.
     */
    public NoMatch(Input<I> input, String expected, Failure<?, ?> cause) {
        this(input, expected, cause, null);
    }

    /**
     * Constructs a new NoMatch with a cause, inheriting the cause's error type,
     * with no custom message.
     */
    public NoMatch(List<Failure<I, A>> failures) {
        this(failures.isEmpty() ? null : failures.get(0).input(), null, null, failures);
    }



    // No explicit canonical constructor override is needed; the record-generated
    // canonical constructor is used.

    @Override
    public ResultType type() {
        return ResultType.NO_MATCH;
    }

    @Override
    public boolean matches() {
        return false;
    }

    @Override
    public A value() {
        throw new RuntimeException(error());
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
        List<Failure<I, A>> failures;
        if (combinedFailures != null && !combinedFailures.isEmpty()) {
            failures = combinedFailures;
        } else {
            failures = List.of(this);
        }

        Failure<I, A> first = failures.get(0);
        Input<I> errorInput = first.input();

        if (errorInput == null) {
            for (Failure<I, A> f : failures) {
                if (f.input() != null) {
                    errorInput = f.input();
                    break;
                }
            }
        }

        StringBuilder sb = new StringBuilder("Error:");
        TextInput text = (errorInput instanceof TextInput ti) ? ti : null;

        if (text != null) {
            sb.append(" line ").append(text.line())
                .append(" position ").append(text.column())
                .append('\n')
                .append(text.getFormattedSnippet(1, 1));
        } else if (errorInput != null) {
            sb.append(" at position ").append(errorInput.position()).append('\n');
        } else {
            sb.append(" at unknown position\n");
        }

        sb.append("Reasons at this location:\n");
        failures.stream()
            .map(f -> f.error(0))
            .distinct()
            .forEach(sb::append);

        return sb.toString();
    }

    @Override
    public String error(int depth) {
        String indent = "  ".repeat(depth);
        StringBuilder builder = new StringBuilder(indent);
        builder.append("- ");
        if (depth > 0) builder.append("caused by: ");

        if (expected != null && !expected.isEmpty()) {
            builder.append("expected ").append(expected);
        } else {
            builder.append("expected correct input");
        }

        String foundValue = null;
        if (input != null && input.hasMore()) {
            foundValue = String.valueOf(input.current());
        }

        if (foundValue != null) {
            builder.append(" found ").append(foundValue);
        } else if (input != null && !input.hasMore()) {
            builder.append(" reached end of input");
        } else {
            builder.append(" found unknown input");
        }

        builder.append("\n");

        if (cause != null) {
            builder.append(cause.error(depth + 1));
        }

        return builder.toString();
    }


    @Override
    public <B> B handle(Function<Result<I, A>, B> success, Function<Result<I, A>, B> failure) {
        return failure.apply(this);
    }

}
