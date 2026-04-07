package ru.protei;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Chain<T, R> implements ChainLink<T, R>, ChainStep<T, R> {
    private List<ChainLink<T, R>> chainLinks = new ArrayList<>();
    private Iterator<ChainLink<T, R>> chainLinkIterator;
    private Result<R> result;

    public Result<R> start(T t) {
        result = Result.ok();
        doNextLocally(t, result);
        return result;
    }

    public Result<R> start(T t, Result<R> r) {
        result = r;
        doNextLocally(t, result);
        return result;
    }

    @Override
    public void doNext(T t, Result<R> res) {
        this.result = res;
        doNextLocally(t, res);
    }

    @Override
    public void doComplete(Result<R> completedResult) {
        chainLinkIterator = null;
        this.result = completedResult;
    }

    public Chain<T, R> addLink(ChainLink<T, R> chainLink) {
        chainLinks.add(chainLink);
        return this;
    }

    @Override
    public void doAction(T t, Result<R> result, ChainStep<T, R> chain) {}

    private void doNextLocally(T t, Result<R> res) {
        if (chainLinkIterator == null) {
            chainLinkIterator = chainLinks.iterator();
        }

        if (chainLinkIterator.hasNext()) {
            chainLinkIterator.next().doAction(t, res, this);
        } else {
            chainLinkIterator = null;
        }
    }
}
