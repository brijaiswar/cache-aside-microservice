package com.wisepay.cache.microservices.service;

import com.wisepay.cache.microservices.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    // We will use "users" as the cache name. The key will be the 'userId' parameter.

    /**
     * Cacheable: On call, checks Redis for 'users::#userId'.
     * - Hit: Returns cached value immediately.
     * - Miss: Executes the method, then stores the result in Redis.
     */
    @Cacheable(value = "users", key = "#userId")
    public User getUserById(Long userId) {
        log.info(">>>> Fetching User {} from the Database (Simulated Slow Operation)...", userId);

        // --- Simulate expensive database call ---
        try {
            Thread.sleep(2000); // 2-second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // ----------------------------------------

        // Simulate returning the User from the DB
        return new User(userId, "user" + userId, "user" + userId + "@example.com");
    }

    /**
     * CachePut: Executes the method, then updates the cache with the method's result.
     * This is used for UPDATE operations to refresh the cache after the DB write.
     */
    @CachePut(value = "users", key = "#user.id")
    public User updateUser(User user) {
        log.warn(">>>> Updating User {} in the Database AND refreshing the cache...", user.getId());

        // Simulate DB update
        // (In a real app, this would call JpaRepository.save(user))

        return user; // Return the updated object to be placed in cache
    }

    /**
     * CacheEvict: Executes the method (optional), then removes the specified key from the cache.
     * This is used for DELETE operations to remove stale data.
     */
    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(Long userId) {
        log.error(">>>> Deleting User {} from the Database AND evicting the cache...", userId);

        // Simulate DB delete
        // (In a real app, this would call JpaRepository.deleteById(userId))
    }
}
