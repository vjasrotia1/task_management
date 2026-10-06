package com.varun.taskmgmtapi.repository;
//we dont write  @Repository
// because Spring Data JPA automatically creates and registers the repository as a Spring bean
// when it sees that it extends JpaRepository

import com.varun.taskmgmtapi.models.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepo extends JpaRepository<Task, Long> {
    //by writing above line only, we automatically get methods like:
            //save(task), findById(id), findAll(),deleteById(id),existsById(id)- so we dont have to write sql ourselves
    //Spring Data JPA generates the appropriate SQL behind the scenes.

    List<Task> findByAssignedUser(Long userId);
    /*
    one thing to check for above method is that
    It must have an appropriate query/method definition.
     */

}
