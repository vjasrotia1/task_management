package com.varun.taskmgmtapi.controller;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import com.varun.taskmgmtapi.repository.UserRepo;
import com.varun.taskmgmtapi.models.User;
import com.varun.taskmgmtapi.models.Role;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql-test")
//@Transactional
public class TaskApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepo userRepo;

    @Test
    void userCreatesTask_taskShouldBeAssignedToLoggedInUser() throws Exception {

        // Step 1: Register a USER
        MvcResult registerResult = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "name": "Task Test User",
                                "email": "tasktest@test.com",
                                "password": "password123"
                            }
                    """)
                ).andExpect(status().isCreated())
                .andReturn();

        // Step 2: Extract the registered user's ID
        String registerResponse =
                registerResult.getResponse().getContentAsString();

        Long userId = Long.valueOf(
                JsonPath.read(registerResponse, "$.id").toString()
        );

        // Step 3: Login
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "email": "tasktest@test.com",
                                "password": "password123"
                            }
                    """)
                ).andExpect(status().isOk())
                .andReturn();

        // Step 4: Extract JWT
        String loginResponse =
                loginResult.getResponse().getContentAsString();

        String token = JsonPath.read(loginResponse, "$.token");

        // Step 5: Create a task without assignedUserId
        mockMvc.perform(
                        post("/api/tasks")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "taskName": "Complete Integration Testing",
                                "taskDescription": "Testing automatic task assignment"
                            }
                    """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title")
                        .value("Complete Integration Testing"))
                .andExpect(jsonPath("$.assignedUserId")
                        .value(userId.intValue()))
                .andExpect(jsonPath("$.assignedUserName")
                        .value("Task Test User"));
    }

    @Test
    void adminCreatesTaskAndAssignsToAnotherUser() throws Exception {

        // STEP 1: Register an ADMIN account
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Admin Assignment",
                                "email": "adminassign15@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Promote the registered account to ADMIN
        User admin = userRepo.findByEmail("adminassign15@test.com")
                .orElseThrow();

        admin.setRole(Role.ADMIN);
        userRepo.save(admin);

        // STEP 3: Register a normal USER
        MvcResult userResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Assigned User 15",
                                "email": "assigned15@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated())
                .andReturn();

        // STEP 4: Extract the USER's ID
        Long assignedUserId = Long.valueOf(
                JsonPath.read(
                        userResult.getResponse().getContentAsString(),
                        "$.id"
                ).toString()
        );

        // STEP 5: Login as ADMIN
        MvcResult adminLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "adminassign15@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isOk())
                .andReturn();

        String adminToken = JsonPath.read(
                adminLogin.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 6: ADMIN creates a task and assigns it to the USER
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "Admin Assigned Task",
                                "taskDescription": "Created by ADMIN for another USER",
                                "assignedUserId": %d
                            }
                            """.formatted(assignedUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Admin Assigned Task"))
                .andExpect(jsonPath("$.assignedUserId").value(assignedUserId))
                .andExpect(jsonPath("$.assignedUserName").value("Assigned User 15"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void userCanRetrieveOwnTask() throws Exception {

        // Step 1: Register a USER
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Retrieve Test User",
                                "email": "retrieve@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 2: Login
        MvcResult loginResult = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "retrieve@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isOk()).andReturn();

        // Step 3: Extract JWT
        String loginResponse =
                loginResult.getResponse().getContentAsString();

        String token = JsonPath.read(loginResponse, "$.token");

        // Step 4: Create a task
        MvcResult createResult = mockMvc.perform(
                post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "My Personal Task",
                                "taskDescription": "Testing task retrieval"
                            }
                    """)
        ).andExpect(status().isCreated()).andReturn();

        // Step 5: Extract the created task ID
        String createResponse =
                createResult.getResponse().getContentAsString();

        Long taskId = Long.valueOf(
                JsonPath.read(createResponse, "$.id").toString()
        );

        // Step 6: Retrieve the task using its ID
        mockMvc.perform(
                        get("/api/tasks/" + taskId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.intValue()))
                .andExpect(jsonPath("$.title").value("My Personal Task"));
    }

    @Test
    void userCannotRetrieveAnotherUsersTask() throws Exception {

        // Step 1: Register User A
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User A",
                                "email": "usera@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 2: Login User A
        MvcResult loginA = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "usera@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isOk()).andReturn();

        String tokenA = JsonPath.read(
                loginA.getResponse().getContentAsString(), "$.token"
        );

        // Step 3: User A creates a task
        MvcResult createResult = mockMvc.perform(
                post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "User A Private Task",
                                "taskDescription": "Only User A should access this"
                            }
                    """)
        ).andExpect(status().isCreated()).andReturn();

        // Step 4: Extract the task ID
        Long taskId = Long.valueOf(
                JsonPath.read(
                        createResult.getResponse().getContentAsString(),
                        "$.id"
                ).toString()
        );

        // Step 5: Register User B
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User B",
                                "email": "userb@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 6: Login User B
        MvcResult loginB = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "userb@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isOk()).andReturn();

        String tokenB = JsonPath.read(
                loginB.getResponse().getContentAsString(), "$.token"
        );

        // Step 7: User B attempts to retrieve User A's task
        mockMvc.perform(
                get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenB)
        ).andExpect(status().isForbidden());
    }

    @Test
    void adminCanRetrieveAnotherUsersTask() throws Exception {

        // Step 1: Register an account for ADMIN
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Admin User",
                                "email": "admin5@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 2: Promote this account to ADMIN for test setup
        User admin = userRepo.findByEmail("admin5@test.com")
                .orElseThrow();

        admin.setRole(Role.ADMIN);
        userRepo.save(admin);

        // Step 3: Register a normal USER
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Normal User",
                                "email": "normal5@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 4: Login as normal USER
        MvcResult userLogin = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "normal5@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isOk()).andReturn();

        String userToken = JsonPath.read(
                userLogin.getResponse().getContentAsString(),
                "$.token"
        );

        // Step 5: USER creates a task
        MvcResult createResult = mockMvc.perform(
                post("/api/tasks")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "Normal User Task",
                                "taskDescription": "ADMIN should be able to access this"
                            }
                    """)
        ).andExpect(status().isCreated()).andReturn();

        Long taskId = Long.valueOf(
                JsonPath.read(
                        createResult.getResponse().getContentAsString(),
                        "$.id"
                ).toString()
        );

        // Step 6: Login as ADMIN
        MvcResult adminLogin = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "admin5@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isOk()).andReturn();

        String adminToken = JsonPath.read(
                adminLogin.getResponse().getContentAsString(),
                "$.token"
        );

        // Step 7: ADMIN retrieves the USER's task
        mockMvc.perform(
                        get("/api/tasks/" + taskId)
                                .header("Authorization", "Bearer " + adminToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.title").value("Normal User Task"));
    }

    @Test
    void userCannotAssignTaskToAnotherUser() throws Exception {

        // Step 1: Register User A
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User A",
                                "email": "usera6@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 2: Login User A
        MvcResult loginA = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "usera6@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isOk()).andReturn();

        String tokenA = JsonPath.read(
                loginA.getResponse().getContentAsString(), "$.token"
        );

        // Step 3: Register User B
        MvcResult registerB = mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User B",
                                "email": "userb6@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated()).andReturn();

        Long userBId = Long.valueOf(
                JsonPath.read(
                        registerB.getResponse().getContentAsString(), "$.id"
                ).toString()
        );

        // Step 4: User A attempts to create a task assigned to User B
        MvcResult createResult = mockMvc.perform(
                        post("/api/tasks")
                                .header("Authorization", "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "taskName": "Assignment Security Test",
                                "taskDescription": "User A tries to assign this to User B",
                                "assignedUserId": %d
                            }
                            """.formatted(userBId))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignedUserId").isNotEmpty())
                .andReturn();

        // Step 5: Verify that the task belongs to User A
        Long assignedUserId = Long.valueOf(
                JsonPath.read(
                        createResult.getResponse().getContentAsString(),
                        "$.assignedUserId"
                ).toString()
        );

        Long userAId = userRepo.findByEmail("usera6@test.com")
                .orElseThrow()
                .getId();

        org.junit.jupiter.api.Assertions.assertEquals(userAId, assignedUserId);
    }

    @Test
    void userCannotDeleteTask() throws Exception {

        // Step 1: Register USER
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Delete Test User",
                                "email": "deleteuser7@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 2: Login USER
        MvcResult loginResult = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "deleteuser7@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isOk()).andReturn();

        String token = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // Step 3: USER creates a task
        MvcResult createResult = mockMvc.perform(
                post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "Task to protect",
                                "taskDescription": "USER should not delete this"
                            }
                    """)
        ).andExpect(status().isCreated()).andReturn();

        // Step 4: Extract task ID
        Long taskId = Long.valueOf(
                JsonPath.read(
                        createResult.getResponse().getContentAsString(),
                        "$.id"
                ).toString()
        );

        // Step 5: USER attempts to delete the task
        mockMvc.perform(
                delete("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
        ).andExpect(status().isForbidden());

        // Step 6: Verify the task still exists
        mockMvc.perform(
                        get("/api/tasks/" + taskId)
                                .header("Authorization", "Bearer " + token)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId));
    }

    @Test
    void adminCanDeleteTask() throws Exception {

        // Step 1: Register an account for ADMIN
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Admin User",
                                "email": "admindelete8@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 2: Promote account to ADMIN for test setup
        User admin = userRepo.findByEmail("admindelete8@test.com")
                .orElseThrow();

        admin.setRole(Role.ADMIN);
        userRepo.save(admin);

        // Step 3: Login as ADMIN
        MvcResult loginResult = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "admindelete8@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isOk()).andReturn();

        String token = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // Step 4: ADMIN creates a task
        MvcResult createResult = mockMvc.perform(
                post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "Admin Delete Test",
                                "taskDescription": "ADMIN will delete this task"
                            }
                    """)
        ).andExpect(status().isCreated()).andReturn();

        // Step 5: Extract task ID
        Long taskId = Long.valueOf(
                JsonPath.read(
                        createResult.getResponse().getContentAsString(),
                        "$.id"
                ).toString()
        );

        // Step 6: ADMIN deletes the task
        mockMvc.perform(
                delete("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
        ).andExpect(status().isNoContent());

        // Step 7: Verify task no longer exists
        mockMvc.perform(
                get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
        ).andExpect(status().isNotFound());
    }
    @Test
    void userCannotUpdateAnotherUsersTask() throws Exception {

        // Step 1: Register User A
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User A",
                                "email": "usera9@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 2: Login User A
        MvcResult loginA = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "usera9@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isOk()).andReturn();

        String tokenA = JsonPath.read(
                loginA.getResponse().getContentAsString(), "$.token"
        );

        // Step 3: User A creates a task
        MvcResult createResult = mockMvc.perform(
                post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "Original Task",
                                "taskDescription": "Original description"
                            }
                    """)
        ).andExpect(status().isCreated()).andReturn();

        Long taskId = Long.valueOf(
                JsonPath.read(
                        createResult.getResponse().getContentAsString(),
                        "$.id"
                ).toString()
        );

        // Step 4: Register User B
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User B",
                                "email": "userb9@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 5: Login User B
        MvcResult loginB = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "userb9@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isOk()).andReturn();

        String tokenB = JsonPath.read(
                loginB.getResponse().getContentAsString(), "$.token"
        );

        // Step 6: User B attempts to update User A's task
        mockMvc.perform(
                patch("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "Hacked Task",
                                "taskDescription": "Modified by User B",
                                 "status": "IN_PROGRESS"
                            }
                    """)
        ).andExpect(status().isForbidden());

        // Step 7: Verify the original task remains unchanged
        mockMvc.perform(
                        get("/api/tasks/" + taskId)
                                .header("Authorization", "Bearer " + tokenA)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Original Task"))
                .andExpect(jsonPath("$.description").value("Original description"));
    }


    @Test
    void adminCanUpdateAnotherUsersTask() throws Exception {

        // Step 1: Register ADMIN account
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Admin",
                                "email": "admin10@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 2: Register USER account
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User",
                                "email": "user10@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 3: Promote ADMIN account using repository
        User admin = userRepo.findByEmail("admin10@test.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepo.save(admin);

        // Step 4: Login ADMIN
        MvcResult adminLogin = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "admin10@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isOk()).andReturn();

        String adminToken = JsonPath.read(
                adminLogin.getResponse().getContentAsString(), "$.token"
        );

        // Step 5: Login USER
        MvcResult userLogin = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "user10@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isOk()).andReturn();

        String userToken = JsonPath.read(
                userLogin.getResponse().getContentAsString(), "$.token"
        );

        // Step 6: USER creates a task
        MvcResult createResult = mockMvc.perform(
                post("/api/tasks")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "Original Task",
                                "taskDescription": "Original description"
                            }
                    """)
        ).andExpect(status().isCreated()).andReturn();

        Long taskId = Long.valueOf(
                JsonPath.read(
                        createResult.getResponse().getContentAsString(), "$.id"
                ).toString()
        );

        // Step 7: ADMIN updates USER's task
        mockMvc.perform(
                        patch("/api/tasks/" + taskId)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "taskName": "Updated by Admin",
                                "description": "Admin changed this task",
                                "status": "IN_PROGRESS"
                            }
                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated by Admin"))
                .andExpect(jsonPath("$.description").value("Admin changed this task"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void userCanUpdateOwnTask() throws Exception {

        // Step 1: Register USER
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User",
                                "email": "user11@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 2: Login USER
        MvcResult loginResult = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "user11@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isOk()).andReturn();

        String token = JsonPath.read(
                loginResult.getResponse().getContentAsString(), "$.token"
        );

        // Step 3: USER creates a task
        MvcResult createResult = mockMvc.perform(
                post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "Original Task",
                                "taskDescription": "Original description"
                            }
                    """)
        ).andExpect(status().isCreated()).andReturn();

        Long taskId = Long.valueOf(
                JsonPath.read(
                        createResult.getResponse().getContentAsString(), "$.id"
                ).toString()
        );

        // Step 4: USER updates their own task
        mockMvc.perform(
                        patch("/api/tasks/" + taskId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "taskName": "My Updated Task",
                                "description": "Updated by task owner",
                                "status": "IN_PROGRESS"
                            }
                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Updated Task"))
                .andExpect(jsonPath("$.description").value("Updated by task owner"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void userCannotChangeAnotherUsersTaskStatus() throws Exception {

        // Step 1: Register User A
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User A",
                                "email": "usera12@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 2: Login User A
        MvcResult loginA = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "usera12@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isOk()).andReturn();

        String tokenA = JsonPath.read(
                loginA.getResponse().getContentAsString(), "$.token"
        );

        // Step 3: User A creates a task
        MvcResult createResult = mockMvc.perform(
                post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "User A Task",
                                "taskDescription": "Original task"
                            }
                    """)
        ).andExpect(status().isCreated()).andReturn();

        Long taskId = Long.valueOf(
                JsonPath.read(
                        createResult.getResponse().getContentAsString(), "$.id"
                ).toString()
        );

        // Step 4: Register User B
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User B",
                                "email": "userb12@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 5: Login User B
        MvcResult loginB = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "userb12@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isOk()).andReturn();

        String tokenB = JsonPath.read(
                loginB.getResponse().getContentAsString(), "$.token"
        );

        // Step 6: User B attempts to change User A's task status
        mockMvc.perform(
                patch("/api/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "status": "IN_PROGRESS"
                            }
                    """)
        ).andExpect(status().isForbidden());

        // Step 7: Verify User A's task status remains unchanged
        mockMvc.perform(
                        get("/api/tasks/" + taskId)
                                .header("Authorization", "Bearer " + tokenA)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TODO"));
    }


    @Test
    void userCanChangeOwnTaskStatus() throws Exception {

        // Step 1: Register USER
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User",
                                "email": "user13@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 2: Login USER
        MvcResult loginResult = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "user13@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isOk()).andReturn();

        String token = JsonPath.read(
                loginResult.getResponse().getContentAsString(), "$.token"
        );

        // Step 3: USER creates a task
        MvcResult createResult = mockMvc.perform(
                post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "My Task",
                                "taskDescription": "Task for status update"
                            }
                    """)
        ).andExpect(status().isCreated()).andReturn();

        Long taskId = Long.valueOf(
                JsonPath.read(
                        createResult.getResponse().getContentAsString(), "$.id"
                ).toString()
        );

        // Step 4: USER changes their task status
        mockMvc.perform(
                        patch("/api/tasks/" + taskId + "/status")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "status": "IN_PROGRESS"
                            }
                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Step 5: Verify updated status
        mockMvc.perform(
                        get("/api/tasks/" + taskId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }


    @Test
    void adminCanChangeAnotherUsersTaskStatus() throws Exception {

        // STEP 1: Register an ADMIN user
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Admin 14",
                                "email": "admin14@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Promote the registered user to ADMIN
        // We use UserRepo directly to set up the ADMIN role for this integration test.
        User admin = userRepo.findByEmail("admin14@test.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepo.save(admin);

        // STEP 3: Register a normal USER
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User 14",
                                "email": "user14@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 4: Login as the normal USER and obtain JWT
        MvcResult userLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "user14@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isOk())
                .andReturn();

        String userToken = JsonPath.read(
                userLoginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 5: USER creates a task
        MvcResult createTaskResult = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "User 14 Task",
                                "taskDescription": "Task created by User 14"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andReturn();

        Long taskId = Long.valueOf(
                JsonPath.read(
                        createTaskResult.getResponse().getContentAsString(),
                        "$.id"
                ).toString()
        );

        // STEP 6: Login as ADMIN and obtain JWT
        MvcResult adminLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "admin14@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isOk())
                .andReturn();

        String adminToken = JsonPath.read(
                adminLoginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 7: ADMIN changes the USER's task status
        mockMvc.perform(patch("/api/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "status": "IN_PROGRESS"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // STEP 8: USER retrieves the task and verifies the updated status
        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void userCannotAccessTasksWithoutAuthentication() throws Exception {

        // STEP 1: Attempt to retrieve personal tasks without a JWT
        mockMvc.perform(get("/api/tasks/my"))
                .andExpect(status().isUnauthorized());

        // STEP 2: Attempt to create a task without a JWT
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "Unauthorised Task",
                                "taskDescription": "This should not be created"
                            }
                            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotAccessAdminTaskEndpoints() throws Exception {

        // STEP 1: Register a normal USER
        MvcResult registerResult = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "name": "Normal User 17",
                                        "email": "normal17@test.com",
                                        "password": "password123"
                                    }
                                    """)
                )
                .andExpect(status().isCreated())
                .andReturn();

        // STEP 2: Extract USER ID
        Long userId = Long.valueOf(
                JsonPath.read(
                        registerResult.getResponse().getContentAsString(),
                        "$.id"
                ).toString()
        );

        // STEP 3: Login as USER
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "normal17@test.com",
                                        "password": "password123"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andReturn();

        String userToken = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 4: USER creates a task
        MvcResult createResult = mockMvc.perform(
                        post("/api/tasks")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "taskName": "User 17 Task",
                                        "taskDescription": "Testing ADMIN-only endpoints"
                                    }
                                    """)
                )
                .andExpect(status().isCreated())
                .andReturn();

        // STEP 5: Extract task ID
        Long taskId = Long.valueOf(
                JsonPath.read(
                        createResult.getResponse().getContentAsString(),
                        "$.id"
                ).toString()
        );

        // STEP 6: USER attempts to retrieve all tasks
        mockMvc.perform(
                        get("/api/tasks")
                                .header("Authorization", "Bearer " + userToken)
                )
                .andExpect(status().isForbidden());

        // STEP 7: USER attempts to assign the task through ADMIN-only endpoint
        mockMvc.perform(
                        patch("/api/tasks/" + taskId + "/assign/" + userId)
                                .header("Authorization", "Bearer " + userToken)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanRetrieveOnlyTheirOwnTasks() throws Exception {

        // STEP 1: Register USER A
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User A 18",
                                "email": "usera18@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Register USER B
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User B 18",
                                "email": "userb18@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 3: Login USER A
        MvcResult loginA = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "usera18@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isOk())
                .andReturn();

        String tokenA = JsonPath.read(
                loginA.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 4: Login USER B
        MvcResult loginB = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "userb18@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isOk())
                .andReturn();

        String tokenB = JsonPath.read(
                loginB.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 5: USER A creates a task
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "User A Private Task 18",
                                "taskDescription": "This task belongs to USER A"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 6: USER B creates a task
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "User B Private Task 18",
                                "taskDescription": "This task belongs to USER B"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 7: USER A retrieves their own tasks

// STEP 7: USER A retrieves their own tasks
        mockMvc.perform(get("/api/tasks/my")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title")
                        .value("User A Private Task 18"))
                .andExpect(jsonPath("$.content[0].assignedUserName")
                        .value("User A 18"));
    }

    @Test
    void adminCanRetrieveAllTasks() throws Exception {

        // STEP 1: Register USER A
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User A 19",
                                "email": "usera19@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Register USER B
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User B 19",
                                "email": "userb19@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 3: Register ADMIN
        MvcResult adminRegisterResult = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "name": "Admin 19",
                                        "email": "admin19@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isCreated())
                .andReturn();

        Integer adminIdValue = JsonPath.read(
                adminRegisterResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long adminId = adminIdValue.longValue();
        // STEP 4: Promote the registered user to ADMIN
        User admin = userRepo.findById(adminId).orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepo.save(admin);

        // STEP 5: Login USER A
        MvcResult loginA = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "usera19@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String tokenA = JsonPath.read(
                loginA.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 6: Login USER B
        MvcResult loginB = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "userb19@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String tokenB = JsonPath.read(
                loginB.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 7: USER A creates a task
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "User A Task 19",
                                "taskDescription": "Task belonging to USER A"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 8: USER B creates a task
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "User B Task 19",
                                "taskDescription": "Task belonging to USER B"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 9: Login ADMIN
        MvcResult adminLogin = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "admin19@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String adminToken = JsonPath.read(
                adminLogin.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 10: ADMIN retrieves all tasks
        // STEP 10: ADMIN retrieves all tasks
        mockMvc.perform(get("/api/tasks?page=0&size=1000")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title",
                        org.hamcrest.Matchers.hasItems(
                                "User A Task 19",
                                "User B Task 19"
                        )))
                .andExpect(jsonPath("$.totalElements",
                        org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void userCannotRetrieveAnotherUsersTasksByUserId() throws Exception {

        // STEP 1: Register USER A
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User A 20",
                                "email": "usera20@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Register USER B and capture their ID
        MvcResult userBRegisterResult = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "name": "User B 20",
                                        "email": "userb20@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isCreated())
                .andReturn();

        Integer userBIdValue = JsonPath.read(
                userBRegisterResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long userBId = userBIdValue.longValue();

        // STEP 3: Login USER A
        MvcResult loginA = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "usera20@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String tokenA = JsonPath.read(
                loginA.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 4: Login USER B
        MvcResult loginB = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "userb20@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String tokenB = JsonPath.read(
                loginB.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 5: USER B creates a task
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "User B Private Task 20",
                                "taskDescription": "Private task belonging to USER B"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 6: USER A attempts to retrieve USER B's tasks
        mockMvc.perform(get("/api/tasks/user/" + userBId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanRetrieveAnotherUsersTasksByUserId() throws Exception {

        // STEP 1: Register USER B
        MvcResult userBRegisterResult = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "name": "User B 21",
                                        "email": "userb21@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isCreated())
                .andReturn();

        Integer userBIdValue = JsonPath.read(
                userBRegisterResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long userBId = userBIdValue.longValue();

        // STEP 2: Register ADMIN
        MvcResult adminRegisterResult = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "name": "Admin 21",
                                        "email": "admin21@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isCreated())
                .andReturn();

        Integer adminIdValue = JsonPath.read(
                adminRegisterResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long adminId = adminIdValue.longValue();

        // STEP 3: Promote registered user to ADMIN
        User admin = userRepo.findById(adminId).orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepo.save(admin);

        // STEP 4: Login USER B
        MvcResult userBLogin = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "userb21@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String userBToken = JsonPath.read(
                userBLogin.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 5: USER B creates a task
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "User B Task 21",
                                "taskDescription": "Task belonging to USER B"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 6: Login ADMIN
        MvcResult adminLogin = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "admin21@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String adminToken = JsonPath.read(
                adminLogin.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 7: ADMIN retrieves USER B's tasks
        mockMvc.perform(get("/api/tasks/user/" + userBId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title")
                        .value("User B Task 21"))
                .andExpect(jsonPath("$.content[0].assignedUserId")
                        .value(userBId));
    }

    @Test
    void getNonExistingTask_shouldReturn404() throws Exception {

        // STEP 1: Register USER
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User 22",
                                "email": "user22@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Login USER
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "user22@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 3: Request a task ID that does not exist
        mockMvc.perform(get("/api/tasks/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateNonExistingTask_shouldReturn404() throws Exception {

        // STEP 1: Register USER
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User 23",
                                "email": "user23@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Login USER
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "user23@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 3: Attempt to update a nonexistent task
        mockMvc.perform(patch("/api/tasks/999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "Updated Task",
                                "description": "Trying to update a missing task",
                                "status": "IN_PROGRESS",
                                "dueDate": null
                            }
                            """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteNonExistingTask_shouldReturn404() throws Exception {

        // STEP 1: Register ADMIN
        MvcResult registerResult = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "name": "Admin 24",
                                        "email": "admin24@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isCreated())
                .andReturn();

        // STEP 2: Extract ADMIN ID
        Integer adminIdValue = JsonPath.read(
                registerResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long adminId = adminIdValue.longValue();

        // STEP 3: Promote user to ADMIN
        User admin = userRepo.findById(adminId).orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepo.save(admin);

        // STEP 4: Login ADMIN
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "admin24@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String adminToken = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 5: Attempt to delete a nonexistent task
        mockMvc.perform(delete("/api/tasks/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTask_withMissingTaskName_shouldReturn400() throws Exception {

        // STEP 1: Register USER
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User 25",
                                "email": "user25@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Login USER
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "user25@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 3: Create task without taskName
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskDescription": "Task name is missing"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_withBlankTaskName_shouldReturn400() throws Exception {

        // STEP 1: Register USER
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User 26",
                                "email": "user26@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Login USER
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "user26@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 3: Create task with blank taskName
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "   ",
                                "taskDescription": "Task name contains only spaces"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeTaskStatus_withMissingStatus_shouldReturn400() throws Exception {

        // STEP 1: Register USER
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User 27",
                                "email": "user27@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Login USER
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "user27@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 3: Create a task
        MvcResult taskResult = mockMvc.perform(
                        post("/api/tasks")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "taskName": "User 27 Task",
                                        "taskDescription": "Testing missing status"
                                    }
                                    """))
                .andExpect(status().isCreated())
                .andReturn();

        Integer taskIdValue = JsonPath.read(
                taskResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long taskId = taskIdValue.longValue();

        // STEP 4: Attempt status update without status
        mockMvc.perform(patch("/api/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {}
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeTaskStatus_withNullStatus_shouldReturn400() throws Exception {

        // STEP 1: Register USER
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User 28",
                                "email": "user28@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Login USER
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "user28@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 3: Create a task
        MvcResult taskResult = mockMvc.perform(
                        post("/api/tasks")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "taskName": "User 28 Task",
                                        "taskDescription": "Testing null status"
                                    }
                                    """))
                .andExpect(status().isCreated())
                .andReturn();

        Integer taskIdValue = JsonPath.read(
                taskResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long taskId = taskIdValue.longValue();

        // STEP 4: Attempt status update with explicit null
        mockMvc.perform(patch("/api/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "status": null
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTask_withMissingTaskName_shouldReturn400() throws Exception {

        // STEP 1: Register USER
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User 29",
                                "email": "user29@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Login USER
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "user29@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 3: Create a task
        MvcResult taskResult = mockMvc.perform(
                        post("/api/tasks")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "taskName": "User 29 Task",
                                        "taskDescription": "Original task"
                                    }
                                    """))
                .andExpect(status().isCreated())
                .andReturn();

        Integer taskIdValue = JsonPath.read(
                taskResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long taskId = taskIdValue.longValue();

        // STEP 4: Update task without taskName
        mockMvc.perform(patch("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "description": "Updated description",
                                "status": "TODO"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTask_withBlankTaskName_shouldReturn400() throws Exception {

        // STEP 1: Register USER
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User 30",
                                "email": "user30@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Login USER
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "user30@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 3: Create a task
        MvcResult taskResult = mockMvc.perform(
                        post("/api/tasks")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "taskName": "User 30 Task",
                                        "taskDescription": "Original task"
                                    }
                                    """))
                .andExpect(status().isCreated())
                .andReturn();

        Integer taskIdValue = JsonPath.read(
                taskResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long taskId = taskIdValue.longValue();

        // STEP 4: Update task with blank taskName
        mockMvc.perform(patch("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "   ",
                                "description": "Updated description",
                                "status": "TODO"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTask_withMissingStatus_shouldReturn400() throws Exception {

        // STEP 1: Register USER
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User 31",
                                "email": "user31@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Login USER
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "user31@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 3: Create a task
        MvcResult taskResult = mockMvc.perform(
                        post("/api/tasks")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "taskName": "User 31 Task",
                                        "taskDescription": "Original task"
                                    }
                                    """))
                .andExpect(status().isCreated())
                .andReturn();

        Integer taskIdValue = JsonPath.read(
                taskResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long taskId = taskIdValue.longValue();

        // STEP 4: Update task without status
        mockMvc.perform(patch("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "Updated User 31 Task",
                                "description": "Updated description"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTask_withNullStatus_shouldReturn400() throws Exception {

        // STEP 1: Register USER
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User 32",
                                "email": "user32@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Login USER
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "user32@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 3: Create a task
        MvcResult taskResult = mockMvc.perform(
                        post("/api/tasks")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "taskName": "User 32 Task",
                                        "taskDescription": "Original task"
                                    }
                                    """))
                .andExpect(status().isCreated())
                .andReturn();

        Integer taskIdValue = JsonPath.read(
                taskResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long taskId = taskIdValue.longValue();

        // STEP 4: Update task with explicit null status
        mockMvc.perform(patch("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "Updated User 32 Task",
                                "description": "Updated description",
                                "status": null
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTask_withInvalidStatusValue_shouldReturn400() throws Exception {

        // STEP 1: Register USER
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User 33",
                                "email": "user33@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Login USER
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "user33@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 3: Create a task
        MvcResult taskResult = mockMvc.perform(
                        post("/api/tasks")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "taskName": "User 33 Task",
                                        "taskDescription": "Original task"
                                    }
                                    """))
                .andExpect(status().isCreated())
                .andReturn();

        Integer taskIdValue = JsonPath.read(
                taskResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long taskId = taskIdValue.longValue();

        // STEP 4: Update task with invalid status
        mockMvc.perform(patch("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "taskName": "Updated User 33 Task",
                                "description": "Updated description",
                                "status": "INVALID"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeTaskStatus_withInvalidStatusValue_shouldReturn400() throws Exception {

        // STEP 1: Register USER
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "User 34",
                                "email": "user34@test.com",
                                "password": "password123"
                            }
                            """))
                .andExpect(status().isCreated());

        // STEP 2: Login USER
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "user34@test.com",
                                        "password": "password123"
                                    }
                                    """))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        // STEP 3: Create a task
        MvcResult taskResult = mockMvc.perform(
                        post("/api/tasks")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "taskName": "User 34 Task",
                                        "taskDescription": "Testing invalid status"
                                    }
                                    """))
                .andExpect(status().isCreated())
                .andReturn();

        Integer taskIdValue = JsonPath.read(
                taskResult.getResponse().getContentAsString(),
                "$.id"
        );

        Long taskId = taskIdValue.longValue();

        // STEP 4: Send an invalid status value
        mockMvc.perform(patch("/api/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "status": "INVALID"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

}
