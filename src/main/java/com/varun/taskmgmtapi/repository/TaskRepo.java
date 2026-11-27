package com.varun.taskmgmtapi.repository;
//we dont write  @Repository
// because Spring Data JPA automatically creates and registers the repository as a Spring bean
// when it sees that it extends JpaRepository

import com.varun.taskmgmtapi.models.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepo extends JpaRepository<Task, Long> {
    //by writing above line only, we automatically get methods like:
            //save(task), findById(id), findAll(),deleteById(id),existsById(id)- so we dont have to write sql ourselves
    //Spring Data JPA generates the appropriate SQL behind the scenes.

    List<Task> findByAssignedUserId(Long userId);
    /*
    one thing to check for above method is that
    It must have an appropriate query/method definition.
     */

    //Spring data JPA provides :
    //Page<Task> findAll(Pageable pageable);
    //through JPA repository

    //Go to the database, find all the tasks assigned to a specific user ID,
    // and give me back just one specific page of those tasks along with the pagination information
    Page<Task> findByAssignedUserId(Long userId, Pageable pageable);
    //Suppose the user requests:
    //- Tasks assigned to user ID 10
    //- Sorted by createdAt
    //- Newest tasks first
    //A composite index on:
    //(assigned_user_id, created_at)
    //could help MySQL with both filtering and sorting.
    //Importantly, this composite index can also serve queries that filter only by assigned_user_id, because that is its first column.
    //What happens when a USER requests their tasks?
    //For example:
    //GET /api/tasks/my?pageNumber=0&pageSize=2&sortBy=createdAt&sortingDirection=desc
    //Your repository executes the equivalent of:
    //SELECT *
    //FROM tasks
    //WHERE assigned_user_id = 10
    //ORDER BY created_at DESC
    //LIMIT 2 OFFSET 0;
    //MySQL needs to:
    //1. Find tasks assigned to user 10.
    //2. Sort those tasks by creation date, newest first.
    //3. Return the requested page.
    //Your existing assigned_user_id index helps with step 1.

    //A composite index can potentially help with both steps 1 and 2.
    //Consider this composite index:
    //(assigned_user_id, created_at)
    //It is particularly relevant to your most common user-specific query.
    //Your existing standalone assigned_user_id index may become redundant because the composite index starts with the same column.
    //However, we should verify actual MySQL indexes before removing anything.

    //both above filter tasks are using assigned_user_id
    //@Index(name = "idx_task_assigned_user", columnList = "assigned_user_id")
    //This index is useful for finding tasks belonging to a particular user. The paginated version benefits from it too.

    boolean existsByIdAndAssignedUser_Id(Long taskId, Long userId);
    //This checks whether a task with a particular ID belongs to a particular user.
    //No additional index is needed.
    //
    //Why? id is already the primary key, and MySQL can locate a task using its primary-key index. It can then check the assigned_user_id value.
    //Spring Data JPA automatically generates the database query from the method name.
    //It checks whether:
    //- A task exists with the given taskId.
    //- That task is assigned to the given userId

}
/*
Page<Task> (The Result): "I want the results wrapped inside a Page container.
This means don't just give me the List<task>;
also calculate the metadata for me (like the total # of tasks in the database and how many pages exist total).


findByAssignedUserId
Look at the Task table inside the database,
find the column named assignedUser (or assigned_user_id), and filter the records to match what I pass in."

Pageable pageable= PageRequest.of(pageNumber,pageSize,sortObj);
It is basically (The Page settings):
"This tells the database which page I want to see (e.g., Page 1)
and how many items to show on that page (e.g., 10 items), as well as how to sort them."

 */


