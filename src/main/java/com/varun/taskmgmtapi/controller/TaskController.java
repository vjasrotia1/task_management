package com.varun.taskmgmtapi.controller;


import com.varun.taskmgmtapi.dto.TaskDto.CreateTaskRequest;
import com.varun.taskmgmtapi.dto.TaskDto.TaskResponse;
import com.varun.taskmgmtapi.dto.TaskDto.UpdateTaskRequest;
import com.varun.taskmgmtapi.dto.TaskDto.UpdateTaskStatusRequest;
import com.varun.taskmgmtapi.models.User;
import com.varun.taskmgmtapi.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
//        return taskService.getAllTasks();
//    }

    @PatchMapping("/{taskId}")
    public TaskResponse updateTask(@Valid @RequestBody UpdateTaskRequest updateTaskRequest, @PathVariable("taskId") Long taskId) {

        return taskService.UpdateTask(taskId, updateTaskRequest);
    }
    @PreAuthorize("hasRole('ADMIN')") //because only admin can delete the task
    @DeleteMapping("/{taskId}")
    public void deleteTaskById(@Valid @PathVariable("taskId") Long taskId) {
        taskService.deleteTask(taskId);
        /*
        now with @PreAuthorize Spring Security will check if logged in User has role of ADMIN or not
        Is logged-in user ROLE_ADMIN?
       ↓
      YES → allow
       ↓
       NO → 403 Forbidden
         */
    }

    @PatchMapping("/{taskId}/assign/{userId}")
    public TaskResponse assignTask(@PathVariable("taskId") Long taskId, @PathVariable("userId") Long userId) {
        return taskService.assignTask(taskId, userId);
    }

    @PatchMapping("/{taskId}/status")
    public TaskResponse changeTaskStatus(@PathVariable("taskId") Long taskId, @Valid @RequestBody UpdateTaskStatusRequest updateTaskStatusRequest) {

        return taskService.changeTaskStatus(taskId, updateTaskStatusRequest);
    }

    // ADMIN: View all tasks
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
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

    //Suppose you want an endpoint that returns tasks belonging to the currently logged-in user.
    //Instead of requiring the user ID in the URL, u can simply use "/my",because The server determines the user from their JWT.
    //We'll make the API identify the logged-in user automatically, so the client doesn't need to send their user ID.

    // USER: View their own tasks
    @GetMapping("/my")
    public List<TaskResponse> getMyTasks() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assert authentication != null;
        User user = (User) authentication.getPrincipal();
        Long userId = user.getId();

        return taskService.getTasksByUserId(userId);
        /*
        other way
        import org.springframework.security.core.annotation.AuthenticationPrincipal;

        @GetMapping("/my")
        public List<TaskResponse> getMyTasks(
        @AuthenticationPrincipal User user) {

            return taskService.getTasksByUserId(user.getId());
        }
         */
        //This example assumes your JWTfilter uses the User entity as its principal
        //Neither user chooses whose identity the server uses.
        //The server derives the "identity" from the authenticated request/object rather than trusting a user ID supplied by the client.

        //One more important distinction:
        // being authenticated doesn't automatically mean a user owns a particular task.
        // For operations such as (updating a task),
        //you should also verify task ownership in your service unless the user has an explicitly permitted ADMIN privilege.
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
