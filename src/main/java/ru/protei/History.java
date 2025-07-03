package ru.protei;

public class History {
    private final Long entityId;
    private final String oldValue;
    private final String newValue;

    public History(Long entityId, String oldValue, String newValue) {
        this.entityId = entityId;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public String toString() {
        return "History{" +
                "entityId=" + entityId +
                ", oldValue='" + oldValue + '\'' +
                ", newValue='" + newValue + '\'' +
                '}';
    }
}
