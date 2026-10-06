package com.varun.taskmgmtapi.dto.TaskDto;

import com.varun.taskmgmtapi.models.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDate dueDate;
    private Long assignedUserId;
    private String assignedUserName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}

/*
we are not simply returning Task Obj
because Task contains private User assignedUser
and user can be assigned multiple tasks
so it can create problems such as:

Task
 ↓
User
 ↓
Task
 ↓
User
 ↓
Task
...

It can also expose fields we don't want to expose.
 */
