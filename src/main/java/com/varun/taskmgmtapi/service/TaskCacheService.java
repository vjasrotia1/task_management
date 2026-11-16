package com.varun.taskmgmtapi.service;

import com.varun.taskmgmtapi.dto.TaskDto.TaskResponse;

public interface TaskCacheService {
    TaskResponse getTaskResponse(Long taskId);

    void evictTaskCache(Long taskId);

}
