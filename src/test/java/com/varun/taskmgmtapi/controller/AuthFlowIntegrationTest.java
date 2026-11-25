package com.varun.taskmgmtapi.controller;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql-test")
@Transactional
public class AuthFlowIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerUser_shouldReturn201() throws Exception {

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "name": "Test User",
                                        "email": "integration@test.com",
                                        "password": "Test@12345"
                                    }
                                    """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("integration@test.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void loginUser_shouldReturn200AndJwtToken() throws Exception {

        // Step 1: Register a user
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Login Test User",
                                "email": "login@test.com",
                                "password": "Test@12345"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 2: Login using the same credentials
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "email": "login@test.com",
                                "password": "Test@12345"
                            }
                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginUser_withWrongPassword_shouldReturn401() throws Exception {

        // Step 1: Register a user
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Wrong Password User",
                                "email": "wrongpassword@test.com",
                                "password": "Test@12345"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 2: Attempt login with incorrect password
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "wrongpassword@test.com",
                                "password": "Wrong@12345"
                            }
                    """)
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void loginUser_withUnknownEmail_shouldReturn401() throws Exception {

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "unknown@test.com",
                                "password": "Test@12345"
                            }
                    """)
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void registerUser_withExistingEmail_shouldReturn409() throws Exception {

        // Step 1: Register user for the first time
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Duplicate User",
                                "email": "duplicate@test.com",
                                "password": "Test@12345"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 2: Try registering with the same email
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Duplicate User 2",
                                "email": "duplicate@test.com",
                                "password": "Test@67890"
                            }
                    """)
        ).andExpect(status().isConflict());
    }
    @Test
    void registerUser_withMissingEmail_shouldReturn400() throws Exception {

        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Validation User",
                                "password": "Test@12345"
                            }
                    """)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_withInvalidEmail_shouldReturn400() throws Exception {

        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Validation User",
                                "email": "invalid-email",
                                "password": "Test@12345"
                            }
                    """)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_withShortPassword_shouldReturn400() throws Exception {

        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Validation User",
                                "email": "shortpassword@test.com",
                                "password": "Test12"
                            }
                    """)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_withMissingPassword_shouldReturn400() throws Exception {

        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Validation User",
                                "email": "missingpassword@test.com"
                            }
                    """)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_withMissingName_shouldReturn400() throws Exception {

        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "missingname@test.com",
                                "password": "Test@12345"
                            }
                    """)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_withBlankName_shouldReturn400() throws Exception {

        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "   ",
                                "email": "blankname@test.com",
                                "password": "Test@12345"
                            }
                    """)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_withBlankEmail_shouldReturn400() throws Exception {

        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Validation User",
                                "email": "   ",
                                "password": "Test@12345"
                            }
                    """)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_withBlankPassword_shouldReturn400() throws Exception {

        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Validation User",
                                "email": "blankpassword@test.com",
                                "password": "   "
                            }
                    """)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void loginUser_withMissingEmail_shouldReturn400() throws Exception {

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "password": "Test@12345"
                            }
                    """)
        ).andExpect(status().isBadRequest());
    }
    @Test
    void loginUser_withInvalidEmail_shouldReturn400() throws Exception {

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "invalid-email",
                                "password": "Test@12345"
                            }
                    """)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void loginUser_withMissingPassword_shouldReturn400() throws Exception {

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "loginvalidation@test.com"
                            }
                    """)
        ).andExpect(status().isBadRequest());
    }
    @Test
    void loginUser_withBlankPassword_shouldReturn400() throws Exception {
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "loginvalidation@test.com",
                                "password": "   "
                            }
                    """)
        ).andExpect(status().isBadRequest());
    }
    @Test
    void loginUser_withShortPassword_shouldReturn400() throws Exception {
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "loginvalidation@test.com",
                                "password": "abc"
                            }
                    """)
        ).andExpect(status().isBadRequest());
    }
    @Test
    void authenticatedUser_shouldAccessMyTasksEndpoint() throws Exception {

        // Step 1: Register a new USER
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "JWT Test User",
                                "email": "jwtintegration@test.com",
                                "password": "password123"
                            }
                    """)
        ).andExpect(status().isCreated());

        // Step 2: Login and obtain JWT
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "email": "jwtintegration@test.com",
                                "password": "password123"
                            }
                    """)
                ).andExpect(status().isOk())
                .andReturn();

        // Step 3: Extract JWT from login response
        String responseBody = loginResult.getResponse().getContentAsString();

        String token = JsonPath.read(responseBody, "$.token");

        // Step 4: Access protected endpoint using JWT
        mockMvc.perform(
                get("/api/tasks/my")
                        .header("Authorization", "Bearer " + token)
        ).andExpect(status().isOk());
    }

}
//we're testing the API's actual database-backed behaviour, not mocking the repository or service.
//We'll build a single complete flow:
//Register → Login → Receive JWT → Access a protected endpoint using JWT.
//We'll use the real Spring services, JWT filter and task_mgmt_lock_test_db.
//Our test data
//We'll use the following values:
//Field	Test value
//Name	Test User
//Email	integration@test.com
//Password	Test@12345
//Test no 1 : Understand what we're testing
//- post() sends an actual HTTP POST request through Spring MVC.
//- contentType() tells Spring that we're sending JSON.
//- content() supplies the registration payload.
//- isCreated() verifies HTTP 201.
//- jsonPath() checks fields in the actual JSON response.
//Unlike your controller-slice test, this test uses the actual AuthServiceImpl and repository, so it should persist a user in the MySQL test database.
//The test passes through Maven in Terminal but fails when you click IntelliJ's green Run icon.
//
//The likely reason is that IntelliJ doesn't have the MYSQL_TEST_PASSWORD environment variable. Your Terminal export doesn't automatically update IntelliJ's environment.
//Step 1: Add the environment variable in IntelliJ
//1. Open IntelliJ.
//2. Go to Run → Edit Configurations.
//3. Select the run configuration for AuthFlowIntegrationTest (or create a JUnit configuration if needed).
//4. Find Environment variables.
//5. Click the environment-variable editor icon.
//6. Add:
//- Name: MYSQL_TEST_PASSWORD
//- Value: your MySQL root password.
//7. Click Apply → OK.
//
//Keep the password private; don't share a screenshot showing its value.
//Step 2
//Run registerUser_shouldReturn201() again using the green Run icon.

//test 2 : loginUser_shouldReturn200AndJwtToken
// Understand what we're testing
//Step	Expected result
//Register user	201 Created
//Login with correct credentials	200 OK
//JWT token returned	Token is not empty
//Important: We register the user inside this test rather than depending on the previous test. Each integration test should be independently executable.

//test no 3 : loginUser_withWrongPassword_shouldReturn401What are we verifying?
//Scenario	Expected HTTP status
//Register user	201
//Login with wrong password	401
//Why register first?
//We want to ensure the email exists in the database. That way, the test specifically checks incorrect-password handling rather than user-not-found handling.

//test no 4 : loginUser_withUnknownEmail_shouldReturn401
// Why this test matters:
//Your AuthServiceImpl throws InvalidCredentialsException when userRepo.findByEmail() cannot find the email.

//test registration validation
//Why are we testing this?
//
//Your RegisterRequest has:
//@NotBlank(message = "Email is required")
//@Email(message = "please provide a valid email")
//private String email;
//
//
//We want to confirm that @Valid in your AuthController actually enforces this validation.

//test - register user with invalid email
//What this verifies:
//- @Email validation is working.
//- Invalid email formats are rejected.
//- The request returns HTTP 400 Bad Request.
//- The service should not register the user.

//last test
//Test 19: Use JWT to access a protected endpoint
//This is an important end-to-end test. It verifies the complete authentication flow:
//1. Register a user.
//2. Log in with that user's credentials.
//3. Receive a JWT.
//4. Send the JWT in the Authorization header.
//5. Access a protected API endpoint successfully.
//Before writing the test, please share your current TaskController.java code.
//I want to use the correct protected endpoint and expected response from your actual implementation rather than assume the API structure.
// Your controller has a suitable endpoint for this test: GET /api/tasks/my.
//We'll use this instead of GET /api/tasks because /api/tasks is restricted to ADMIN, whereas /api/tasks/my is available to an authenticated USER.
//Our test will verify that the JWT filter correctly authenticates a normal USER and that Spring Security makes the logged-in user available to TaskController.
// Understand what we're verifying
//1. Register — A USER is saved in MySQL.
//2. Login — The application generates a JWT.
//3. Extract token — We retrieve the token from the login response.
//4. Send token — We pass it in the Authorization header.
//5. JWT filter — It validates the token and loads the user.
//6. Controller — /api/tasks/my retrieves the authenticated user's tasks.
//
//Expected result: HTTP 200 OK.
