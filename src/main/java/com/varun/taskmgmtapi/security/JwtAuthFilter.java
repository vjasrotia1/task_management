package com.varun.taskmgmtapi.security;

import com.varun.taskmgmtapi.models.User;
import com.varun.taskmgmtapi.repository.UserRepo;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
//extends OncePerRequestFilter means-- run this filter once for each HTTP request
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
private JwtService jwtService;
private UserRepo userRepo;


public  JwtAuthFilter(JwtService jwtService, UserRepo userRepo) {
    this.jwtService = jwtService;
    this.userRepo = userRepo;
}

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //Get Auth Header : below string reads Authorization: Bearer <token> from the header
        //get header from th  HTTP request
    String authorizationHeader = request.getHeader("Authorization");

    //check whether it is a bearer token?
        //only Authorization: Bearer eyJ... should be processed as JWT

        if(authorizationHeader==null || !authorizationHeader.startsWith("Bearer ")){
            //then it is not a bearer token
            filterChain.doFilter(request,response);
            return;
        }
        //extract JWT from header
        String token = authorizationHeader.substring(7);

        //next is to validate and decode the JWT

        try{
            Claims claims= jwtService.extractAllClaims(token);
            //jwtService.extractAllClaims(token) ---> returns
            //         Jwts.parser()
            //        .verifyWith(secretKey)
            //        .build()
            //        .parseSignedClaims(token)
            //        .getPayload();

            //extract userId
            String userId=claims.getSubject();

            if(SecurityContextHolder.getContext().getAuthentication()==null){
                //Long.parseLong(userId) converts String into number
                User user=userRepo.findById(Long.parseLong(userId))
                        .orElse(null);
                //why do we query the db? Because the user may have been deleted after the token was issued.
                //and We don't want a deleted user to remain authenticated indefinitely.
                if(user!=null){
                    //spring security uses authorities
                    //so we convert User into Role_USER
                    //Later, this allows us to do things like:@PreAuthorize("hasRole('ADMIN')") for admin only operations
                    var authorities= List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole().name()));

                    //create authentication object
                    //We're basically telling Spring Security
                    //"This request has been authenticated. This is the user.

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user,null,authorities);
                    //lastly put authentication object in Security-context
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    //after this moment, Spring Security effectively knows:
                    //current HTTP request--->authenticated user--->User#1--->ROLE_USER
                }
            }

        }catch(Exception e){
            //invalid token
            //dont authenticte the request
        }
        //It means:
        //"I'm done processing this filter. Continue the request." you can proceed to Next Spring Security filter
        //if not, then go to the controller
        filterChain.doFilter(request,response);
    }
}
