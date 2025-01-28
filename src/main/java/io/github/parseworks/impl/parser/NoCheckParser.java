package io.github.parseworks.impl.parser;

import io.github.parseworks.Input;
import io.github.parseworks.Parser;
import io.github.parseworks.Result;

import java.util.function.Function;

/**
 * A parser that does not perform any recursive descent checks.
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 */
public class NoCheckParser<I, A> extends Parser<I, A> {

    public NoCheckParser(Function<Input<I>, Result<I, A>> applyHandler) {
        super(applyHandler);
    }

    @Override
    public Result<I, A> apply(Input<I> in) {
        return applyHandler.apply(in);
    }

}
