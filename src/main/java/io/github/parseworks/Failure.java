package io.github.parseworks;

import java.util.List;

/**
 * Represents a failure result in a parser combinator.
 * Both NoMatch and PartialMatch are types of Failure.
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 */
public interface Failure<I, A> extends Result<I, A> {
    /**
     * Returns the underlying failure that caused this failure.
     *
     * @return the underlying failure, or null if there is no cause
     */
    Failure<?, ?> cause();

    /**
     * Returns what was expected by the parser that failed.
     *
     * @return the expected input description
     */
    String expected();

    /**
     * Returns a list of failures that were combined to form this failure.
     * This is used when multiple alternative parsers fail at the same position.
     *
     * @return the list of combined failures, or null if not an aggregated failure
     */
    List<Failure<I, A>> combinedFailures();

    /**
     * Returns a human-friendly error message for this failure.
     *
     * @return the error message
     */
    @Override
    default String error() {
        List<Failure<I, A>> failures;
        var combinedFailures = this.combinedFailures();
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

        StringBuilder sb = new StringBuilder();
        if (this.type() == ResultType.PARTIAL) {
            sb.append("Partial match failed: ");
        } else {
            sb.append("Error:");
        }
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

    /**
     * Returns a human-friendly error message for this failure with indentation based on depth.
     *
     * @param depth the depth of the failure in the chain
     * @return the indented error message
     */
    default String error(int depth) {
        String indent = "  ".repeat(depth);
        StringBuilder builder = new StringBuilder(indent);
        builder.append("- ");
        if (depth > 0) builder.append("caused by: ");
        var expected = this.expected();
        if (expected != null && !expected.isEmpty()) {
            builder.append("expected ").append(expected);
        } else {
            builder.append("expected correct input");
        }

        String foundValue = null;
        var input = this.input();
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
        var cause = this.cause();
        if (cause != null) {
            builder.append(cause.error(depth + 1));
        }

        return builder.toString();
    }

}
