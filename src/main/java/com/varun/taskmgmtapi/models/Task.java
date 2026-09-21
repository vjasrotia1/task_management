package com.varun.taskmgmtapi.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(
        name="tasks",
        indexes = {
                @Index(name="idx_task_status",columnList = "status"),
                @Index(name="idx_task_assigned_user", columnList = "assigned_user_id"),
                @Index(name="idx_task_created_at", columnList = "created_at")
        }
)
public class Task {

    @Id
    //in DB --> id BIGINT PRIMARY KEY AUTO_INCREMENT
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    //it means a newly created task starts as TODO
    private TaskStatus status=TaskStatus.TODO;

    @Column(name="due_date")
    //Later, our DTO validation can enforce:
    //@FutureOrPresent
    //so users can't create a task with a date in the past.
    private LocalDate dueDate;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="assigned_user_id")
    //@JoinCoumn -- tells JPA to "Create a foreign-key column in the tasks table called assigned_user_id."
    //fetch type -- lazy means " Don't immediately load the entire User object when loading a Task unless it's actually needed.
    private User assignedUser;


}
