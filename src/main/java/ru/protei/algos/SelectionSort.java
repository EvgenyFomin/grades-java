package ru.protei.algos;

import java.util.Arrays;

public class SelectionSort {
    private static void selectionSort(int[] arr){
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            int min_idx = i;

            for (int j = i + 1; j < n; j++) {
                if (arr[j] < arr[min_idx]) {
                    min_idx = j;
                }
            }

            int temp = arr[i];
            arr[i] = arr[min_idx];
            arr[min_idx] = temp;
        }
    }

    public static void main(String[] args){
        int[] arr = { 64, 25, 12, 22, 11 };

        selectionSort(arr);

        System.out.println(Arrays.toString(args));
    }
}
