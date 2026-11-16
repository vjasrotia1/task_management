package com.varun.taskmgmtapi.ServiceImpl;

import com.varun.taskmgmtapi.dto.TaskDto.TaskResponse;
import com.varun.taskmgmtapi.exception.ResourceNotFoundException;
import com.varun.taskmgmtapi.models.Task;
import com.varun.taskmgmtapi.repository.TaskRepo;
import com.varun.taskmgmtapi.service.TaskCacheService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service //spring detects this class during component scanning and registers it as a bean
//we will test the complete Redis caching flow using Redis CLI. We'll also verify that cache eviction happens only after a successful database transaction commit
//redis Testing will be done by using Redis CLI-->
// "docker exec -it task-mgmt-redis redis-cli"
/*
complete the cache invalidation checks for all four task operations:
UPDATE: cached task is evicted.
STATUS CHANGE: cached task is evicted.
DELETE: cached task is evicted.
ASSIGN TASK: cached task is evicted.

after above: next step is improving cache configuration with TTL (Time To Live).
 */
public class RedisTaskCacheServiceImpl implements TaskCacheService {

    private final TaskRepo taskRepo;

    RedisTaskCacheServiceImpl(TaskRepo taskRepo) {
        this.taskRepo = taskRepo;
    }

    @Override
    @Cacheable(value = "tasks", key = "#taskId")
    //cacheable --> Fetch from Redis or MySQL and cache the response.
    public TaskResponse getTaskResponse(Long taskId) {

        System.out.println("Fetching task from MySQL for taskId: " + taskId );

        Task task = taskRepo.findById(taskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Task with id " + taskId + " not found"
                        ));

        return convertToResponse(task);
        //- No "Fetching task from MySQL" message from the cacheable method
        // if the existence check rejects the request before the cache service is called.

    }

    @CacheEvict(value = "tasks", key = "#taskId")
    public void evictTaskCache(Long taskId) {
        System.out.println("Evicting task from Redis cache with id: " + taskId);
        /*
        cacheEvict --> It removes the corresponding task from Redis.
        For example:
Cache key	Action
tasks::5	Deleted
We need to call taskCacheService.evictTaskCache(taskId) after successful database updates.
In our TaskServiceImpl, the relevant methods are:
- updateTask()
- changeTaskStatus()
- deleteTask()
We'll add cache eviction at the correct point, preserving our existing authorization and transaction logic


        Why do we need cache invalidation?
Imagine this sequence:
1. You fetch task ID 5.
2. Redis stores its response with status TODO.
3. You update the task status in MySQL to DONE.
4. You fetch task ID 5 again.
5. Redis still returns TODO!
This is called stale cache data.

We need to remove or refresh the cached response whenever a task changes.
         */
    }

    private TaskResponse convertToResponse(Task task) {
        Long assignedUserId = null;
        String assignedUserName = null;

        if (task.getAssignedUser() != null) {
            assignedUserId = task.getAssignedUser().getId();
            assignedUserName = task.getAssignedUser().getName();
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
        //Now we'll connect TaskCacheServiceImpl to your existing TaskServiceImpl.
    }
}
    /*- value = "tasks": Redis cache name.
- key = "#taskId": uses the task ID as the cache key.
For example:
Request	Redis key
Task ID 5	tasks::5
Task ID 10	tasks::10

First request for task 5:
- Redis checks tasks::5.
- If absent, Spring executes the method.
- MySQL fetches the task.
- Result is stored in Redis.
Second request for task 5:
- Redis finds tasks::5.
- Spring returns the cached result.
- The method does not execute again.- value = "tasks": Redis cache name.
- key = "#taskId": uses the task ID as the cache key.
For example:
Request	Redis key
Task ID 5	tasks::5
Task ID 10	tasks::10

First request for task 5:
- Redis checks tasks::5.
- If absent, Spring executes the method.
- MySQL fetches the task.
- Result is stored in Redis.
Second request for task 5:
- Redis finds tasks::5.
- Spring returns the cached result.
- The method does not execute again.

     */

