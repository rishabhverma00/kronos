package com.in.kronos.task;

public interface Task<T> {
  void execute(T context);
  String taskName();
}
