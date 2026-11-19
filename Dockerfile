
#Dockerfile contains build instructions, Dockerfile has instructions for building your application image.Docker will package your Spring Boot application into a Docker image.
#pom.xml — Maven project configuration.
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

#Build your Spring Boot Docker image
 #Now we'll build a Docker image that Kubernetes can use to create your Spring Boot application Pod.
 #Important: This does not start a container or stop your IntelliJ application. It only creates the image
#docker build -t task-mgmt-api:1.0 .- docker reads docker file, maven downlaods dependecies, springboot appln is packaeged to JAR, docker creates the image

#Once SB appn is packaged into a Docker Image, A few important things happened:
 #1. mvn dependency:go-offline — Maven downloaded the required dependencies.
 #2. mvn clean package -DskipTests — Your application compiled and the JAR was created.
 #3. COPY --from=build ... — The JAR was copied into the lightweight Java runtime image.
 #4. naming to ... task-mgmt-api:1.0 — Docker created your image.
#verify this SB docker image and after verification, we'll use this image in your Kubernetes Deployment.
#Remember, Docker Desktop Kubernetes can use this locally built image without pushing it to Docker Hub.
#then after checkign that this image can be seen by K8S, then create your first Kubernetes Deployment for the Task Management API using task-mgmt-api:1.0 and imagePullPolicy: Never so Kubernetes uses the local image rather than trying to download it from Docker Hub.


#our app currently connects to MySQL and Redis using Docker host ports.Kubernetes Pods cannot simply use "localhost" to reach those services. We'll configure connectivity separately rather than changing your existing database setup blindly.
#we need to configure how the application inside Kubernetes will connect to MySQL and Redis. as inside a Kubernetes Pod, "localhost" refers to that Pod—not your Mac or your Docker MySQL/Redis containers.
#since out default k8s namespace is task-mgmt-- Kubernetes commands will automatically target this namespace unless you explicitly specify another one.


# we know which environment variables your Docker setup uses. lets first classify them

#Variable	Kubernetes configuration
 #MYSQL_DATABASE	ConfigMap
 #MYSQL_USER	ConfigMap
 #MYSQL_PASSWORD	Secret
 #MYSQL_ROOT_PASSWORD	Secret
 #JWT_SECRET	Secret
 #JWT_EXPIRATION	ConfigMap

 #A ConfigMap stores non-sensitive configuration, while a Secret stores sensitive values such as passwords and JWT signing keys.

 #Docker Compose vs Spring Boot
   #Your .env currently defines MySQL initialization variables. However, your Spring Boot application.properties expects:
   #- DB_URL
   #- DB_USERNAME
   #- DB_PASSWORD
   #- JWT_SECRET
   #- JWT_EXPIRATION
   #- REDIS_HOST
   #- REDIS_PORT
#so, We'll need to map the MySQL variables to the Spring Boot variables correctly.



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


#after docker+docker compose and OpenAPI+swagger+JWT authentication, Our first goal: get Redis running in Docker and confirm that your Spring Boot application can connect to it.
#why redis?Redis can store frequently accessed results in memory, reducing repeated database queries.else our appn will always query Mysql

