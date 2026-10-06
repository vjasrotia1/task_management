package com.varun.taskmgmtapi.ServiceImpl;

import com.varun.taskmgmtapi.dto.TaskDto.CreateTaskRequest;
import com.varun.taskmgmtapi.dto.TaskDto.TaskResponse;
import com.varun.taskmgmtapi.dto.TaskDto.UpdateTaskRequest;
import com.varun.taskmgmtapi.exception.ResourceNotFoundException;
import com.varun.taskmgmtapi.models.Task;
import com.varun.taskmgmtapi.repository.TaskRepo;
import com.varun.taskmgmtapi.service.TaskService;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

public class TaskServiceImpl implements TaskService {
    private TaskRepo  taskRepo;
    public TaskServiceImpl(TaskRepo taskRepo) {
        this.taskRepo = taskRepo;
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
        List<Task>tasks=taskRepo.findByAssignedUser(userId);

        for(Task task : tasks){
            TaskResponse response=convertToResponse(task);
            taskResponses.add(response);
        }
        return taskResponses;

    }
}
