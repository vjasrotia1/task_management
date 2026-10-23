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

/*
policy for my project:
- USER: Can update a task assigned to them.
- ADMIN: Can update any task.
- USER: Cannot update another user's task.
- ADMIN: Can view all tasks and delete tasks
 */





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
        Authentication authenticationObj=SecurityContextHolder.getContext().getAuthentication();
            //assert  authenticationObj!=null;
        User user=(User)authenticationObj.getPrincipal();
        //assert user!=null;
        Long userId=user.getId();

        String role=authenticationObj.getAuthorities().iterator().next().getAuthority();

        boolean isAdmin=false;
        if(role.equals("ROLE_ADMIN")){
            isAdmin=true;
        }

        /*
        other way
        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
            || !(authentication.getPrincipal() instanceof User user)) {
        throw new AccessDeniedException("Current User is not authenticated");
    }

    boolean isAdmin = user.getRole() == Role.ADMIN;

    return taskService.createTask(
            createTaskRequest,
            user.getId(),
            isAdmin);
         */

            return taskService.createTask(createTaskRequest,userId,isAdmin);
    }


    @GetMapping("/{taskId}")
    public TaskResponse getTaskById(@Valid @PathVariable("taskId") Long taskId) {
        Authentication authObj=SecurityContextHolder.getContext().getAuthentication();
        assert authObj != null;
        User user=(User) authObj.getPrincipal();
        assert user != null;
        Long userId=user.getId();

        String role=authObj.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        boolean isAdmin=false;
        if(role.equals("ROLE_ADMIN")){
            isAdmin=true;
        }

        return taskService.getTasksById(taskId,userId,isAdmin);
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
    //The next step is to pass the authenticated user's ID and role from the controller
    //  and not from request parameters that the client can manipulate
    //the server will determine the current user's ID and role from Spring Security,
    //rather than trusting a user ID sent by the client.
    public TaskResponse updateTask(@PathVariable("taskId") Long taskId, @Valid @RequestBody UpdateTaskRequest updateTaskRequest) {

        Authentication authObj=SecurityContextHolder.getContext().getAuthentication();
        assert authObj != null;
        User user=(User) authObj.getPrincipal();
        assert user != null;
        Long userId=user.getId();

        //getAuthorities() contains GrantedAuthority objects, not String objects.
        // Casting one directly to String will cause a ClassCastException.

        String role=authObj.getAuthorities()
                //getAuthorities()--> gets the user's granted permissions.
                .iterator()
                .next()
                //iterator().next()-->gets the first authority
                .getAuthority();
        //retrieves its text, such as "ROLE_ADMIN" or "ROLE_USER".
        //This assumes your authentication contains at least one authority,
        // as your JWT filter should assign the user's role.


        boolean isAdmin=false;

        assert role != null;
        if(role.equals("ROLE_ADMIN")){
            isAdmin=true;
        }
        return taskService.UpdateTask(taskId,updateTaskRequest,userId,isAdmin);
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
    @PreAuthorize("hasRole('ADMIN')")
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

    //In real World, instead of requiring the userId in the URL
    //you can expose: GET/api/tasks/my--> where the server determines the User from their JWT---> as shown below
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
    public Page<TaskResponse> getMyTasks(
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "2") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortingDirection
    )
    {
    //it means : get the current authenticated request's security information, including principal and authorities
        //the server derives the identity from authenticated requestrather than trusting a userIdprovided by client
        Authentication authenticationObj =
                SecurityContextHolder.getContext().getAuthentication();

        assert authenticationObj != null;
        //Gets the principal and casts it to your User entity.
        //make sure to have following imports
        /*
        import org.springframework.security.core.Authentication;
        import org.springframework.security.core.context.SecurityContextHolder;
         */
        //we can retrive User in controller as below
        //the controller must derive these arguments from the authenticated principal and its granted authorities, never from request-body fields or arbitrary query parameters or request params.
        User user = (User) authenticationObj.getPrincipal();
        assert user != null;
        Long userId = user.getId();

        return taskService.getAllTheTasksByUserIdPageWise(userId,pageNumber,pageSize,sortBy,sortingDirection);
        /*
        other way
        import org.springframework.security.core.annotation.AuthenticationPrincipal;

        @GetMapping("/my")
        public List<TaskResponse> getMyTasks(
        @AuthenticationPrincipal User user) {

        a controller parameter such as (@AuthenticationPrincipal User currentUser) should work when this JWTfilter successfully authenticates the request and Spring Security's context is preserved.
with this  @AuthenticationPrincipal annotation, we cn get Principal without manually accessing SecurityContextHolder
Spring injects the authenticated principal into the method parameter
The user ID must come from the authenticated principal, not from a client-supplied ID that the server blindly trusts
            return taskService.getTasksByUserId(user.getId());
        }
       ALSO (retrieving tasks for the current user) is different from updating or deleting a task:
       for those operations, you must separately enforce ownership and/or the appropriate ADMIN permission.
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
