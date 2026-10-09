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

/*
Why do we need a filter?
Because we don't want to write this in every controller:
String token = request.getHeader("Authorization"); or we dont want authentication code duplicated everywhere
instead
HTTP request--->JWTfilter(Does token validation using JwtService..jwts.parser())--->creates authentication object if token valid--->Controller
The filter handles authentication centrally.
 */
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
        //example : only Authorization: Bearer eyJ... should be processed as JWT

        if(authorizationHeader==null || !authorizationHeader.startsWith("Bearer ")){
            //then it is not a bearer token
            //I am done processing this filter, go to next Spring Security filter or if not, go to Controller
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
            //        .parseSignedClaims(token) --- this can throw an error if secretkeys donot match
            //        .getPayload();
            //jwts.parser() e.g. it is like "digital letter" checking tool/machine having "company" stamp with it"
            //.verifyWith(secretKey)-- means we provide a copy of company stamp(secretkey) to thus tool/machine
            //or in other words, we can say parser() verifies incoming "digital letter" (i.e. token) with the help of this Stamp(secretKey)
            //.build()-- means "now turn on this machine"
            //.parseSignedClaims(token)-- means compare the seal on the letter(token's secret key) with the seal provided in step 2
            //.getPayload()-- if seals match, get the payload


            //extract userId from the claims/payload
            String userId=claims.getSubject();

            //if authentication object is not present, then we hv to create it and put in securitycontextholder
            //so as to remember the User info while processing the current request
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

                    UsernamePasswordAuthenticationToken authenticationObject =
                            new UsernamePasswordAuthenticationToken(user,null,authorities);
                    /*
                    Here:

                user — the authenticated user's entity.
                null — credentials aren't needed after authentication.
               authorities — the user's granted roles, such as ROLE_USER or ROLE_ADMIN
                    Authentication object has
                    ├── Principal → varun/User object
                    ├── Credentials → null
                    └── Authorities → ROLE_USER
                     */
                    //lastly put this authentication object in Security-context
                    SecurityContextHolder.getContext().setAuthentication(authenticationObject);

                    //after this moment, Spring Security effectively knows:
                    //for current HTTP request---> who is the authenticated user making this request and what is his/her authority/role--->User#1--->ROLE_USER
                    //or "Spring Security, for this request, remember that this user is authenticated.
                }
            }

        }catch(Exception e){
            e.printStackTrace();
            //invalid token
            //dont authenticate the request
        }
        //It means:
        //"I'm done processing this filter. Continue the request." you can proceed to Next Spring Security filter
        //if not, then go to the controller
        //Without this line, the request may never reach your controller.
        filterChain.doFilter(request,response);
    }
}

/*
SecurityContextHolder

SecurityContextHolder is used inside your Spring application to keep track of the authenticated user during request processing.

So:

JWT
 ↓
comes from outside
 ↓
validate it
 ↓
create Authentication
 ↓
SecurityContextHolder
 ↓
Spring Security uses it


In one sentence

If you have to explain this in an interview:

JWT validation verifies that the token's signature is valid and that the token has not expired. After successful validation, Spring Security creates an Authentication object and stores it in the SecurityContextHolder, which allows the application to know who the current authenticated user is and what authorities they have.

And the simplest mental model is:

JWT = ID card
JWT validation = checking the ID card
Authentication = verified identity
SecurityContextHolder = temporary place where Spring remembers that identity
Authorities = permissions written on the badge


Request
  ↓
JWT Filter
  ↓
Authentication
  ↓
filterChain.doFilter()
  ↓
Next Spring Security filter
  ↓
Controller


The main idea is: instead of asking the client which user is logged in, we can get the authenticated user's information directly from Spring Security.

1. Why do we need SecurityContextHolder?

Suppose your database contains these users:

User ID Name Role
101 Rahul USER
102 Amit USER
103 varun ADMIN

say, Rahul logs in and receives a JWT token.

Now Rahul sends this request:

GET /api/tasks/user/102

If your API trusts the userId in the URL, Rahul might try to access Amit's tasks.
Instead, we can identify Rahul from his (authenticated session context) and retrieve only Rahul's tasks.
That's where SecurityContextHolder helps.

1. Client sends JWT
The request includes the Bearer token
2. JwtAuthFilter validates the token
It identifies the user by querying the DB and creates an Authentication object.
3. SecurityContextHolder
Spring Security makes the current Authentication Object available to your application code.
4. Your controller or service
Gets the authenticated user's identity and performs the appropriate operation.

 */

