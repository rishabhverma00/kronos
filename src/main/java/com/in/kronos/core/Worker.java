package com.in.kronos.core;

import com.in.kronos.api.TaskContext;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Worker implements Runnable {

  private final BlockingQueue<TaskWrapper> queue;

  Worker(BlockingQueue<TaskWrapper> queue) {
    this.queue = queue;
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        TaskWrapper taskWrapper = queue.take(); // Blocks until a job is available
        TaskContext context = new TaskContext(taskWrapper.getId());

        log.info("Worker {} picked up task {}. Starting execution.", Thread.currentThread().getName(), context.taskId());

        // CRITICAL: Catch all exceptions from the job's execution to prevent the worker thread from dying.
        try {
          taskWrapper.getTask().execute(context);
        }  catch (Throwable t) {
          log.error("ERROR: task {} failed with an exception", context.taskId(), t);
        }

        log.info("Worker {} finished task {}", Thread.currentThread().getName(), context.taskId());

      } catch (InterruptedException e) {
        // This exception is expected during a graceful shutdown.
        // We re-interrupt the thread to ensure the loop condition is false and the thread exits cleanly.
        Thread.currentThread().interrupt();
        break;
      }
    }
    log.info("Worker {} is shutting down.", Thread.currentThread().getName());

  }
}
