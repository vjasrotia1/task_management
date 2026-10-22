package com.varun.taskmgmtapi.service;

import com.varun.taskmgmtapi.dto.TaskDto.CreateTaskRequest;
import com.varun.taskmgmtapi.dto.TaskDto.TaskResponse;
import com.varun.taskmgmtapi.dto.TaskDto.UpdateTaskRequest;
import com.varun.taskmgmtapi.dto.TaskDto.UpdateTaskStatusRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TaskService {
    TaskResponse createTask(CreateTaskRequest createTaskRequest, Long currentUserId, boolean isAdmin);
    TaskResponse getTasksById(Long taskId,Long currentUserId,boolean isAdmin);
    List<TaskResponse> getAllTasks();
    TaskResponse UpdateTask(Long taskId,UpdateTaskRequest updateTaskRequest,Long currentUserId,boolean isAdmin);
    void deleteTask(Long id);
    List<TaskResponse> getTasksByUserId(Long userId);
    TaskResponse assignTask(Long taskId,Long userId);
    TaskResponse changeTaskStatus(Long taskId, UpdateTaskStatusRequest updateTaskStatusRequest);
    Page<TaskResponse> getAllTheTasksPagewise(int pageNumber, int size,String sortBy,String sortDirection);
    Page<TaskResponse> getAllTheTasksByUserIdPageWise(Long userId, int pageNumber, int pageSize, String sortBy, String direction);
}
