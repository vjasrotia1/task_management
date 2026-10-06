package com.varun.taskmgmtapi.controller;


import com.varun.taskmgmtapi.models.Task;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("/api/tasks")
public class TaskController {

    @GetMapping("/{id}")
    public List<Task> getAllTasks(@PathVariable Long id) {
        return null;

    }


}
