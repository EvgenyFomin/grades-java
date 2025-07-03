package ru.protei;

import java.util.List;

public class HistoryDAO {
    public void persistBatch(List<History> histories) {
        System.out.println("Persist histories......");
        System.out.println(histories);
        System.out.println("Persisted");
    }
}
