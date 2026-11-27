package com.varun.taskmgmtapi.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity
//@Entity tells JPA that this java class is the database Table
@Table(name="users",
indexes = {
        @Index(name="idx_user_email",columnList = "email")
})
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false,unique = true)
    //unique=true, means Mysql creates a unique index for email column
    //database constraints provide the final protection against race conditions.
    //concept of DataIntegrityViolationException
    //although while registering user, we are checking if email already exists or not-- that is primarily for sending clean API response
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    //Because of Lombok's @Getter, Jackson could potentially serialize the password hash if a User entity is accidentally returned directly from a controller.
    //Eventually this field will contain a BCrypt hash,
    //not the actual password.
    //as we never store plain text passwords in DB
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    //without this annotation, JPA can store enum as number 0,1
    //so we use EnumType.STRING
    //in java Role.USER, in DB --> "USER"
    private Role role=Role.USER;

    @Column(name= "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "assignedUser")
    //with which field in (Task entity) will this attribute of User will get mapped to
    //it means Task.assignedUser field/attribute in (Tasks entity) owns this relationship
    //This prevents JPA from creating unnecessary relationship tables/columns
    //on the User side.
    private List<Task> tasks=new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
//Now, about N+1 queries
//Your Task entity has:
//@ManyToOne(fetch = FetchType.LAZY)
//@JoinColumn(name = "assigned_user_id")
//private User assignedUser;
//
//
//When convertToResponse(task) executes:
//task.getAssignedUser().getId();
//task.getAssignedUser().getName();
//
//
//Hibernate may fetch the assigned user's details separately for each task.
//
//For example, if a page contains 10 tasks belonging to 10 different users:
//- 1 query fetches the tasks.
//- Up to 10 additional queries fetch assigned-user details.
//This is the potential N+1 query problem.
//
//But we shouldn't assume it is happening. Hibernate's first-level cache can avoid repeated queries for the same user.
//@EntityGraph optimization is worthwhile for N+1 query problem. We won't change the code until we verify the behavior.