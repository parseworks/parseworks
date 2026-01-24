package io.github.parseworks;

import java.util.List;

public interface ResultError<I,A> extends Result<I,A>{

    String message();

    String message(int depth);

    String expected();

    ResultError<?,?> cause();

    List<ResultError<I,A>> combinedFailures();
}
