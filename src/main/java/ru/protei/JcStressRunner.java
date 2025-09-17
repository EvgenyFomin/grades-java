package ru.protei;

import org.openjdk.jcstress.Main;

import java.util.ArrayList;
import java.util.List;

public class JcStressRunner {
    public static void main(String[] args) {
        List<String> arguments = new ArrayList<>();
        arguments.add("-v");
        arguments.add("-t"); arguments.add(ReadWriteTest.class.getName());
//        arguments.add("-t"); arguments.add("ru.protei.LostUpdateTest");

        try {
            Main.main(arguments.toArray(new String[0]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}