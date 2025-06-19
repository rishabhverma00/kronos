package com.in.kronos.api;

@FunctionalInterface
public interface Task {
  void execute(TaskContext context) throws Exception;
}
