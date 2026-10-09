package com.varun.taskmgmtapi.dto.TaskDto;

import com.varun.taskmgmtapi.models.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTaskStatusRequest {

    @NotNull(message = "status is required")
    private TaskStatus status;
}
