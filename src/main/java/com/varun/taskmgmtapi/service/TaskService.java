package com.varun.taskmgmtapi.service;

import com.varun.taskmgmtapi.dto.TaskDto.CreateTaskRequest;
import com.varun.taskmgmtapi.dto.TaskDto.TaskResponse;
import com.varun.taskmgmtapi.dto.TaskDto.UpdateTaskRequest;
import com.varun.taskmgmtapi.dto.TaskDto.UpdateTaskStatusRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TaskService {
    TaskResponse createTask(CreateTaskRequest createTaskRequest);
    TaskResponse getTasksById(Long id);
    List<TaskResponse> getAllTasks();
    TaskResponse UpdateTask(Long id,UpdateTaskRequest updateTaskRequest);
    void deleteTask(Long id);
    List<TaskResponse> getTasksByUserId(Long userId);
    TaskResponse assignTask(Long taskId,Long userId);
    TaskResponse changeTaskStatus(Long taskId, UpdateTaskStatusRequest updateTaskStatusRequest);
    Page<TaskResponse> getAllTheTasksPagewise(int pageNumber, int size,String sortBy,String sortDirection);
    Page<TaskResponse> getAllTheTasksByUserIdPageWise(Long userId, int pageNumber, int pageSize, String sortBy, String direction);
    TaskResponse UpdateTheTask(Long taskId, UpdateTaskRequest updateTaskRequest);
}
