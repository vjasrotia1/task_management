package com.varun.taskmgmtapi.controller;


import com.varun.taskmgmtapi.repository.UserRepo;
import com.varun.taskmgmtapi.security.JwtService;
import com.varun.taskmgmtapi.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.varun.taskmgmtapi.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import com.varun.taskmgmtapi.dto.UserDto.UpdateUserRoleRequest;
import com.varun.taskmgmtapi.dto.authDto.UserResponse;
import com.varun.taskmgmtapi.models.Role;
import com.varun.taskmgmtapi.models.User;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import org.junit.jupiter.api.AfterEach;



@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
//It tells Spring to load your application's actual security configuration in this MVC test, including:
//- CSRF disabled.
//- Stateless session management.
//- JWT authentication filter.
//- Authentication required for protected endpoints
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepo userRepo;
//Step 4 — Write our first test: Unauthenticated request → 401
    @Test
    void updateUserRole_whenUnauthenticated_shouldReturn401() throws Exception {

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/users/role/1")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "role": "ADMIN"
                            }
                            """)
        ).andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isUnauthorized()
        );
    }
    @Test
    @WithMockUser(roles = "USER")
    //It creates an authenticated mock user with the authority:ROLE_USER, Spring checks this permission before executing the controller method. Since the user has ROLE_USER, access is denied with 403.
    void updateUserRole_whenUserIsNotAdmin_shouldReturn403() throws Exception {

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/users/role/1")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "role": "ADMIN"
                            }
                            """)
        ).andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isForbidden()
        );
    }

    @Test
    void updateUserRole_whenAdmin_shouldUpdateRoleSuccessfully() throws Exception {
        // Arrange: Create an ADMIN user
        User admin = new User();
        admin.setId(10L);
        admin.setName("Admin");
        admin.setEmail("admin@test.com");
        admin.setRole(Role.ADMIN);

        // Set ADMIN as the authenticated principal
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        admin,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Prepare request DTO
        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(Role.ADMIN);

        // Prepare expected response
        UserResponse response = new UserResponse(
                1L,
                "Test User",
                "test@test.com",
                Role.ADMIN
        );

        // Mock service response
//        when(userService.updateUserRole(1L, request, 10L))
//                .thenReturn(response);
        when(userService.updateUserRole(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(UpdateUserRoleRequest.class),
                org.mockito.ArgumentMatchers.eq(10L)
        )).thenReturn(response);

        // Act & Assert
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/users/role/1")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "role": "ADMIN"
                            }
                            """)
                )
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .status().isOk()
                )
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.id").value(1)
                )
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .jsonPath("$.role").value("ADMIN")
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUserRole_whenRoleIsNull_shouldReturn400() throws Exception {

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/users/role/1")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "role": null
                            }
                            """)
        ).andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isBadRequest()
        );
        //Why use @WithMockUser here?
        //Validation happens after authentication and authorization. This mock ADMIN passes the @PreAuthorize check. Then @Valid checks the request DTO and rejects the null role.
        //We expect HTTP 400, and the service should never be called.

        verify(userService, never()).updateUserRole(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(UpdateUserRoleRequest.class),
                org.mockito.ArgumentMatchers.anyLong()
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

}
