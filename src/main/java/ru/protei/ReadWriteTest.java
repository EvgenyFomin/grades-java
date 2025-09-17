package ru.protei;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.II_Result;

@JCStressTest
@Outcome(id = "1, 1", expect = Expect.ACCEPTABLE, desc = "Обе записи")
@Outcome(id = {"0, 1", "1, 0"}, expect = Expect.ACCEPTABLE, desc = "Одна запись")
@Outcome(id = "0, 0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Отсутствие записей")
public class ReadWriteTest {
    @Actor
    public final void actor1(DataHolder dataHolder, II_Result r) {
        r.r1 = dataHolder.actor1();
    }

    @Actor
    public final void actor2(DataHolder dataHolder, II_Result r) {
        r.r2 = dataHolder.actor2();
    }

    @State
    public static class DataHolder {
        private int x = 0;
        private int y = 0;

        public int actor1() {
            x = 1;
            return y;
        }

        public int actor2() {
            y = 1;
            return x;
        }
    }
}