package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPool implements CustomExecutor {
    private final int corePoolSize;
    private final int maxPoolSize;
    private final int queueSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int minSpareThreads;
    private final ThreadFactory threadFactory;
    private final RejectedExecutionHandler rejectedHandler;

    private final List<Worker> workers = new ArrayList<>();
    private final AtomicInteger currentPoolSize = new AtomicInteger(0);
    private final AtomicInteger busyWorkers = new AtomicInteger(0);
    private volatile boolean isShutdown = false;

    public CustomThreadPool(
            int corePoolSize,
            int maxPoolSize,
            long keepAliveTime,
            TimeUnit timeUnit,
            int queueSize,
            int minSpareThreads,
            ThreadFactory threadFactory,
            RejectedExecutionHandler rejectedHandler
    ) {
        if (corePoolSize < 0 || maxPoolSize <= 0 || maxPoolSize < corePoolSize || keepAliveTime < 0)
            throw new IllegalArgumentException();

        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.queueSize = queueSize;
        this.minSpareThreads = minSpareThreads;
        this.threadFactory = threadFactory;
        this.rejectedHandler = rejectedHandler;

        for (int i = 0; i < corePoolSize; i++) {
            addWorker();
        }
    }

    private synchronized void addWorker() {
        if (currentPoolSize.get() >= maxPoolSize) return;
        Worker worker = new Worker(this, queueSize, keepAliveTime, timeUnit);
        workers.add(worker);
        Thread thread = threadFactory.newThread(worker);
        worker.setThread(thread);
        thread.start();
        currentPoolSize.incrementAndGet();
    }

    synchronized void removeWorker(Worker worker) {
        workers.remove(worker);
        currentPoolSize.decrementAndGet();
    }

    @Override
    public void execute(Runnable command) {
        if (command == null) throw new NullPointerException();
        if (isShutdown) {
            rejectedHandler.rejectedExecution(command, this);
            return;
        }

        Worker worker = findAvailableWorker();
        if (worker != null && worker.addTask(command)) {
            System.out.println("[Pool] Task accepted into queue: " + command);
            return;
        }

        if (currentPoolSize.get() < maxPoolSize) {
            addWorker();
            execute(command);
            return;
        }

        System.out.println("[Rejected] Task " + command + " was rejected due to overload!");
        rejectedHandler.rejectedExecution(command, this);
    }

    private Worker findAvailableWorker() {
        Worker bestWorker = null;
        int minTasks = Integer.MAX_VALUE;

        for (Worker worker : workers) {
            int tasks = worker.getQueueSize();
            if (tasks < minTasks && tasks < queueSize) {
                minTasks = tasks;
                bestWorker = worker;
            }
        }
        return bestWorker;
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> future = new FutureTask<>(callable);
        execute(future);
        return future;
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        workers.forEach(Worker::stop);
    }

    @Override
    public void shutdownNow() {
        isShutdown = true;
        workers.forEach(worker -> {
            worker.stop();
            Thread.currentThread().interrupt();
        });
    }

    public int getCurrentPoolSize() {
        return currentPoolSize.get();
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getMinSpareThreads() {
        return minSpareThreads;
    }

    public int getBusyWorkersCount() {
        return busyWorkers.get();
    }

    @Override
    public int getActiveCount() {
        return busyWorkers.get();
    }

    @Override
    public int getQueueSize() {
        return workers.stream().mapToInt(Worker::getQueueSize).sum();
    }
}