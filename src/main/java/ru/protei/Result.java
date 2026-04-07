package ru.protei;

public class Result<T> {
    private T data;
    private En_ResultStatus status;

    public Result(T data, En_ResultStatus status) {
        this.data = data;
        this.status = status;
    }

    public static <T> Result<T> ok() {
        return new Result<>(null, En_ResultStatus.OK);
    }

    public static <T> Result<T> error(En_ResultStatus status) {
        return new Result<>(null, status);
    }

    public boolean isOk() {
        return status == En_ResultStatus.OK;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Result{" +
                "data=" + data +
                ", status=" + status +
                '}';
    }
}
