package com.varun.taskmgmtapi.config;

import com.varun.taskmgmtapi.dto.TaskDto.TaskResponse;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/*
caching annotations
1. @EnableCaching---> Enables Spring's caching functionality in your application.
2. @Cacheable---> suppose taskId 5 is requested for the first time,Spring executes the method.Method fetches task 5 from MySQL and this task with ID=5 gets stored in Redis
@Cacheable(value = "tasks", key = "#taskId")
public TaskResponse getTaskById(Long taskId) {
    // Fetch task from database
}

3. @CachePut---> The method always executes. Once the MYSQL database update succeeds, its returned result replaces the cached value.
@CachePut(value = "tasks", key = "#taskId")
public TaskResponse updateTask(Long taskId, ...) {
    // Update task in database
}
4. @CacheEvict-->After deleting a task from MySQL DB, we remove its cached entry too.Otherwise, Redis might continue returning a task that no longer exists.

@CacheEvict(value = "tasks", key = "#taskId")
public void deleteTask(Long taskId) {
    // Delete task from database
}

One important design decision for your project:
your task APIs enforce USER ownership and ADMIN permissions.
We'll preserve those checks when implementing caching, rather than blindly caching getTaskById() and
accidentally bypassing authorization on a cache hit.

also our project uses:
1. Spring Data JPA for MySQL.
2. Spring Data Redis for caching.

Spring is detecting both modules and checking whether your repositories belong to JPA or Redis.
our TaskRepo and UserRepo extends JPA Repository << >>
in our project, We are using Redis as a cache, not as a replacement for MySQL or JPA repositories. so we dont need @RedisHash to add to our entities

We need to configure Redis to serialize Java objects into JSON usig JSON serialiser
 */
@Configuration
@EnableCaching //it enables spring's caching mechanism

public class CacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
    JacksonJsonRedisSerializer<TaskResponse> serializer =
            new JacksonJsonRedisSerializer<>(TaskResponse.class);

        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
            RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
            RedisSerializationContext.SerializationPair
                    .fromSerializer(serializer)
                );
    }
}

