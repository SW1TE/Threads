package org.example;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        CustomThreadPool executor = new CustomThreadPool(
                2, // corePoolSize
                4, // maxPoolSize
                5, // keepAliveTime
                TimeUnit.SECONDS,
                5, // queueSize
                1, // minSpareThreads
                new CustomThreadFactory("MyPool"),
                (task, pool) -> {
                    System.out.println("[Rejected] Task " + task + " rejected!");
                    throw new RejectedExecutionException();
                }
        );

        int taskCount = 10;
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            try {
                executor.execute(() -> {
                    System.out.println("[Task] " + taskId + " started. Active: " +
                            executor.getActiveCount() + ", Queue: " + executor.getQueueSize());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println("[Task] " + taskId + " finished.");
                });
            } catch (RejectedExecutionException e) {
                System.out.println("[Task] " + taskId + " was rejected.");
            }
            Thread.sleep(100);
        }

        Thread.sleep(10000);
        executor.shutdown();
    }
}