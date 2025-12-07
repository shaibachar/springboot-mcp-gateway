package com.example.usermanagement.controller;

import com.example.usermanagement.model.User;
import com.example.usermanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userService.deleteUser(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Test endpoint with optional and required parameters.
     * Tests nullable field in parameter schemas.
     */
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(
            @RequestParam String name,  // required parameter
            @RequestParam(required = false) Integer minAge,  // optional parameter
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {  // optional with default
        
        return ResponseEntity.ok(userService.getAllUsers().stream()
                .filter(user -> user.getName().toLowerCase().contains(name.toLowerCase()))
                .toList());
    }

    /**
     * Test endpoint with various parameter types.
     * Tests javaType metadata in parameter schemas.
     */
    @PostMapping("/batch")
    public ResponseEntity<String> batchOperation(
            @RequestParam Long userId,
            @RequestParam Integer count,
            @RequestParam Double amount,
            @RequestParam Boolean verify) {
        
        String result = String.format("Batch operation: userId=%d, count=%d, amount=%.2f, verify=%b",
                userId, count, amount, verify);
        return ResponseEntity.ok(result);
    }
}
