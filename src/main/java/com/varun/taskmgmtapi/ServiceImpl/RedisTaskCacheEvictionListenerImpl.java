package com.varun.taskmgmtapi.ServiceImpl;

import com.varun.taskmgmtapi.events.TaskCacheEvictionEvent;
import com.varun.taskmgmtapi.service.TaskCacheEvictionListener;
import com.varun.taskmgmtapi.service.TaskCacheService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
//@component-->it Marks a single generic bean component.
public class RedisTaskCacheEvictionListenerImpl implements TaskCacheEvictionListener {

    private final TaskCacheService taskCacheService;

    public RedisTaskCacheEvictionListenerImpl(TaskCacheService taskCacheService) {
        this.taskCacheService = taskCacheService;
    }

    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    //@TransactionalEventListener- this annotation tells spring "Wait for the database transaction to finish."
    //If the transaction commits successfully, execute this listener.- If the transaction rolls back, don't execute it.
    public void handleTaskCacheEviction(TaskCacheEvictionEvent event) {

        taskCacheService.evictTaskCache(event.taskId());
    }
}
