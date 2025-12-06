# User Management API

A simple Spring Boot REST API for managing users with in-memory storage.

## Prerequisites

- Java 17 or higher
- Maven 3.6+

## Running the Application

```bash
mvn spring-boot:run
```

The server will start on `http://localhost:8080`

## API Endpoints

### Get All Users
```
GET /api/users
```

### Get User by ID
```
GET /api/users/{id}
```

### Create User
```
POST /api/users
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "123-456-7890"
}
```

### Update User
```
PUT /api/users/{id}
Content-Type: application/json

{
  "name": "John Updated",
  "email": "john.updated@example.com",
  "phone": "111-222-3333"
}
```

### Delete User
```
DELETE /api/users/{id}
```

## Example Usage with curl

```bash
# Get all users
curl http://localhost:8080/api/users

# Get user by ID
curl http://localhost:8080/api/users/1

# Create a new user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","phone":"555-1234"}'

# Update a user
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"John Updated","email":"john@example.com","phone":"123-456-7890"}'

# Delete a user
curl -X DELETE http://localhost:8080/api/users/1
```
