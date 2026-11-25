package com.varun.taskmgmtapi.repository;

import com.varun.taskmgmtapi.models.Role;
import com.varun.taskmgmtapi.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
//it means create a Repository for an entity 'User' whose primary key DataType is LONG
public interface UserRepo extends JpaRepository<User, Long> {
//we use Optional bec, user might not exist
    Optional<User> findByEmail(String email);
    //corresponding to above, spring creates a sql query
    //select * from users where email=? so basically spring data JPA performs database lookup

    boolean existsByEmail(String email);
    //conceptually the query here will be : select EXISTS(select 1 from users where email=?);

    //spring data JPA gives us following methods directly:
    //we dont hv to write SQL ourselves for these basic operations
    /*
    1.userRepository.save(user);
    2.userRepository.findById(1L);
    3.userRepository.findAll();
    4. userRepository.deleteById(1L);
     */

    long countByRole(Role role);

//Conceptually, Hibernate generates a query similar to:SELECT *
//FROM users
//ORDER BY id ASC
//FOR UPDATE;
// this is the important part: the database holds the locks until the transaction commits or rolls back.

//
//Your comment about ascending ID order is sensible. It can help reduce deadlock risk, but it does not guarantee that deadlocks are impossible.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<User> findAllByOrderByIdAsc();
    //Why are we locking all users?
    //Imagine two ADMINs attempt to demote each other simultaneously.
    //1. Request A obtains write locks on the user rows.
    //2. Request B must wait for those locks.
    //3. Request A checks the ADMIN count and demotes one user.
    //4. Request A commits its transaction.
    //5. Request B obtains the locks and checks the updated ADMIN count.
    //6. Request B sees only one ADMIN and cannot demote that last ADMIN.
    //The ORDER BY id ASC gives both requests a consistent row ordering, helping reduce deadlock risk.
    //his deliberately serializes role updates and locks all existing user rows. It's a reasonable approach for a small application's infrequent role-management operations, though a dedicated lock row would be more scalable for a larger system.
    //
    //Also, the locking query must execute inside the existing transaction.

}
