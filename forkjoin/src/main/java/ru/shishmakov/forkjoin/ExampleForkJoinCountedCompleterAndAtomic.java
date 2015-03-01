package ru.shishmakov.forkjoin;

import java.util.Arrays;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;

public class ExampleForkJoinCountedCompleterAndAtomic {
  public static void main(String[] args) {
    chooseForkJoinCountedCompleterAndAtomic(0, 1_000_000, 499_100);
  }

  private static void chooseForkJoinCountedCompleterAndAtomic(
      int fromInclusive, int toExclusive, int searchNumber) {
    if (searchNumber < fromInclusive || searchNumber >= toExclusive) {
      throw new IllegalArgumentException(String.format(
          "Search number out of array range[%d..%d): %d", fromInclusive,
          toExclusive, searchNumber));
    }
    final int cores = Runtime.getRuntime().availableProcessors();
    final int threshold = toExclusive / (cores * 20);
    final AtomicReference<String> position = new AtomicReference<>();
    final BinaryOperator<Integer> function = (left, right) -> {
      final int[] array = new int[right - left];
      int value = left;
      for (int i = 0; i < array.length; i++) {
        array[i] = value++;
      }
      return Arrays.binarySearch(array, searchNumber);
    };

    final CountedCompleter<Void> task = new SeekingCountedCompleterAndAtomic(
        function, position, fromInclusive, toExclusive, threshold);
    final int parallelism = Runtime.getRuntime().availableProcessors() * 2;
    final ForkJoinPool pool = new ForkJoinPool(parallelism);
    // synchronous call task
    pool.invoke(task);
    System.out.printf("position of number %d into: %s%n", searchNumber,
        position.get());
  }

  private static class SeekingCountedCompleterAndAtomic extends
      CountedCompleter<Void> {

    private final AtomicReference<String> result;
    private final int from;
    private final int to;
    private final int threshold;
    private final BinaryOperator<Integer> operation;

    public SeekingCountedCompleterAndAtomic(BinaryOperator<Integer> operation,
        AtomicReference<String> result, int from, int to, int threshold) {
      this.operation = operation;
      this.result = result;
      this.from = from;
      this.to = to;
      this.threshold = threshold;
    }

    public SeekingCountedCompleterAndAtomic(CountedCompleter<Void> completer,
        BinaryOperator<Integer> operation, AtomicReference<String> result,
        int from, int to, int threshold) {
      super(completer);
      this.operation = operation;
      this.result = result;
      this.from = from;
      this.to = to;
      this.threshold = threshold;
    }

    @Override
    public void compute() {
      if (to - from < threshold) {
        final int result = operation.apply(from, to);
        if (result >= 0) {
          this.result.set(String.format("range of [%d..%d)", from, to));
        }
        quietlyComplete();
      } else {
        int mid = (from + to) >>> 1;
        final SeekingCountedCompleterAndAtomic completerLeft = new SeekingCountedCompleterAndAtomic(
            this, operation, result, from, mid, threshold);
        final SeekingCountedCompleterAndAtomic completerRight = new SeekingCountedCompleterAndAtomic(
            this, operation, result, mid, to, threshold);

        // quantity of sub task
        setPendingCount(2);
        // push into queue for asynchronous invocation
        completerLeft.fork();
        // push into queue for asynchronous invocation
        completerRight.fork();
      }
      tryComplete();
    }
  }


}
