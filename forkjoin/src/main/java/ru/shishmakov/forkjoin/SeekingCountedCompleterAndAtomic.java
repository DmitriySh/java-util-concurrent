package ru.shishmakov.forkjoin;

import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;

/**
 * Simple prototype of {@link CountedCompleter CountedCompleter&lt;T&gt;} class
 * for recursive computation into {@link java.util.concurrent.ForkJoinPool}.
 *
 * @author Dmitriy Shishmakov
 * @see CountedCompleter
 * @see ForkJoinPool
 */
public class SeekingCountedCompleterAndAtomic extends
        CountedCompleter<Void> {

    private final int from;
    private final int to;
    private final int threshold;
    private final BinaryOperator<Integer> operation;
    private final AtomicReference<String> result;

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

    /**
     * {@inheritDoc}
     */
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
