package ru.shishmakov.forkjoin;

import java.util.concurrent.RecursiveTask;
import java.util.function.BinaryOperator;

/**
 * Simple prototype of RecursiveTask class for recursive computation into {@link java.util.concurrent.ForkJoinPool}.
 *
 * @author Dmitriy Shishmakov
 * @see java.util.concurrent.RecursiveTask
 * @see java.util.concurrent.ForkJoinPool
 */
public class SeekingRecursiveTask extends RecursiveTask<String> {

    private static final String EMPTY = "";

    private final int from;
    private final int to;
    private final int threshold;
    private final BinaryOperator<Integer> operation;

    public SeekingRecursiveTask(BinaryOperator<Integer> operation, int from,
                                int to, int threshold) {
        this.operation = operation;
        this.from = from;
        this.to = to;
        this.threshold = threshold;
    }

    /**
     * {@inheritDoc}<br/>
     * Decision of sequential processing or divide task into subtask.
     *
     * @return the result of the computation
     */
    @Override
    public String compute() {
        if (to - from < threshold) {
            final int result = operation.apply(from, to);
            if (result >= 0) {
                return String.format("range of [%d..%d)", from, to);
            }
            return EMPTY;
        } else {
            int mid = (from + to) >>> 1;
            final RecursiveTask<String> completerLeft = new SeekingRecursiveTask(
                    operation, from, mid, threshold);
            final RecursiveTask<String> completerRight = new SeekingRecursiveTask(
                    operation, mid, to, threshold);

            // push into queue for asynchronous invocation
            completerLeft.fork();
            completerRight.fork();
            return completerLeft.join() + completerRight.join();
        }
    }

}
