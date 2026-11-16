package com.varun.taskmgmtapi.service;

import com.varun.taskmgmtapi.events.TaskCacheEvictionEvent;

public interface TaskCacheEvictionListener {

    void handleTaskCacheEviction(TaskCacheEvictionEvent event);

}
