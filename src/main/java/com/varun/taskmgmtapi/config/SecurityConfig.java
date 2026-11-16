package com.varun.taskmgmtapi.config;

//beans are created in Security config package
//BcryptPasswordEncoder is a part of Spring Security
//Your JWT filter is supposed to put the authentication object into SecurityContextHolder after validating the JWT
/*
A @Configuration class is essential in several key scenarios:
• Third-Party Libraries: You cannot open up external library classes (like ObjectMapper from Jackson, RestTemplate, or a database DataSource) and add @Component to them because you don't own their source code. A @Configuration class lets you instantiate and configure those external objects inside a @Bean method so Spring can manage them.
• Complex Initialization Logic: Stereotype annotations like @Service simply tell Spring to call the class's constructor. If an object requires conditional logic, environment-specific settings, or multiple setup steps before it is ready to use, you need a @Bean method inside a @Configuration class to write that custom Java setup code.
• Multiple Beans of the Same Type: You can only annotate a single class once with @Component. Inside a @Configuration class, you can define multiple @Bean methods that return different instances of the exact same class (for example, connecting to two different databases using two separate DataSource beans).
• Decoupling Configuration from Business Logic: Putting setup logic in @Configuration keeps your @Service and @Component classes clean and focused purely on business rules, without mixing in framework-specific wiring code.

Quick Comparison


Scenario	Best Choice	Why?
Your own business classes	@Service, @Repository, @Component ---> why? ---> Quick, automatic classpath scanning with minimal boilerplate.
External library classes	@Configuration + @Bean	You cannot modify external bytecode to add annotations.
Complex object setup	@Configuration + @Bean	Allows custom Java code, if/else logic, and manual parameter passing.

 */
import com.varun.taskmgmtapi.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
//@config--> marks a Java class as a source of bean definitions(contains methods marked with @Bean where method name acts as bean ID and returned obj is managed by spring) for the Spring IoC container
//The Spring IoC (Inversion of Control) Container is the core engine of the Spring Framework responsible for creating, configuring, and managing the entire lifecycle of your application's objects (called beans).
//Normally in Java, your code controls object creation using the new keyword. With Inversion of Control, you hand that responsibility over to Spring. The container reads your configuration, creates the objects, injects their required dependencies (via Dependency Injection), and destroys them when the app shuts down.
// In practice, this container is represented by interfaces like BeanFactory or the more commonly used ApplicationContext.
@EnableMethodSecurity
//@EnableMethodSecurity -- helps Spring to process @PreAuthorise
//@EnableMethodSecurity is a configuration annotation. It enables (method-level security) feature throughout your application.
//above annotation marks the class as source of bean definitions
//@EnableWebSecurity: Activates Spring Security’s web security support and integrates it with your application
public class SecurityConfig {
    private JwtAuthFilter jwtAuthFilter;
    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
    /*
    SecurityFilterChain: यह वह असली "चेन" (रास्ता) है जिससे होकर हर HTTP रिक्वेस्ट (User Request) को गुज़रना पड़ता है।
    इसी के अंदर आप तय करते हैं कि कौन सा URL सुरक्षित रहेगा और कौन सा खुला।
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    //this is basically spring security
    //after JWT generation: Spring Security doesn't automatically know what to do with our JWT yet.
    //so we need JwtAuthenticationFilter
    //suppose after login-- FE sends request as GET/api/tasks Auth : bearer eyJhGC.....

    //Now after creating JWTAuthFilter class, connect the JWT filter to Spring Security
    //We need to tell Spring Security:
    //"Run my JWTAuthfilter before Spring's username/password authentication filter."
        //username/password authenticationfilter- its job is to process form based login authentication  by validating username and pwd

    //this security filter chain will process the incoming HTTP request
    //firstly, filterchainproxy will determine which set of spring security rules apply to the incoming HTTP request
    // and then pass(dispatch) the HTTP request to the appropriate security filter chain(which has custom list of filters)
        //the HTTP request passes thru the sequence of these custom filters on security filters chain


        //Here, We'll secure task APIs after we build the JWT filter.
        http.csrf(csrf -> csrf.disable())
                /*
                Existing configuration disables CSRF and uses stateless sessions, which is a common setup for a REST API that authenticates exclusively through bearer tokens. If your frontend sends credentials through cookies instead, the CSRF decision needs to be revisited.
                 */
                //traditional session authentication

                //Login
                // ↓
                //Server creates session
                // ↓
                //Server remembers session
                // ↓
                //Browser sends session cookie


                //JWT
                //Login
                // ↓
                //Server creates JWT
                // ↓
                //Client stores JWT
                // ↓
                //Client sends JWT on every request

                //since server doesn't maintain login session for the API
                //therefore: STATELESS means "Don't use server-side HTTP sessions to remember authentication."
                //"stateless" means the server completely forgets who you are the moment a "request" is finished.

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                //FilterChainProxy evaluates the HTTP request's URL path using a RequestMatcher
                /*
                As soon as it finds a matching chain,
                it dispatches the request to the specific security filters
                (like CSRF protection, authentication, and authorization filters) tied to that chain
                 */
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**").permitAll()

                        .requestMatchers("/api/auth/**").permitAll()

