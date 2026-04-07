package ru.protei;

@FunctionalInterface
public interface ChainLink<T, R> {
    void doAction(T t, Result<R> r, ChainStep<T, R> chain);
}
