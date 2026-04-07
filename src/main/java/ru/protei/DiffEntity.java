package ru.protei;

public class DiffEntity<T> {
    private T initEntityState;
    private T resultEntityState;

    public DiffEntity(T initEntityState, T resultEntityState) {
        this.initEntityState = initEntityState;
        this.resultEntityState = resultEntityState;
    }

    public T getInitEntityState() {
        return initEntityState;
    }

    public void setInitEntityState(T initEntityState) {
        this.initEntityState = initEntityState;
    }

    public T getResultEntityState() {
        return resultEntityState;
    }

    public void setResultEntityState(T resultEntityState) {
        this.resultEntityState = resultEntityState;
    }
}
