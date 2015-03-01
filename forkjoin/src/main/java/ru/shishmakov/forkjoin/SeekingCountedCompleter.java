package ru.shishmakov.forkjoin;

import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;

/**
 * Simple prototype of {@link CountedCompleter CountedCompleter&lt;T&gt;} class
 * for recursive computation into {@link ForkJoinPool}.
 *
 * @author Dmitriy Shishmakov
 * @see CountedCompleter
 * @see ForkJoinPool
 */
public class SeekingCountedCompleter extends CountedCompleter<String> {

    private final int from;
    private final int to;
    private final int threshold;
    private final BinaryOperator<Integer> operation;
    private final AtomicReference<String> rawResult = new AtomicReference<>(null);

    public SeekingCountedCompleter(BinaryOperator<Integer> operation, int from,
                                   int to, int threshold) {
        this.operation = operation;
        this.from = from;
        this.to = to;
        this.threshold = threshold;
    }

    public SeekingCountedCompleter(CountedCompleter<String> completer,
                                   BinaryOperator<Integer> operation, int from, int to, int threshold) {
        super(completer);
        this.operation = operation;
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
                this.complete(String.format("range of [%d..%d)", from, to));
            }
            this.quietlyComplete();
        } else {
            int mid = (from + to) >>> 1;
            final SeekingCountedCompleter completerLeft = new SeekingCountedCompleter(
                    this, operation, from, mid, threshold);
            final SeekingCountedCompleter completerRight = new SeekingCountedCompleter(
                    this, operation, mid, to, threshold);

            // quantity of sub task
            setPendingCount(2);
            // push into queue for asynchronous invocation
            completerLeft.fork();
            // push into queue for asynchronous invocation
            completerRight.fork();
        }
        this.propagateCompletion();
    }

    /**
     * {@inheritDoc}
     *
     * @return the result of the computation of the string type
     */
    @Override
    public String getRawResult() {
        return rawResult.get();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden method if thread-safe.
     *
     * @param rawResult summary data for this node; {@code null} value is default unsuccessful result
     */
    @Override
    protected void setRawResult(String rawResult) {
        this.rawResult.compareAndSet(null, rawResult);
    }

    /**
     * {@inheritDoc}
     *
     * @param caller the task invoking this method (which may
     *               be this task itself)
     */
    @Override
    public void onCompletion(CountedCompleter<?> caller) {
        final SeekingCountedCompleter child = (SeekingCountedCompleter) caller;
        final SeekingCountedCompleter parent = (SeekingCountedCompleter) child
                .getCompleter();
        if (parent != null) {
            final String result = child.getRawResult();
            parent.setRawResult(result);
        }
    }
}
