package com.example.usermanagement.controller;

import com.example.usermanagement.model.User;
import com.example.usermanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class UserGraphQLController {

    private final UserService userService;

    @Autowired
    public UserGraphQLController(UserService userService) {
        this.userService = userService;
    }

    @QueryMapping
    public User getUserById(@Argument Long id) {
        return userService.getUserById(id).orElse(null);
    }

    @QueryMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @QueryMapping
    public List<User> searchUsers(@Argument String name) {
        return userService.getAllUsers().stream()
                .filter(user -> user.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    @MutationMapping
    public User createUser(@Argument String name, @Argument String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return userService.createUser(user);
    }

    @MutationMapping
    public User updateUser(@Argument Long id, @Argument String name, @Argument String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return userService.updateUser(id, user).orElse(null);
    }

    @MutationMapping
    public Boolean deleteUser(@Argument Long id) {
        return userService.deleteUser(id);
    }

    /**
     * Test GraphQL query with optional parameters.
     * Tests nullable, graphqlType, and javaType metadata.
     */
    @QueryMapping
    public List<User> advancedSearch(
            @Argument String keyword,  // Required parameter
            @Argument(name = "maxResults") Integer maxResults,  // Optional parameter
            @Argument(name = "includeInactive") Boolean includeInactive) {  // Optional parameter
        
        return userService.getAllUsers().stream()
                .filter(user -> keyword == null || 
                        user.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                        user.getEmail().toLowerCase().contains(keyword.toLowerCase()))
                .limit(maxResults != null ? maxResults : 100)
                .collect(Collectors.toList());
    }

    /**
     * Test GraphQL mutation with mixed parameter types.
     * Tests various javaType values in GraphQL metadata.
     */
    @MutationMapping
    public String complexOperation(
            @Argument Long targetId,
            @Argument Integer priority,
            @Argument Double score,
            @Argument Boolean confirmed) {
        
        return String.format("Operation: targetId=%d, priority=%d, score=%.2f, confirmed=%b",
                targetId, priority, score, confirmed);
    }
}
