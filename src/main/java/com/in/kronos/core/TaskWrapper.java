package com.in.kronos.core;

/**
 * wrapper class to hold a Job and its metadata (e.g., its ID).
 */

import com.in.kronos.api.Task;

public class TaskWrapper {

  private final String id;
  private final Task task;

  TaskWrapper(String id, Task task) {
    this.id = id;
    this.task = task;
  }

  public String getId() {
    return id;
  }

  public Task getTask() {
    return task;
  }
}
