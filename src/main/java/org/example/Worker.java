package org.example;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Worker implements Runnable {
    private final BlockingQueue<Runnable> taskQueue;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final CustomThreadPool pool;
    private Thread thread;
    private final AtomicBoolean isBusy = new AtomicBoolean(false);

    public Worker(CustomThreadPool pool, int queueSize, long keepAliveTime, TimeUnit timeUnit) {
        this.pool = pool;
        this.taskQueue = new LinkedBlockingQueue<>(queueSize);
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public boolean addTask(Runnable task) {
        return taskQueue.offer(task);
    }

    public int getQueueSize() {
        return taskQueue.size();
    }

    public void stop() {
        isRunning.set(false);
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public void run() {
        String workerName = Thread.currentThread().getName();
        try {
            while (isRunning.get() || !taskQueue.isEmpty()) {
                Runnable task = taskQueue.poll(keepAliveTime, timeUnit);
                if (task != null) {
                    isBusy.set(true);
                    try {
                        System.out.println("[Worker] " + workerName + " executes " + task);
                        task.run();
                    } finally {
                        isBusy.set(false);
                    }
                } else if (shouldTerminate()) {
                    System.out.println("[Worker] " + workerName + " idle timeout, stopping.");
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.removeWorker(this);
            System.out.println("[Worker] " + workerName + " terminated.");
        }
    }

    private boolean shouldTerminate() {
        int current = pool.getCurrentPoolSize();
        int busy = pool.getBusyWorkersCount();
        return current > pool.getCorePoolSize()
                && (current - busy) > pool.getMinSpareThreads();
    }
}