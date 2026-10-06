package com.varun.taskmgmtapi.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    //database constraints provide the final protection against race conditions.
    //concept of DataIntegrityViolationException
    //although while registering user, we are checking if email already exists or not-- that is primarily for sending clean API response
    private String email;

    @Column(nullable = false)
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
    //it means Task.assignedUser field in (Tasks entity)
    //owns this relationship
    //This prevents JPA from creating unnecessary relationship tables/columns
    //on the User side.
    private List<Task> tasks=new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
