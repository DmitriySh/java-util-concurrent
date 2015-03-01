package ru.shishmakov.executor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Simple prototype of executor service with thread pool
 *
 * @author Dmitriy Shishmakov
 * @see java.util.concurrent.Executor
 */
public class CustomThreadExecutor implements Executor {
    private final WorkerThread[] pool;
    private final BlockingQueue<Runnable> taskQueue = new ArrayBlockingQueue<>(
            256);
    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean shutdown = false;


    public CustomThreadExecutor() {
        this(Runtime.getRuntime().availableProcessors() * 2);
    }

    public CustomThreadExecutor(final int threadsCount) {
        this.pool = new WorkerThread[threadsCount];
        for (int i = 0; i < pool.length; i++) {
            // initialization of thread
            pool[i] = new WorkerThread("Thread-" + i, taskQueue);
            pool[i].start();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param task the runnable task
     */
    @Override
    public void execute(final Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        if (shutdown) {
            System.out.println("Task has been rejected");
            return;
        }
        try {
            // Producer
            taskQueue.put(task);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Unsuccessful executed task!", e);
        }
    }

    /**
     * Tries to make shutdown for thread pool in which previously submitted tasks are executed.
     * New tasks won't be accepted.
     */
    public void shutdown() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            this.shutdown = true;
            if (!taskQueue.isEmpty()) {
                taskQueue.clear();
            }
            for (WorkerThread thread : pool) {
                thread.interrupt();
            }
            System.out.println("All tasks have been canceled!");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are
     * executed, but no new tasks will be accepted.
     */
    public void shutdownNow() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            shutdown();
            boolean terminated;
            do {
                terminated = true;
                for (WorkerThread thread : pool) {
                    if (!thread.isCancel()) {
                        terminated = false;
                        break;
                    }
                }
            } while (!terminated);
            System.out.println("All threads have been stopped!");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Worker thread for pool
     */
    private static class WorkerThread extends Thread {
        private final BlockingQueue<Runnable> taskQueue;
        private volatile boolean cancel;

        private WorkerThread(String name, final BlockingQueue<Runnable> taskQueue) {
            super(name);
            this.taskQueue = taskQueue;
            this.cancel = false;
        }

        public boolean isCancel() {
            return cancel;
        }

        @Override
        public void run() {
            for (; ; ) {
                try {
                    if (isInterrupted()) {
                        throw new InterruptedException();
                    }
                    // Consumer
                    final Runnable task = taskQueue.take();
                    task.run();
                } catch (InterruptedException e) {
                    this.cancel = true;
                    break;
                }
            }
            System.out.println("Thread " + getName() + " leaves the run() method");
        }
    }

}
