package io.github.parseworks.impl.result;

import io.github.parseworks.Failure;
import io.github.parseworks.Input;
import io.github.parseworks.Result;
import io.github.parseworks.ResultType;

import java.util.List;
import java.util.function.Function;

/**
 * Represents a partial match result in a parser combinator.
 * This indicates that the parser matched, but not all of the input was consumed
 * or it's a success that should be distinguished from a full match.
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 */
public record PartialMatch<I, A>(
        Input<I> input,
        Failure<I, A> cause
) implements Failure<I, A> {

    @Override
    public Input<I> input() {
        return cause.input();
    }

    @Override
    public Failure<I, A> cause() {
        return cause;
    }

    @Override
    public String expected() {
        return cause.expected();
    }

    @Override
    public List<Failure<I, A>> combinedFailures() {
        return cause.combinedFailures();
    }

    @Override
    public ResultType type() {
        return ResultType.PARTIAL;
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
    public <B> B handle(Function<Result<I, A>, B> success, Function<Result<I, A>, B> failure) {
        // Since matches() is false, we should probably call failure.
        // But wait, the previous implementation called success.apply(this).
        // If it's a failure (matches() == false), standard practice is to call the failure handler.
        return failure.apply(this);
    }

    public String toString() {
        return "PartialMatch(" + cause + ")";
    }
}
