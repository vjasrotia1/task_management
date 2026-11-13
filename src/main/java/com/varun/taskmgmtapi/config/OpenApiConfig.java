package com.varun.taskmgmtapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI(){
    final String securitySchemeName="bearerAuth";
    return new OpenAPI()
            .info(new Info()
                    .title("Task Management API")
                    .description("REST API documentation for Task Management Application")
                    .version("1.0.0"))
            .addSecurityItem(new SecurityRequirement()
                    .addList(securitySchemeName))
            .components(new io.swagger.v3.oas.models.Components()
                    .addSecuritySchemes(securitySchemeName,
                            new SecurityScheme()
                                    .name(securitySchemeName)
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")));
    }

}
/*
lets understand what we just configured
@Bean - registers the OpenAPI object in Spring's application context
Info()-- definesAPI title,description,version
SecurityScheme--tells swagger that we use Bearer JWT Authenticaiton
SecurityRequirement--applies the security requirement to documented API operations
The important part is the JWT configuration. It will enable an Authorize button in Swagger UI.

Next we'll configure Spring Security to allow Swagger UI & API documentation access without authentication
 while keeping our actual API endpoints protected.
 This is important because Swagger must be accessible before you can enter your JWT token.

 */
