package com.varun.taskmgmtapi.dto.TaskDto;

import com.varun.taskmgmtapi.models.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateTaskRequest {

    @NotBlank(message = "taskName is required")
    private String taskName;
    private String description;
    @NotNull(message = "status is required")
    private TaskStatus status;
    private LocalDate dueDate;
}
/*
client can update :
title
description
status
dueDate


if FE snds {"status" : "DONE" }
Spring/Jackson converts: it to TaskStatus.DONE
Spring Boot uses Jackson to convert the incoming JSON into your Java DTO.
Jackson effectively does --TaskStatus.valueOf("DONE") and gets TaskStatus.DONE

FE sends JSON "in progress"
Jackson converts it to UpdateTaskRequest request
where internally request.getStatus() returns TaskStatus.DONE
therefore service can safely do : Task.setStatus(request.getStatus())

Frontend
   |
   | JSON
   | "DONE"
   ↓
Spring Boot
   |
   | Jackson converts JSON
   ↓
UpdateTaskStatusRequest
   |
   | status = TaskStatus.DONE
   ↓
Your Controller
 */