/*
Spring Data is an umbrella project under the Spring Framework ecosystem designed to radically simplify how Java applications interact with databases.
Instead of writing tedious, repetitive database boilerplate code—like manually opening database connections, handling SQL exceptions, or writing standard CRUD (Create, Read, Update, Delete) queries—Spring Data lets you manage data using simple Java interfaces.

💡 The Core Concepts of Spring Data


1. Repository-Driven Development

The most powerful feature of Spring Data is the Repository abstraction. You define a basic interface, and Spring automatically generates all the database logic behind the scenes at runtime:
java
// You just write this interface...
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Spring automatically implements standard methods like:
    // save(), findById(), findAll(), deleteById(), and count()!
}
Use code with caution.

2. Query Generation from Method Names

Spring Data can actually read the name of a Java method you write and automatically construct the corresponding database query.
java
// Spring parses this name and automatically generates a SQL WHERE clause:
List<Task> findByStatusAndDueDateBefore(String status, LocalDate date);
Use code with caution.
(Behind the scenes, Spring creates: SELECT * FROM tasks WHERE status = ? AND due_date < ?)

3. Seamless Pagination and Sorting

As seen in your previous code snippet, Spring Data handles advanced database operations like offset pagination (Pageable) and sorting (Sort) out of the box with virtually zero manual configuration.

🗄️ Different Spring Data Modules

Because different applications use different types of databases, Spring Data provides specialized modules for almost every popular data store. The programming model remains almost identical across all of them:
• Spring Data JPA: For relational databases using Hibernate (e.g., PostgreSQL, MySQL, Oracle).
• Spring Data MongoDB: For NoSQL document-based storage.
• Spring Data Redis: For fast, key-value, in-memory caching.
• Spring Data Elasticsearch: For advanced text searching.

⚖️ Analogy: The Restaurant Waiter

Think of standard database connectivity (JDBC) as walking into a kitchen, hunting down the raw ingredients, cooking the meal yourself, and washing the dishes.
Spring Data is like having a professional waiter. You look at a simple menu (the Repository interface), point to what you want (call a method like findAll()), and the waiter handles the entire backend kitchen process, delivering the food (your data objects) perfectly formatted to your table.
 */

//Review of your three existing indexes
//A. idx_task_assigned_user
//Keep
//This is important.
//
//Your application frequently fetches tasks belonging to a particular user. The index helps MySQL locate those tasks without scanning the entire tasks table.
//B. idx_task_created_at
//Keep for now
//Your pagination supports sorting by createdAt.
//
//An index on created_at can help when MySQL needs to sort tasks by creation date.
//C. idx_task_status
//Review
//I noticed something important.
//
//Your repository doesn't currently have a query that filters tasks using status.
//
//You do allow sorting by status, but sorting alone doesn't necessarily justify a separate index.
//
//Also, your application has only three possible statuses:
//- TODO
//- IN_PROGRESS
//- DONE
//This is a low-cardinality column, so a standalone index may provide limited benefit.


/*
SHOW CREATE TABLE tasks;
SHOW COLUMNS FROM tasks LIKE 'version';
# Hibernate should create this column automatically if your schema configuration uses ddl-auto=update.
SELECT id, status, version
FROM tasks
WHERE id = 4;
What happens when concurrent updates occur?
Hibernate automatically:
- Reads the current version.
- Checks that version during an update.
- Increments it after a successful update.
- Throws an optimistic locking exception if another transaction has already updated the row.
One important point: We should eventually translate that exception into HTTP 409 Conflict, rather than exposing an internal server error.
This is our baseline:
Task ID	Status	Version
4	TODO	0

Step 5 — Verify version increment
Using Swagger, call:

PATCH /api/tasks/4/status

Request body:
{
  "status": "IN_PROGRESS"
}


If successful, run:
SELECT id, status, version
FROM tasks
WHERE id = 4;


Expected result:
Task ID	Status	Version
4	IN_PROGRESS	1
Handle concurrent update conflicts
If two requests try to update the same task using the same version, the second update should receive:

HTTP 409 — Conflict

Let's handle this gracefully instead of returning HTTP 500.
 */
