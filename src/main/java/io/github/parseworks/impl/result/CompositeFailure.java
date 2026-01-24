package io.github.parseworks.impl.result;

import io.github.parseworks.Failure;
import io.github.parseworks.Input;
import io.github.parseworks.Result;
import io.github.parseworks.ResultType;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a failure that is composed of multiple alternative failures.
 * This is used by combinators like oneOf and or.
 */
public record CompositeFailure<I, A>(
        List<Failure<I, A>> failures
) implements Failure<I, A> {

    @SafeVarargs
    public CompositeFailure(Failure<I,A> ... failures) {
        this(List.of(failures));
    }

    @Override
    public Input<I> input() {
        return failures.isEmpty() ? null : failures.get(0).input();
    }

    @Override
    public String expected() {
        return failures.stream()
                .map(Failure::expected)
                .filter(e -> e != null && !e.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    @Override
    public Failure<?, ?> cause() {
        return null;
    }

    @Override
    public List<Failure<I, A>> combinedFailures() {
        return failures;
    }

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
    public <B> B handle(Function<Result<I, A>, B> success, Function<Result<I, A>, B> failure) {
        return failure.apply(this);
    }

    @Override
    public String error(int depth) {
        if (depth == 0) {
            StringBuilder sb = new StringBuilder();
            failures.stream()
                    .map(f -> f.error(0))
                    .distinct()
                    .forEach(sb::append);
            return sb.toString();
        }
        return Failure.super.error(depth);
    }
}
