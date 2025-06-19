package com.in.kronos.api;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
/**
 * Provides metadata and context to a running {@link Task}.
 * An instance of this class is passed to the {@link Task#execute()} (JobContext)} method.
 */
public class TaskContext {
  private String taskId;
}
