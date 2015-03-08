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
        final int taskCount = 10;
        final int threadsCount = Runtime.getRuntime().availableProcessors() * 2;
        final CustomThreadExecutor executor = new CustomThreadExecutor(threadsCount);
        try {
            putTasks(taskCount, executor);
        } finally {
            executor.shutdownNow();
        }

    }

    private static void putTasks(int taskCount, CustomThreadExecutor executor) {
        final CountDownLatch latch = new CountDownLatch(taskCount);
        // a simple range of tasks to perform
        for (int i = 0; i < taskCount; i++) {
            int number = i;
            executor.execute(() -> {
                System.out.printf("Task - %d%n", number);
                latch.countDown();
            });
        }
        // dangerous the task is going to throw an exception
        executor.execute(() -> {
            System.out.println("Task - The Fifth Element");
            latch.countDown();
            throw new RuntimeException("Big Bada Boom!");
        });
        try {
            // waiting completion of all tasks
            latch.await();
        } catch (InterruptedException ignored) {
        }
    }

}
