package com.varun.taskmgmtapi.models;

//this rep the workflow req by assignment
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE
}

/*
USER
│
├── Register
├── Login
├── View tasks(all or own)
├── Create task
└── Update own task


ADMIN
│
├── All USER permissions
├── Assign tasks
├── Delete tasks
└── Manage users/tasks

                    JWT validation done
                     ↓
              Authentication object
                     ↓
             SecurityContext holds authentication obj
                     ↓
              ┌──────┴──Authorisation part────┐
              ↓             ↓
             USER          ADMIN
              ↓             ↓
        User operations   Admin operations
 */
