package com.in.kronos;

import com.in.kronos.api.Task;
import com.in.kronos.api.TaskContext;
import com.in.kronos.core.TaskQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KronosApplication {

  public static void main(String[] args) throws InterruptedException {
    //SpringApplication.run(KronosApplication.class, args);
    System.out.println("Application starting...");

    // 1. Configure and build the JobQueue using the builder.
    TaskQueue jobQueue = new TaskQueue();

    try {
      // 2. Submit jobs to the queue.
      System.out.println("Submitting jobs...");
      for (int i = 1; i <= 5; i++) {
        CompletableFuture<String> jobId = jobQueue.submitTask(
            new EmailJob("user" + i + "@example.com"));
        System.out.printf("Submitted EmailJob with ID: %s%n",
            jobId.isDone() ? jobId.get() : "Pending");
      }

      // Submit the failing job to see how the library handles it.
      jobQueue.submitTask(new FailingJob());

      // Check queue status
      System.out.printf("Jobs waiting in queue: %d%n", jobQueue.getQueueSize());
      System.out.printf("Jobs actively running: %d%n", jobQueue.getActivetaskCount());

      // 3. Let the application run for a bit to process jobs.
      System.out.println("\n--- Main thread is now waiting for jobs to complete... ---\n");
      TimeUnit.SECONDS.sleep(5);

    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    } finally {
      // 4. CRITICAL: Always ensure shutdown is called to clean up resources.
      System.out.println("\n--- Application is shutting down. Finalizing job queue... ---");
      jobQueue.shutdown();
    }

    System.out.println("Application finished.");
  }

  public static class EmailJob implements Task {

    private final String recipient;

    public EmailJob(String recipient) {
      this.recipient = recipient;
    }

    @Override
    public void execute(TaskContext context) throws Exception {
      System.out.printf("--> [Job %s] : [Thread %s] (virtual=%s) Sending welcome email to %s...%n",
          context.taskId(),
          Thread.currentThread().getName(),
          Thread.currentThread().isVirtual(),
          recipient);
      TimeUnit.MILLISECONDS.sleep(500); // Simulate network latency
      System.out.printf("--> [Job %s] Email sent!%n", context.taskId());
    }
  }

  public static class FailingJob implements Task {

    @Override
    public void execute(TaskContext context) {
      System.out.printf("--> [Job %s] This job is about to fail...%n", context.taskId());
      throw new RuntimeException("Something went wrong during execution!");
    }
  }

}
