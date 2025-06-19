package com.in.kronos.core;

import com.in.kronos.api.Task;
import com.in.kronos.api.TaskContext;
import jakarta.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * A robust, in-memory, FIFO task queue and executor. This is the main entry point for the library.
 * <p>
 * Use the {@link Builder} to configure and create an instance.
 */

@Slf4j
public class TaskQueue {

  private final BlockingQueue<TaskWrapper> queue;
  private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
  private final AtomicInteger activeTaskCount = new AtomicInteger(0);


  public TaskQueue() {
    this.queue = new LinkedBlockingQueue<>();
    executorService.submit(new Worker(queue));
  }

  /**
   * Submits a new task to the queue for asynchronous execution.
   *
   * @param task The task to be executed.
   * @return A unique ID for the submitted task, which can be used for tracking.
   */
  public CompletableFuture<String> submitTask(Task task) {
    CompletableFuture<String> future = new CompletableFuture<>();
    executorService.submit(() -> {
      activeTaskCount.incrementAndGet();
      try {
        TaskContext context = new TaskContext(UUID.randomUUID().toString());
        task.execute(context);
        future.complete(context.taskId());
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        activeTaskCount.decrementAndGet();
      }
    });
    return future;
  }

  /**
   * Initiates a graceful shutdown of the task queue. Previously submitted tasks are executed, but
   * no new tasks will be accepted. This method waits for all active tasks to complete.
   */
  @PreDestroy
  public void shutdown() {
    log.info("queue shutdown initiated...");
    executorService.shutdown(); // Disable new tasks from being submitted
    try {
      // Wait a while for existing tasks to terminate
      if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
        executorService.shutdownNow(); // Cancel currently executing tasks
        if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
          log.error("Executor service did not terminate.");
        }
      }
    } catch (InterruptedException ie) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
    log.info("queue shutdown complete.");
  }

  /**
   * Gets the number of tasks currently waiting in the queue.
   *
   * @return The number of tasks pending execution.
   */
  public int getQueueSize() {
    return queue.size();
  }

  /**
   * Gets the number of threads that are actively executing tasks.
   *
   * @return The number of active threads.
   */
  public int getActivetaskCount() {
    return activeTaskCount.get();
  }
}
