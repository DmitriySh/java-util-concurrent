package ru.shishmakov.forkjoin;

import java.util.Arrays;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;

/**
 * Class runs simple prototype of {@link CountedCompleter CountedCompleter&lt;T&gt;} for Fork/Join Framework.
 * <p>
 * There is range of the sorted numbers {@code [0 .. 1_000_000)}.
 * Need to find the specified number and display the range in which it was found.
 * Uses a binary search is divided into between subranges.
 * Source code shows recursive division and expectation of the return value.
 * Tasks of {@link CountedCompleter CountedCompleter&lt;T&gt;} not need to be joinable
 * (invoke {@link CountedCompleter#join() join} method).
 *
 * @author Dmitriy Shishmakov
 * @see SeekingCountedCompleterAndAtomic
 * @see ForkJoinPool
 */
public class RunnerForkJoinCountedCompleterAndAtomic {
    public static void main(String[] args) {
        final int fromInclusive = 0;
        final int toExclusive = 1_000_000;
        final int searchNumber = 499_100;

        // function for perform the action
        final BinaryOperator<Integer> function = (left, right) -> {
            final int[] array = new int[right - left];
            int value = left;
            for (int i = 0; i < array.length; i++) {
                array[i] = value++;
            }
            return Arrays.binarySearch(array, searchNumber);
        };

        final AtomicReference<String> position = new AtomicReference<>();
        final int cores = Runtime.getRuntime().availableProcessors();
        final int threshold = toExclusive / (cores * 20);
        final int parallelism = cores * 2;
        final CountedCompleter<Void> task = new SeekingCountedCompleterAndAtomic(
                function, position, fromInclusive, toExclusive, threshold);
        final ForkJoinPool pool = new ForkJoinPool(parallelism);
        // synchronous call task
        pool.invoke(task);
        System.out.printf("position of number %d into: %s%n", searchNumber,
                position.get());
    }


}
