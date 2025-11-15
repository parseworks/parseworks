package io.github.parseworks.impl.result;

import io.github.parseworks.Input;
import io.github.parseworks.Result;
import io.github.parseworks.TextInput;

import java.util.ArrayList;
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
        NoMatch<?, ?> cause,
        List<NoMatch<I,A>> combinedFailures
) implements Result<I, A> {

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
    public NoMatch(Input<I> input, String expected, NoMatch<?, ?> cause) {
        this(input, expected, cause, null);
    }

    /**
     * Constructs a new NoMatch with a cause, inheriting the cause's error type,
     * with no custom message.
     */
    public NoMatch(List<NoMatch<I,A>> failures) {
        this(failures.isEmpty() ? null : failures.get(0).input(), null, null, failures);
    }



    // No explicit canonical constructor override is needed; the record-generated
    // canonical constructor is used.

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
        return buildSingleFrameMessage();
    }

    /**
     * Builds a single, human-friendly message that aggregates multiple failures
     * occurring at the same input position. This is useful when several
     * alternative parsers were tried and all failed at the same point: the
     * combined message will show the location once and list all distinct reasons.
     *
     * Note: This method only formats the message string. It does not include
     * code snippets for each inner failure to keep output compact; the top-level
     * snippet (for text input) is included once.
     */
    private String buildCombinedReasonsMessage(List<NoMatch<I, A>> failures) {
        NoMatch<I, A> first = failures.get(0);
        Input<I> errorInput = first.input;
        
        // Find a failure with a valid input if the first one doesn't have it (e.g. wrapper)
        if (errorInput == null) {
            for (NoMatch<I, A> f : failures) {
                if (f.input != null) {
                    errorInput = f.input;
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
            .map(NoMatch::rootMessage)
            .distinct()
            .forEach(sb::append);

        return sb.toString();
    }

    private String rootMessage(){
        return rootMessage(0);
    }

    private String rootMessage(int depth){
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
                // If we don't have input context, we might not know if it's EOF
                builder.append(" found unknown input");
            }

            builder.append("\n");

            if (cause != null) {
                builder.append(cause.rootMessage(depth + 1));
            }

            return builder.toString();
    }

    /**
     * Combines this failure with another failure into a new aggregated failure at this input position.
     * <p>
     * The resulting failure contains a single, readable message that lists distinct reasons from both
     * failures while showing the location only once. This does not mutate either failure; a new instance
     * is returned.
     * <p>
     * Note: If the other failure is {@code null}, this failure is returned unchanged.
     *
     * @param other another failure to combine with
     * @return a new aggregated failure message representing both failures
     */
    public NoMatch<I, A> combineWith(NoMatch<I, A> other) {
        List<NoMatch<I,A>> failures = this.combinedFailures;
        if (failures == null) {
            failures = new ArrayList<>();
            failures.add(this);
        }
        failures.add(other);
        return new NoMatch<>(failures);
    }

    public NoMatch<I, A> combineWith(List<NoMatch<I, A>> moreFailures) {
        List<NoMatch<I,A>> allFailures = this.combinedFailures;
        if (allFailures == null) {
            allFailures = new ArrayList<>();
            allFailures.add(this);
        }
        allFailures.addAll(moreFailures);
        return new NoMatch<>(allFailures);
    }

    // Builds a single-frame message for this failure without traversing the cause chain
    private String buildSingleFrameMessage() {
        // If we have aggregated failures, lazily synthesize the combined message now
        if (combinedFailures != null && !combinedFailures.isEmpty()) {
            return buildCombinedReasonsMessage(combinedFailures);
        }

        return buildCombinedReasonsMessage(java.util.List.of(this));
    }

    // Builds the chained error message including all causes, with indentation
    private String buildChainedMessage() {
        StringBuilder message = new StringBuilder();
        NoMatch<?, ?> current = this;
        int depth = 0;

        while (current != null) {
            if (depth > 0) {
                message.append("-".repeat(depth)).append("> Caused by: ");
            }

            String errorMessage = current.buildSingleFrameMessage();
            if (depth > 0) {
                errorMessage = errorMessage.replace("\n", "\n" + "  ".repeat(depth + 1));
            }

            message.append(errorMessage);
            current = current.cause();
            depth++;
        }

        return message.toString();
    }

    public NoMatch<?, ?> cause() {
        return cause;
    }

    @Override
    public <B> B handle(Function<Result<I, A>, B> success, Function<Result<I, A>, B> failure) {
        return failure.apply(this);
    }

}
