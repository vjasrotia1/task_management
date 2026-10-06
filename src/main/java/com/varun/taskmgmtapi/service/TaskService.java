package com.varun.taskmgmtapi.service;

import com.varun.taskmgmtapi.dto.TaskDto.CreateTaskRequest;
import com.varun.taskmgmtapi.dto.TaskDto.TaskResponse;
import com.varun.taskmgmtapi.dto.TaskDto.UpdateTaskRequest;

import java.util.List;

public interface TaskService {
    TaskResponse createTask(CreateTaskRequest createTaskRequest);
    TaskResponse getTasksById(Long id);
    List<TaskResponse> getAllTasks();
    TaskResponse UpdateTask(Long id,UpdateTaskRequest updateTaskRequest);
    void deleteTask(Long id);
    List<TaskResponse> getTasksByUserId(Long userId);
}