                        .anyRequest().authenticated()
                )
                //generalised format: addFilterBefore(filter, class): Runs your filter before a standard filter.
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
                /*
what we're doing above?
URL	Access
/swagger-ui/**	--->Public
/swagger-ui.html --->Public
/v3/api-docs/** --->Public
/api/auth/**	----> Public (as before)
/api/tasks/**	---> Requires authentication and your existing authorization rules
only the documetation endpoints are public

after this-- rebuild and restart the application(since we are running thru docker so type   "docker compose up -d --build app" in terminal
after this open a link -- http://localhost:8080/swagger-ui/index.html and u should be able to see swagger UI page-- which means Swagger UI is working

till now we have configired: openAPI dependency, OpenAPIconfig.java, jwt bearer auth scheme,spring security access for swagger doc, docker rebuild and restart

Now, we will Test JWT authentication through Swagger. now we will verify that Swagger can authenticate against your existing JWT security setup.

first login for already existing user, copy JWT and then click on Authorise button at the top
then test a protected endpoint e.g. get/api/tasks
Then try a task belonging to that USER:get/api/tasks/{taskId}

Step 1: Log in as the user you created
In Swagger UI:
1. Open your login endpoint (POST /api/auth/login).
2. Click Try it out.
3. Enter the email and password of the user you created in Docker MySQL.
4. Click Execute.
5. Copy the JWT token from the response.
6. Click Authorize at the top and paste the token.

Step 2: Create a task
Find your task creation endpoint: POST/api/tasks
click "try it out"
and then try to test protected endpoint

what this test just verified
1. Swagger sends your JWT token.
2. JwtAuthFilter identifies the logged-in user.
3.TaskServiceImpl checks ownership before returning the task.

later u can Verify ownership in the database (docker mysql db)  by writing the below sql query inside docker mysql db
SELECT id, title, assigned_user_id
FROM tasks; we can use Intellij terminal to write this query or we cn use IntelliJ Database Tool(connect IntelliJ's Database window to Docker MySQL(mysql db running inside docker) using
            host - localhost
            Port-3307
            Databasetask_mgmt_api_db
            user - task_user
            Password- our .env password(Password for mysql db running inside docker)

after this, let's test whether your ownership-based authorization works correctly through Swagger.
Step 2: Test ownership restriction
Now try accessing a task assigned to a different user while using the same USER token.
Step 3: Test ADMIN access
Log in using your ADMIN credentials, authorize Swagger with the ADMIN JWT token, and try accessing that same task.

once these tests pass, it confirms your Swagger JWT integration and ownership-based authorization are working correctly.

now we start phase 3 -Redis caching

                 */




/*
public class SecurityConfig{
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
http.csrf(csrf -> csrf.disable())
.authorizeHttpRequests(auth -> auth.requestMatchers("/api/auth/**")
.permitAll()
.anyRequest()
.authenticated());

return http.build();
}
 */
/*
Do not confuse FilterChainProxy with Spring Web's DispatcherServlet.
While FilterChainProxy dispatches requests to security filters,
the DispatcherServlet runs later down the line to dispatch requests to your actual Controller endpoints.
 */

/*
complete FLOW
                 REGISTER
                    │
                    ▼
             POST /api/auth/register
                    │
                    ▼
               Create User
                    │
                    ▼
               BCrypt hash
                    │
                    ▼
                 MySQL


                  LOGIN
                    │
                    ▼
              POST /api/auth/login
                    │
                    ▼
              Check password
                    │
                    ▼
                Generate JWT
                    │
                    ▼
              Return JWT
                    │
                    ▼
                  Client
                    │
                    │ Authorization: Bearer JWT
                    ▼
              GET /api/tasks
                    │
                    ▼
          JwtAuthenticationFilter
                    │
                    ▼
             Validate JWT
                    │
                    ▼
             Find User
                    │
                    ▼
          SecurityContextHolder
                    │
                    ▼
          .authenticated() ?
                    │
               YES  │
                    ▼
             TaskController
 */

/*
1. The CSRF Rule java.csrf(csrf -> csrf.disable())
Use code with caution.What it means: "Hey guard, regarding the CSRF protection feature, I want you to turn it off.
"Layman Analogy: Tell the front desk receptionist not to ask visitors for a specific, matching company stamp on their paperwork today.
(Note: This is usually done for APIs where another system is making the requests). 


2. The Authorization Rule java.authorizeHttpRequests(auth -> auth
    .requestMatchers("/public/**").permitAll()
    .anyRequest().authenticated()
)
Use code with caution.
What it means: "Hey guard, regarding who gets into which room (Authorization),
here are the rules:
If someone goes to any door starting with /public/, let anyone in without checking an ID.
For absolutely any other door, they must prove who they are (be logged in)."
 Layman Analogy: The lobby and cafeteria are open to the public;
everyone walks right in. But to enter any actual office or the elevator, you must scan your badge.

 3. The Login Rule java.formLogin(Customizer.withDefaults());
Use code with caution.What it means: "Hey guard, regarding how people log in (Form Login), just use the standard, out-of-the-box settings that you came with." Layman Analogy: Don't build a custom, fancy badge-scanning station. Just use the standard turnstile and standard ID card scanner that the manufacturer provided. 🧩 Why the (x -> x...) syntax is usedThat weird arrow symbol (->) is just a pointer. You can read it out loud as "Configure it like this:" csrf(csrf -> ...) \[\rightarrow \] "For CSRF, configure the csrf system like this..."auth -> ... \[\rightarrow \] "For authorization, configure the auth rules like this..." It acts like an envelope. Everything inside that specific envelope belongs to only that security feature, keeping the rules neat, organized, and separated.
 */

/*
Suppose the database contains:

id = 10
name = Varun
role = USER

The JWT request comes in:

DELETE /api/tasks/5
Authorization: Bearer <JWT>

Your JwtAuthFilter does roughly:

JWT
 ↓
extract userId
 ↓
find User from DB
 ↓
user.getRole()
 ↓
USER
 ↓
ROLE_USER
 ↓
SecurityContext

Then Spring reaches:

@PreAuthorize("hasRole('ADMIN')")

Spring effectively asks:

Does SecurityContext contain ROLE_ADMIN?

It contains:

ROLE_USER

Therefore:

❌ Access denied

and the client receives:

403 Forbidden

This is a real and expected 403
 */

