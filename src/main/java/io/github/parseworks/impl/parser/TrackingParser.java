package io.github.parseworks.impl.parser;

import io.github.parseworks.Input;
import io.github.parseworks.Parser;
import io.github.parseworks.Result;

import java.util.Stack;
import java.util.function.Function;

public class TrackingParser<I, A>  extends Parser<I, A>  {

    Stack<Integer> indexStack = new Stack<>();

    public TrackingParser(Function<Input<I>, Result<I, A>> applyHandler) {
        super(applyHandler);
    }

    @Override
    public Result<I, A> apply(Input<I> in) {
        if (!indexStack.empty() && indexStack.peek() == in.position()) {
            return Result.failure(in, "Infinite loop detected");
        }
        indexStack.push(in.position());
        Result<I, A> result = applyHandler.apply(in);
        indexStack.pop();
        System.out.println("TrackingParser.apply: index = " + indexStack);
        return result;
    }
}
