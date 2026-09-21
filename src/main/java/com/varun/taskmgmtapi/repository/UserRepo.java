package com.varun.taskmgmtapi.repository;

import com.varun.taskmgmtapi.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
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


}
