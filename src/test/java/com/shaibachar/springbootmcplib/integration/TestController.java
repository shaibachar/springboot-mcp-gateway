package com.shaibachar.springbootmcplib.integration;

import org.springframework.web.bind.annotation.*;

/**
 * Test controller for integration testing.
 */
@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable String id) {
        return new User(id, "User " + id);
    }

    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        return user;
    }

    @GetMapping("/greet")
    public String greet(@RequestParam String name) {
        return "Hello, " + name + "!";
    }

    public static class User {
        private String id;
        private String name;

        public User() {
        }

        public User(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
