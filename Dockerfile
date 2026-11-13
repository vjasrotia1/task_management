
#Dockerfile contains build instructions
# we'll validate everything before building or starting the containers.
#before validating docker configurations, Make sure the terminal is currently inside project root directory, task-mgmt-api
#This is important because Docker needs to find your Dockerfile, docker-compose.yml and .env files.
#Image building will start using this command "docker compose build app" in Intellij Terminal
#it reads docker-compose.yml, finds appn service and its dockerfile,Downloads the required Maven dependencies and Java base images (if not already available).
#Copies your Spring Boot source code into the build environment, Builds your application JAR using Maven, 1. Packages the JAR into a Docker image.
#once this is done, we will start your Spring Boot application container and MySQL database container together using Docker Compose.
# "docker compose up -d"-- this command starts both the containers, Uses the health check to wait until MySQL is ready before starting Spring Boot, - Runs both containers in the background (-d).
#since Your Mac already has a local MySQL server running, which is occupying port 3306. so use port 3307 for Docker
#This allows you to keep your existing Mac MySQL server running while Docker uses its own MySQL database.
#check container status using cmd "docker compose ps"
#check logs "docker compose logs app" and look for springBoot startuo message
#then test ur api- without running Intellij application --http://localhost:8080
#A 403 Forbidden response means your request reached a server, but Spring Security denied access(as it requires authentication)
#Don't click Run in IntelliJ while testing the Docker version. Otherwise, you may have two instances of your application running.
#You can keep IntelliJ open for editing code; Docker runs the application independently.

#what have we achived till now?
#MySQL running inside Docker
# Docker database is healthy and accessible on host port 3307.

#Spring Boot container running
#our application container is up and exposing port 8080.

#Docker Compose networking configured
#Your application can use the MySQL service name mysql to connect internally.
#Once Docker containers are running, and the application logs show no issues.
#we now verify that our APIs work when the application runs inside Docker.
#Docker MySQL is a separate database from the MySQL instance running directly on your Mac. Our old local database records won't automatically appear in Docker.

#Docker Phase 1 progress-core docker setup completed
#DockerFile created, .dockerignore configured,DockerCompose configured,Springboot image built successfully
#mysql container running and healthy, springboot container running successfully, application connected to Docker mysql, API auth behaviour verified

#move to Phase 2: OpenAPI + Swagger UI with JWT integration. This will let us explore and test our APIs directly in the browser without Postman.



# Stage 1: Build the Spring Boot application
FROM maven:3.9.9-eclipse-temurin-17 AS build
#From- Selects a base image
WORKDIR /app
#workdir- Sets the working directory
# Copy Maven configuration
COPY pom.xml .
#copy- Copies files into the image
# Download dependencies
RUN mvn -B dependency:go-offline
#run- Executes a command while building the image
# Copy application source code
COPY src ./src

# Build executable JAR
RUN mvn -B clean package -DskipTests


# Stage 2: Run the application
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the generated JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Application port
EXPOSE 8080
#expose- Documents the application's listening port
# Start Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
#entrypoint- Defines the command that starts the container


#An important concept: Multi-stage Docker build
#we have 2 FROM instructions
# $stage 1: uses maven and java 17 to compile the appn and generate the JAR
# $stage 2: uses only the java runtime to run that JAR
#we dont need maven inside the final running container,this helps keep runtime image smaller
#We are using -DskipTests for this initial packaging step because we have already been working on the unit tests. We'll run tests separately in our CI/CD phase.
