package com.varun.taskmgmtapi.ServiceImpl;

import com.varun.taskmgmtapi.dto.TaskDto.CreateTaskRequest;
import com.varun.taskmgmtapi.dto.TaskDto.TaskResponse;
import com.varun.taskmgmtapi.dto.TaskDto.UpdateTaskRequest;
import com.varun.taskmgmtapi.dto.TaskDto.UpdateTaskStatusRequest;
import com.varun.taskmgmtapi.exception.ResourceNotFoundException;
import com.varun.taskmgmtapi.models.Task;
import com.varun.taskmgmtapi.models.User;
import com.varun.taskmgmtapi.repository.TaskRepo;
import com.varun.taskmgmtapi.repository.UserRepo;
import com.varun.taskmgmtapi.service.TaskService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class TaskServiceImpl implements TaskService {
    private TaskRepo  taskRepo;
    private UserRepo userRepo;
    public TaskServiceImpl(TaskRepo taskRepo, UserRepo userRepo) {
        this.taskRepo = taskRepo;
        this.userRepo=userRepo;
    }

    @Override
    @Transactional
    public TaskResponse createTask(CreateTaskRequest createTaskRequest) {
        /*
        suppose the business requirement says task titles must be unique
        if(taskRepo.existsByTitle(createTaskRequest.getTaskName())){
        throw new ResourceAlreadyExistsException("task already exists with name : "+createTaskRequest.getTaskName());
        }
         */
        Task task=new Task();
        task.setTitle(createTaskRequest.getTaskName());
        task.setDescription(createTaskRequest.getTaskDescription());
        task.setDueDate(createTaskRequest.getDueDate());

        Task savedTask=taskRepo.save(task);

        return convertToResponse(savedTask);
    }

    //this function is basically converting Task to TaskResponse
    private TaskResponse convertToResponse(Task task) {
        //koi bhi Task Obj ko TaskResponse obj me kasie convert kare?
        Long assignedUserId=null;
        String assignedUserName=null;

        if(task.getAssignedUser()!=null) {
            assignedUserId=task.getAssignedUser().getId();
            assignedUserName=task.getAssignedUser().getName();
        }

        return new TaskResponse(
                    task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate(),
                assignedUserId,
                assignedUserName,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );

    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTasksById(Long id) {
        Task task=taskRepo.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Task with id: " + id + " not found!"));
        //here  java is saying : "I'm giving you a small piece of code. Don't execute it now.
        // Execute it only if the Optional is empty."
        //When you need the exception, create this exception or else if Optional is not empty return Task
        return convertToResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        List<Task> tasks=taskRepo.findAll();
        List<TaskResponse> taskResponses=new ArrayList<>();
        for(Task task:tasks){
            TaskResponse response=convertToResponse(task);
            taskResponses.add(response);
        }
        return taskResponses;
        /*
        Task 1 → convertToResponse() → TaskResponse 1
        Task 2 → convertToResponse() → TaskResponse 2
        Task 3 → convertToResponse() → TaskResponse 3
         */
    }


    @Override
    @Transactional
    public TaskResponse UpdateTask(Long id,UpdateTaskRequest updateTaskRequest) {
        //We could technically put the ID inside UpdateTaskRequest
        //but in REST APIs--> it is cleaner to keep
        //URL → identifies the resource
        //Body → contains the changes

        Task task=taskRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task with id: " + id + " not found!"));

        task.setTitle(updateTaskRequest.getTaskName());
        task.setDescription(updateTaskRequest.getDescription());
        task.setDueDate(updateTaskRequest.getDueDate());
        task.setStatus(updateTaskRequest.getStatus());

        Task UpdatedTask=taskRepo.save(task);
        return convertToResponse(UpdatedTask);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        Task task=taskRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task with id: " + id + " not found!"));

        taskRepo.delete(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByUserId(Long userId) {

        List<TaskResponse> taskResponses=new ArrayList<>();
        List<Task>tasks=taskRepo.findByAssignedUserId(userId);

        for(Task task : tasks){
            TaskResponse response=convertToResponse(task);
            taskResponses.add(response);
        }
        return taskResponses;

    }

    @Override
    @Transactional
    //@Transactional is the Safety Supervisor standing over the Hibernate.
    // It ensures that either the entire job is finished successfully or nothing happens at all.
    //• With @Transactional: If any error occurs midway,
    // Spring and Hibernate will Rollback (undo) everything.
    // If everything succeeds, it will Commit (permanently save) the changes.

    public TaskResponse assignTask(Long taskId, Long userId) {
        Task task=taskRepo.findById(taskId)
                .orElseThrow(()->
                        new ResourceNotFoundException("Task with id "+taskId+" not found!"));

        User user= userRepo.findById(userId)
                .orElseThrow(()->
                        new ResourceNotFoundException("User with id "+userId+" not found!"));

        //assign the task to the user
        task.setAssignedUser(user);

        //taskRepo.save(task);
        //no need to write above line because we are inside @Transactional and task is a managed JPA Entity
        //when we do above : task.setAssignedUser(user); Hibernate detects that the entity changed
        //at the time of transaction commit, it performs :"Update Tasks SET assigned_user_id=102 where id=1;
        //This is called dirty checking.
        //u can still keep it, but for a managed entity loaded inside the transaction, it isn't strictly necessary
        taskRepo.save(task);

        return convertToResponse(task);
        /*
        Hibernate dirty checking is an automatic optimization mechanism that tracks changes made to managed entities and synchronizes them with the database without requiring you to explicitly call update() or save() statements.
The term "dirty" simply refers to any entity whose in-memory state has changed or diverged from its original database state during a transaction

The entire mechanism relies on Hibernate's Persistence Context (the first-level cache) and operates in a clear lifecycle:
1. Snapshot Creation: When you retrieve an entity from the database (e.g., using session.get() or a JPQL query), Hibernate loads it into memory and immediately creates a raw snapshot copy of its original field values.
2. In-Memory Modification: As your application logic updates fields on that Java object (e.g., user.setEmail("new@email.com")), only the live object changes. The background snapshot stays the same.
3. The Flush Process: Before a transaction commits (or when you call session.flush()), Hibernate scans the persistence context.
4. Comparison & SQL Generation: Hibernate iterates through all managed entities, matching the current state of each property against its original snapshot.
If any properties differ, the entity is marked "dirty," and Hibernate automatically constructs and executes the required SQL UPDATE statement.
         */
    }

    @Override
    @Transactional
    public TaskResponse changeTaskStatus(Long taskId, UpdateTaskStatusRequest updateTaskStatusRequest) {

        Task task=taskRepo.findById(taskId)
                .orElseThrow(()->
                        new ResourceNotFoundException("Task with id: "+taskId+" not found!"));

                task.setStatus(updateTaskStatusRequest.getStatus());
        return convertToResponse(task);
    }

    private static  final Set<String> ALLOWED_SORT_FIELDS=
            Set.of(
                    "id",
                    "title",
                    "status",
                    "dueDate",
                    "createdAt",
                    "updatedAt"
            );

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTheTasksPagewise(int pageNumber, int size, String sortbycolumn, String sortDirection) {

        if(pageNumber<0) {
            throw new IllegalArgumentException("Page number can't be negative");
        }
        if(size<=0 || size>100) {
            throw new IllegalArgumentException("Page Size must be between 1 and 100");
        }

        if(!ALLOWED_SORT_FIELDS.contains(sortbycolumn)) {
            throw new IllegalArgumentException("Invalid Sort field: " + sortbycolumn);
        }
        if(!sortDirection.equals("asc") && !sortDirection.equals("desc")) {
            throw new IllegalArgumentException("Direction must either be asc or desc");
        }


        //Creating the Sorting Configuration object(sorting rules)
        //it creates sorting instructions : e.g. CreatedAt Desc
        Sort sort=Sort.by(Sort.Direction.fromString(sortDirection), sortbycolumn);

        //creating Pagination request
        Pageable pageable = PageRequest.of(pageNumber, size, sort);
        //it tells JPA : "Give me page 0, maximum 5 records, sorted by createdAt descending."
        //Pageable is an interface used by Spring Data JPA to understand database pagination.
        //PageRequest.of(...) packages your page number, page size, and sorting rules into a single setup object.

        Page<Task> tasksPage=taskRepo.findAll(pageable);
        //• This triggers the actual SQL query to your database.
        /*
        • This triggers the actual SQL query to your database.
        • Behind the scenes, Spring Data JPA translates this into a SQL query using LIMIT and OFFSET clauses
        (e.g., SELECT * FROM tasks ORDER BY CreatedAt DESC LIMIT 10 OFFSET 0).

        • It returns a page containing raw database entities (Task).

        Instead of just sending back an array of tasks,
        Spring Boot automatically calculates details like total counts
        so your front-end application knows how to build pagination buttons.

        calculation is like this :
        OFFSET = pageNumber× pageSize
       = 1 × 5
       = 5
         */
        List<TaskResponse> taskResponseList=new ArrayList<>();

        for(Task task:tasksPage.getContent()){
            TaskResponse taskResponse=convertToResponse(task);
            taskResponseList.add(taskResponse);
        }

        return new PageImpl<>(taskResponseList,pageable,tasksPage.getTotalElements());
        //return tasks.map(this::convertToResponse);
        //above is the short form
        /*
        {

        if u make a network request to this endpoint,
        the JSON response looks like this.
        Notice how your actual items are tucked inside content, while everything else is the metadata:
  "content": [
    { "id": 1, "title": "Buy groceries", "status": "PENDING" },
    { "id": 2, "title": "Clean room", "status": "COMPLETED" }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 2
  },
  "totalPages": 5,
  "totalElements": 10,
  "last": false,
  "size": 2,
  "number": 0,
  "numberOfElements": 2,
  "first": true,
  "empty": false
}
         */

    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTheTasksByUserIdPageWise(Long userId,
                                                             int pageNumber,
                                                             int pageSize,
                                                             String sortBy,
                                                             String direction) {

        Sort sort=Sort.by(Sort.Direction.fromString(direction), sortBy);

        Pageable pageable=PageRequest.of(pageNumber,pageSize,sort);

        Page<Task> tasksPageforUser=taskRepo.findByAssignedUserId(userId,pageable);

        List<TaskResponse> taskResponseList=new ArrayList<>();

        for(Task task: tasksPageforUser.getContent()){
            taskResponseList.add(convertToResponse(task));
        }

        return new PageImpl<>(taskResponseList,pageable,tasksPageforUser.getTotalElements());
        //this method is saying "Don't give me all tasks. Give me tasks belonging to this particular user, and paginate them.

    }
}
