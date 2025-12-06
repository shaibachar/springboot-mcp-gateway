package com.example.usermanagement.service;

import com.example.usermanagement.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {
    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public UserService() {
        // Add some initial users
        createUser(new User(null, "John Doe", "john@example.com", "123-456-7890"));
        createUser(new User(null, "Jane Smith", "jane@example.com", "098-765-4321"));
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public Optional<User> getUserById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    public User createUser(User user) {
        Long id = idGenerator.getAndIncrement();
        user.setId(id);
        users.put(id, user);
        return user;
    }

    public Optional<User> updateUser(Long id, User updatedUser) {
        if (!users.containsKey(id)) {
            return Optional.empty();
        }
        updatedUser.setId(id);
        users.put(id, updatedUser);
        return Optional.of(updatedUser);
    }

    public boolean deleteUser(Long id) {
        return users.remove(id) != null;
    }
}
