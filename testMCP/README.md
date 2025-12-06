# User Management API

A simple Spring Boot REST API for managing users with in-memory storage.

This project serves as a test application for the Spring Boot MCP Gateway Library, demonstrating both REST and GraphQL endpoints exposed through the Model Context Protocol (MCP).

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

## GraphQL Endpoints

The application also exposes GraphQL queries and mutations:

### Queries
- `getUserById(id: ID!)`: Get a user by ID
- `getAllUsers`: Get all users
- `searchUsers(name: String!)`: Search users by name

### Mutations
- `createUser(name: String!, email: String!)`: Create a new user
- `updateUser(id: ID!, name: String, email: String)`: Update a user
- `deleteUser(id: ID!)`: Delete a user

## MCP Endpoints

The Spring Boot MCP Gateway Library automatically exposes all REST and GraphQL endpoints through MCP:

### List Tools
```
GET /mcp/tools
```

Returns all discovered REST and GraphQL endpoints as MCP tools with enriched metadata.

### Execute Tool
```
POST /mcp/tools/execute
Content-Type: application/json

{
  "name": "get_api_users_api_users_getAllUsers",
  "arguments": {}
}
```

### Refresh Tools Cache
```
POST /mcp/tools/refresh
```

Forces a refresh of the discovered endpoints (clears the 5-minute cache).

## Testing

The project includes comprehensive integration tests for both the core functionality and enhanced MCP features:

### Running Tests

```bash
mvn test
```

### Test Coverage

#### GraphQLMcpIntegrationTest
Tests the basic MCP functionality with GraphQL integration:
- GraphQL endpoint discovery
- GraphQL query execution
- GraphQL mutation execution
- GraphQL query with arguments
- GraphQL search functionality

#### McpEnhancedFeaturesTest
Tests the enhanced features added to the MCP library:

**Discovery Caching:**
- Cache usage within TTL (5 minutes)
- Cache invalidation via `/mcp/tools/refresh` endpoint

**Enriched Metadata:**
- GraphQL tools contain `graphqlType`, `javaType`, and `nullable` fields
- REST tools contain `javaType` field
- Proper nullability detection for GraphQL arguments

**Structured Error Payloads:**
- Validation errors return `errorCode=validation_error` with `requestId` and `details`
- Tool not found errors return `errorCode=tool_not_found` with `requestId`
- Successful executions include unique `requestId` for correlation
- RequestIds are unique across different requests

**Combined Features:**
- GraphQL tools with enriched metadata execute successfully
- All responses include correlation identifiers for debugging

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

# List MCP tools
curl http://localhost:8080/mcp/tools

# Execute an MCP tool
curl -X POST http://localhost:8080/mcp/tools/execute \
  -H "Content-Type: application/json" \
  -d '{"name":"get_api_users_api_users_getAllUsers","arguments":{}}'

# Refresh MCP tools cache
curl -X POST http://localhost:8080/mcp/tools/refresh
```

## Features Tested

This test application validates the following Spring Boot MCP Gateway Library features:

1. **Automatic Endpoint Discovery**: Both REST and GraphQL endpoints are automatically discovered and exposed as MCP tools
2. **Discovery Caching**: Discovered endpoints are cached for 5 minutes to improve performance
3. **Cache Refresh**: The cache can be manually refreshed via the `/mcp/tools/refresh` endpoint
4. **Enriched Metadata**: Tool schemas include additional metadata:
   - `graphqlType`: The GraphQL type name for GraphQL arguments
   - `javaType`: The fully qualified Java type name
   - `nullable`: Whether the argument is nullable
5. **Structured Error Handling**: All responses include:
   - `errorCode`: Machine-readable error codes (e.g., `validation_error`, `tool_not_found`)
   - `requestId`: Unique correlation ID for debugging
   - `details`: Additional structured error information
6. **GraphQL Integration**: Full support for GraphQL queries, mutations, and argument mapping
7. **REST Integration**: Full support for REST endpoints with path variables, request bodies, and query parameters

