package org.example;

import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadFactory implements java.util.concurrent.ThreadFactory {
    private final String namePrefix;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final ThreadGroup group;

    public CustomThreadFactory(String poolName) {
        this.group = Thread.currentThread().getThreadGroup();
        this.namePrefix = poolName + "-worker-";
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement());
        t.setDaemon(false);
        t.setPriority(Thread.NORM_PRIORITY);
        System.out.println("[ThreadFactory] Creating new thread: " + t.getName());
        return t;
    }
}