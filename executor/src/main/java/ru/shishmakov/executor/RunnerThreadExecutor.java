package ru.shishmakov.executor;

import java.util.concurrent.CountDownLatch;

/**
 * Class runs simple prototype of executor service
 *
 * @author Dmitriy Shishmakov
 * @see ru.shishmakov.executor.CustomThreadExecutor
 */
public class RunnerThreadExecutor {

    public static void main(String[] args) {
        final int threadsCount = Runtime.getRuntime().availableProcessors() * 2;
        final int taskCount = 10;
        final CustomThreadExecutor executor = new CustomThreadExecutor(threadsCount);
        final CountDownLatch latch = new CountDownLatch(taskCount);
        for (int i = 0; i < taskCount; i++) {
            int number = i;
            executor.execute(() -> {
                System.out.println("Task-" + number);
                latch.countDown();
            });
        }
        try {
            // waiting completion of all tasks
            latch.await();
        } catch (InterruptedException ignored) {
        }
        executor.shutdownNow();
    }

}
