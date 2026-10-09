package com.varun.taskmgmtapi.config;

//beans are created in securityconfig package
//BcryptPasswordEncoder is a part of Spring Security
//Your JWT filter is supposed to put the authentication object into SecurityContextHolder after validating the JWT
import com.varun.taskmgmtapi.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
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


