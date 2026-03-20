package ru.protei.memory.heapdump;

import java.util.ArrayList;
import java.util.List;

//-XX:+HeapDumpOnOutOfMemoryError
//-XX:HeapDumpPath=./java_oom.hprof
public class OOM {
    public static void main(String[] args) {
        List<byte[]> list = new ArrayList<>();
        while (true) {
            list.add(new byte[10_000_000]); // 10MB каждый раз
        }
    }
}
