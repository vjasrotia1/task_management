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
