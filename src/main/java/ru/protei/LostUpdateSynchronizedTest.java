package ru.protei;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

@JCStressTest
@Outcome(id = "2", expect = Expect.ACCEPTABLE, desc = "Оба инкремента выполнены")
@Outcome(id = "1", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Потерянное обновление!")
@State
public class LostUpdateSynchronizedTest {
    private int counter = 0;
    
    @Actor
    public void actor1() {
        synchronized (this) {
            counter++;
        }
    }
    
    @Actor
    public void actor2() {
        synchronized (this) {
            counter++;
        }
    }
    
    @Arbiter
    public void arbiter(I_Result r) {
        r.r1 = counter;
    }
}