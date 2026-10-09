package com.varun.taskmgmtapi.controller;


import com.varun.taskmgmtapi.dto.TaskDto.CreateTaskRequest;
import com.varun.taskmgmtapi.dto.TaskDto.TaskResponse;
import com.varun.taskmgmtapi.dto.TaskDto.UpdateTaskRequest;
import com.varun.taskmgmtapi.dto.TaskDto.UpdateTaskStatusRequest;
import com.varun.taskmgmtapi.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private TaskService taskService;
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@Valid @RequestBody CreateTaskRequest createTaskRequest) {
            return taskService.createTask(createTaskRequest);
    }


    @GetMapping("/{taskId}")
    public TaskResponse getTaskById(@Valid @PathVariable("taskId") Long taskId) {
        return taskService.getTasksById(taskId);
    }
    //The variable names taskId and userId do not make the URLs different.
    //so spring says: Ambiguous handler methods mapped for '/api/tasks/2'
    //so Give the two APIs different URL patterns.


//    @GetMapping("/user/{userId}")
//    public List<TaskResponse> getAllTasksByUserId(@Valid @PathVariable("userId") Long userId) {
//
//        return taskService.getTasksByUserId(userId);
//
//    }


//    @GetMapping
//    //PAGINATION: our goal is to change GET /api/tasks to
//    // GET /api/tasks?page=0&size=10&sortBy=createdAt&direction=desc
//    //because we dont want to return potentially every task at once


//    public List<TaskResponse> getAllTasks() {
//        return  taskService.getAllTasks();
//    }

    @PatchMapping("/{taskId}")
    public TaskResponse updateTask(@Valid @RequestBody UpdateTaskRequest updateTaskRequest, @PathVariable("taskId") Long taskId) {

        return taskService.UpdateTask(taskId, updateTaskRequest);
    }

    @DeleteMapping("/{taskId}")
    public void deleteTaskById(@Valid @PathVariable("taskId") Long taskId) {
        taskService.deleteTask(taskId);
    }

    @PatchMapping("/{taskId}/assign/{userId}")
    public TaskResponse assignTask(@PathVariable("taskId") Long taskId, @PathVariable("userId") Long userId) {
        return taskService.assignTask(taskId, userId);
    }

    @PatchMapping("/{taskId}/status")
    public TaskResponse changeTaskStatus(@PathVariable("taskId") Long taskId, @Valid @RequestBody UpdateTaskStatusRequest updateTaskStatusRequest) {

        return taskService.changeTaskStatus(taskId, updateTaskStatusRequest);
    }

    @GetMapping
    public Page<TaskResponse> getAllTheTasksPagewise(
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "2") int pageSize,
            @RequestParam(defaultValue= "createdAt") String sortByColumn,
            @RequestParam(defaultValue= "desc") String directionCriteria
    )
    {
        return taskService.getAllTheTasksPagewise(
                pageNumber,
                pageSize,
                sortByColumn,
                directionCriteria);
    }

    @GetMapping("/user/{userId}")
    public Page<TaskResponse> getAllTheTasksforUserPagewise(
            @PathVariable("userId") Long userId,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "2") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortingDirection
    )

    {
        return taskService.getAllTheTasksByUserIdPageWise(userId,
                pageNumber,pageSize,sortBy,sortingDirection);
    }

}

/*
complete flow

Postman
   │
   │ POST /api/tasks
   ↓
Security Filter
   │
   │ JWT validation
   ↓
TaskController
   │
   │ CreateTaskRequest
   ↓
TaskService
   │
   │ business logic
   ↓
TaskRepository
   │
   │ JPA
   ↓
MySQL


response travels back

MySQL
   ↓
Task entity
   ↓
TaskService
   ↓
TaskResponse DTO
   ↓
TaskController
   ↓
JSON response
 */
