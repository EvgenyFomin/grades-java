package ru.protei;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class FJPMergeSortTaskTest {
    @Test
    public void testForkJoinPoolMergeSort() {
        List<Person> longs = new ArrayList<>(Arrays.asList(
                new Person(10, "a"),
                new Person(9, "a"),
                new Person(8, "a"),
                new Person(7, "a"),
                new Person(6, "a"),
                new Person(5, "a"),
                new Person(4, "a"),
                new Person(3, "a"),
                new Person(2, "a"),
                new Person(2, "a"),
                new Person(2, "a"),
                new Person(2, "a"),
                new Person(2, "b"),
                new Person(2, "c")
        ));

        ForkJoinPool forkJoinPool = new ForkJoinPool();
        List<Person> result = forkJoinPool.invoke(new MergeSortSplitter<>(longs, Person::compareTo));
        System.out.println("Result ----------> " + result);
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

    private static class MergeSortSplitter<T> extends RecursiveTask<List<T>> {
        private final Comparator<T> comparator;
        private List<T> values;

        public MergeSortSplitter(List<T> values, Comparator<T> comparator) {
            this.values = values;
            this.comparator = comparator;
        }

        @Override
        protected List<T> compute() {
            if (values.size () <= 1) {
                return values;
            }

            System.out.println("List = " + values + ", Thread = " + Thread.currentThread().getName());

            int mid = values.size() / 2;
            ForkJoinTask<List<T>> leftTask = new MergeSortSplitter<>(values.subList(0, mid), comparator).fork();
            values = values.subList(mid, values.size());
            List<T> rightResult = compute();
            System.out.println("Join, Thread = " + Thread.currentThread().getName());
            List<T> leftResult = leftTask.join();

            System.out.println("Left = " + leftResult + ", Right = " + rightResult);

            return mergeResults(leftResult, rightResult, comparator);
        }

        private List<T> mergeResults(List<T> leftResult, List<T> rightResult, Comparator<T> comparator) {
            System.out.printf("Merge started on thread %s. Left = %s, Right = %s\n", Thread.currentThread().getName(), leftResult, rightResult);
            List<T> mergedResult = new ArrayList<>(leftResult.size() + rightResult.size());

            int leftIndex = 0;
            int rightIndex = 0;

            while (leftIndex != leftResult.size() || rightIndex != rightResult.size()) {
                if (leftIndex == leftResult.size()) {
                    mergedResult.add(rightResult.get(rightIndex++));
                    continue;
                }

                if (rightIndex == rightResult.size()) {
                    mergedResult.add(leftResult.get(leftIndex++));
                    continue;
                }

                if (comparator.compare(leftResult.get(leftIndex), rightResult.get(rightIndex)) < 0) {
                    mergedResult.add(leftResult.get(leftIndex++));
                    continue;
                }

                mergedResult.add(rightResult.get(rightIndex++));
            }

            System.out.printf("Merge ended on thread %s. Left = %s, Right = %s, Result = %s\n", Thread.currentThread().getName(), leftResult, rightResult, mergedResult);
            return mergedResult;
        }
    }
}
