package com.in.kronos.core;

import com.in.kronos.api.Task;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
  private final ThreadPoolExecutor executorService;

  @Value("${kronos.thread.pool.size}")
  private Integer threadPoolSize;
  private static final int DEFAULT_THREAD_POOL_SIZE = 2; // Default size if not configured

  public TaskQueue() {
    this.queue = new LinkedBlockingQueue<>();
    this.executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(
        Objects.isNull(threadPoolSize) ? DEFAULT_THREAD_POOL_SIZE : threadPoolSize);

    // Start worker threads. They will immediately block on the queue, waiting for tasks.
    for (int i = 0;
        i < (Objects.isNull(threadPoolSize) ? DEFAULT_THREAD_POOL_SIZE : threadPoolSize); i++) {
      executorService.submit(new Worker(queue));
    }
  }

  /**
   * Submits a new task to the queue for asynchronous execution.
   *
   * @param task The task to be executed.
   * @return A unique ID for the submitted task, which can be used for tracking.
   */
  public String submitTask(Task task) {
    String taskId = UUID.randomUUID().toString();
    TaskWrapper taskWrapper = new TaskWrapper(taskId, task);
    boolean offered = queue.offer(taskWrapper);
    if (!offered) {
      // This case is unlikely with a LinkedBlockingQueue unless we hit memory limits.
      // A more robust implementation might throw an exception here.
      log.error(
          "FATAL: Failed to add task to the queue. The queue may be full or corrupted.");
    }
    return taskId;
  }

  /**
   * Initiates a graceful shutdown of the task queue. Previously submitted tasks are executed, but
   * no new tasks will be accepted. This method waits for all active tasks to complete.
   */
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
    return executorService.getActiveCount();
  }
}
