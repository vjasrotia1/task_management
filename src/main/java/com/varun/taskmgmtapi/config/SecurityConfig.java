package com.varun.taskmgmtapi.config;

//beans are created in config package
//BcryptPasswordEncoder is a part of Spring Security
import com.varun.taskmgmtapi.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {


    private JwtAuthFilter jwtAuthFilter;
    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }


    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    //this is basically spring security
    //after JWT generation: Spring Security doesn't automatically know what to do with our JWT yet.
    //so we need JwtAuthenticationFilter
    //suppose after login-- FE sends request as GET/api/tasks Auth : bearer eyJhGC.....

    //Now after creating JWTAuthFilter class, connect the JWT filter to Spring Security
    //We need to tell Spring Security:
    //"Run my JWT filter before Spring's username/password authentication filter."
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

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

                //since server doesnto maintain login session for the API
                //therefore: STATELESS means "Don't use server-side HTTP sessions to remember authentication."
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
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

