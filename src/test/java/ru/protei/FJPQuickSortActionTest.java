package ru.protei;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class FJPQuickSortActionTest {
    @Test
    public void testForkJoinPoolQuickSort() {
        List<Person> people = new ArrayList<>(Arrays.asList(
                new Person(10, "j"),
                new Person(9, "i"),
                new Person(8, "h"),
                new Person(7, "g"),
                new Person(6, "f"),
                new Person(7, "e"),
                new Person(2, "d"),
                new Person(1, "c"),
                new Person(3, "b"),
                new Person(5, "a")
        ));

        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPool.invoke(new QuickSortSplitter<>(people, Person::compareTo));
        System.out.println("Result ----------> " + people);
    }

    private static class Person implements Comparable<Person> {
        private final int age;
        private final String name;

        public Person(int age, String name) {
            this.age = age;
            this.name = name;
        }

        @Override
        public int compareTo(Person o) {
            return this.name.compareTo(o.name);
        }

        @Override
        public String toString() {
            return "Person{" +
                    "age=" + age +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    private static class QuickSortSplitter<T> extends RecursiveAction {
        private final Comparator<T> comparator;
        private final List<T> elements;
        private final int high;
        private int low;

        public QuickSortSplitter(List<T> elements, Comparator<T> comparator) {
            this.elements = elements;
            this.comparator = comparator;
            this.low = 0;
            this.high = elements.size() - 1;
        }

        public QuickSortSplitter(int low, int high, List<T> elements, Comparator<T> comparator) {
            this.elements = elements;
            this.comparator = comparator;
            this.low = low;
            this.high = high;
        }

        @Override
        protected void compute() {
            if (high <= low) {
                return;
            }

            int divider = partition(low, high, elements, comparator);
            ForkJoinTask<Void> leftResult = new QuickSortSplitter<>(low, divider - 1, elements, comparator).fork();
            low = divider + 1;
            compute();
            leftResult.join();
        }

        private int partition(int low, int high, List<T> elements, Comparator<T> comparator) {
            System.out.printf("Partition on Thread = %s started. List = %s\n", Thread.currentThread().getName(), elements.subList(low, high + 1));
            T pivot = elements.get(high);
            int divider = low - 1;

            for (int i = low; i < high; i++) {
                if (comparator.compare(elements.get(i), pivot) <= 0) {
                    divider++;
                    swap(elements, divider, i);
                }
            }

            divider++;

            swap(elements, divider, high);

            System.out.println(makePartitionFinishedMessage(low, high, divider, elements));
            return divider;
        }

        private void swap(List<T> elements, int from, int to) {
            T tmp = elements.get(from);
            elements.set(from, elements.get(to));
            elements.set(to, tmp);
        }

        private String makePartitionFinishedMessage(int low, int high, int divider, List<T> elements) {
            StringBuilder msg = new StringBuilder("Partition on Thread = ").append(Thread.currentThread().getName()).append(" finished. List = [");

            for (int i = low; i < high + 1; i++) {
                if (i != low) {
                    msg.append(", ");
                }

                if (i == divider) {
                    msg.append("->").append(elements.get(i)).append("<-");
                } else {
                    msg.append(elements.get(i));
                }

                if (i + 1 == high + 1) {
                    msg.append("]");
                }
            }

            return msg.toString();
        }
    }
}
