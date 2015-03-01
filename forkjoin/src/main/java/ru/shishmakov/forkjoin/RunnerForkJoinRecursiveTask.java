package ru.shishmakov.forkjoin;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.BinaryOperator;

/**
 * Class runs simple prototype of RecursiveTask for Fork/Join Framework.
 * <p>
 * There is range of the sorted numbers [0 .. 1_000_000).
 * Need to find the specified number and display the range in which it was found.
 * Uses a binary search is divided into between subranges.
 * Source code shows recursive division and expectation of the return value.
 *
 * @author Dmitriy Shishmakov
 * @see ru.shishmakov.forkjoin.SeekingRecursiveTask
 * @see java.util.concurrent.ForkJoinPool
 */
public class RunnerForkJoinRecursiveTask {
    public static void main(String[] args) {
        final int fromInclusive = 0;
        final int toExclusive = 1_000_000;
        final int parallelism = Runtime.getRuntime().availableProcessors() * 2;
        final int cores = Runtime.getRuntime().availableProcessors();
        final int threshold = toExclusive / (cores * 20);

        // function for perform the action
        final BinaryOperator<Integer> function = (left, right) -> {
            final int[] array = new int[right - left];
            int value = left;
            for (int i = 0; i < array.length; i++) {
                array[i] = value++;
            }
            return Arrays.binarySearch(array, 499_100);
        };

        final RecursiveTask<String> task = new SeekingRecursiveTask(function,
                fromInclusive, toExclusive, threshold);
        final ForkJoinPool pool = new ForkJoinPool(parallelism);
        // synchronous call task
        final String position = pool.invoke(task);
        System.out.printf("position of number %d into: %s%n", 499_100,
                position);
    }

}
