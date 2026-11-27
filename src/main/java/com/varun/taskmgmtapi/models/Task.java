package com.varun.taskmgmtapi.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Version;
@Entity
@Getter
@Setter
@Table(
        name="tasks",
        indexes = {
                @Index(name="idx_task_status",columnList = "status"),
                @Index(name="idx_task_created_at", columnList = "created_at"),
                @Index(name = "idx_task_assigned_created", columnList = "assigned_user_id, created_at")
                //The composite index can also support queries filtering only by assigned_user_id, because that column is its leftmost column.
                //Therefore, the standalone index is likely redundant.
                //Changing (Task.java) does not automatically guarantee that Hibernate removes an existing index when using ddl-auto=update
        }

)
public class Task {

    @Version
    //optimistic locking
    private Long version;
    //JPA's @Version detects when another transaction has modified the same task and prevents a silent overwrite.
//Hibernate reads the task and its version.
//When updating, Hibernate checks whether the version is still unchanged
//If another transaction updated the task first, the second update fails with an optimistic locking exception instead of silently overwriting data.
    @Id
    //in DB --> id BIGINT PRIMARY KEY AUTO_INCREMENT
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING) //enumerated means "status will be stored in DB as string"
    //because of this,
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
    //@JoinColumn -- tells JPA to "Create a foreign-key column(called assigned_user_id) in the tasks table."
    //fetch type -- lazy means "    Don't immediately load the entire User object when loading a Task unless it's actually needed.
    //Task owns the @ManyToOne relationship through : @JoinColumn(name="assigned_user_id")
    private User assignedUser;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt=LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    //because of above method, timestamps get populated




}

//tasks table currently has exactly these four indexes:
//Index	Column	Purpose
//PRIMARY	id	Fast lookup by task ID
//idx_task_status	status	Status-based filtering
//idx_task_assigned_user	assigned_user_id	Finding tasks assigned to a user
//idx_task_created_at	created_at	Sorting by creation date

//Currently, MySQL has separate indexes for assigned_user_id and created_at.
//
//A composite index can support both filtering and sorting together:
//(assigned_user_id, created_at)
//
//
//Why this order?
//1. assigned_user_id comes first because it is used in the WHERE condition.
//2. created_at comes second because it is used in ORDER BY.
//Make this one change in Task.java
//Inside your existing @Table annotation, add:
//@Index(
//    name = "idx_task_assigned_created",
//    columnList = "assigned_user_id, created_at"
//)

//EXPLAIN
//SELECT *
//FROM tasks
//WHERE assigned_user_id = 10
//ORDER BY created_at DESC
//LIMIT 2 OFFSET 0;
//1,SIMPLE,tasks,,ref,idx_task_assigned_created,idx_task_assigned_created,9,const,1,100,Backward index scan
//Excellent! Composite index is being used
//
//Let's understand your EXPLAIN output:
//
//Column
//
//Your result
//
//Meaning
//
//type
//
//ref
//
//MySQL is using an index to find matching rows.
//
//possible_keys
//
//idx_task_assigned_created
//
//This index is a candidate.
//
//key
//
//idx_task_assigned_created
//
//This is the index MySQL actually selected.
//
//ref
//
//const
//
//It compares the indexed column against a constant (10).
//
//rows
//
//1
//
//MySQL estimates examining one row.
//
//Extra
//
//Backward index scan
//
//MySQL scans the index backwards to satisfy created_at DESC.
//
//The most important result is key:
//
//idx_task_assigned_created
//This confirms that MySQL is using our composite index for filtering and sorting.
//
//Also, Backward index scan is not an error. MySQL can scan an ascending index backwards to support descending order.
//

//EXPLAIN
//SELECT *
//FROM tasks
//ORDER BY created_at DESC
//LIMIT 2 OFFSET 0;
//What does this mean?
//- ALL: MySQL is performing a full table scan.
//- key = NULL: It is not using any index.
//- Using filesort: MySQL is performing a separate sorting operation rather than retrieving rows directly in the required order from an index.
//Is this a problem?
//Not necessarily. Your tasks table currently has only 2 estimated rows.
//
//For a very small table, MySQL may decide that scanning and sorting the table is cheaper than using an index. This is called a cost-based optimizer decision.
//
//With a much larger table, the optimizer may choose idx_task_created_at.
//Let's verify whether the index can support this query
//Run:
//EXPLAIN
//SELECT *
//FROM tasks FORCE INDEX (idx_task_created_at)
//ORDER BY created_at DESC
//LIMIT 2 OFFSET 0;
//
//
//FORCE INDEX is only for this experiment. It does not permanently change your database or application code.
//Excellent! This confirms that your idx_task_created_at index can support the admin pagination query.
//
//Let's compare the two execution plans.
//Property	Without FORCE INDEX	With FORCE INDEX
//type	ALL	index
//key	NULL	idx_task_created_at
//Extra	Using filesort	Backward index scan
//Sorting	Separate sorting	Index order
//
//What have we learned?
//1. Your created_at index is working correctly.
//2. MySQL can retrieve records in descending order directly from the index.
//3. MySQL chose a full table scan earlier because the table is very small (estimated 2 rows).
//4. FORCE INDEX demonstrated the alternative execution plan; we should not add it to your application query.