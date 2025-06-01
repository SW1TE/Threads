package org.example;

public interface RejectedExecutionHandler {
    void rejectedExecution(Runnable task, CustomThreadPool executor);
}