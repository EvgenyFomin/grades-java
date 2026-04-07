package ru.protei;

public interface ChainStep<T, R> {
    void doNext(T t, Result<R> res);
    void doComplete(Result<R> err);
}
