# Spring Boot MCP Library

A Spring Boot library that automatically exposes REST endpoints as MCP (Model Context Protocol) tools, enabling LLM engines to discover and execute your API endpoints dynamically.

## Features

- 🔍 **Automatic Endpoint Discovery**: Scans and discovers all REST controllers in your Spring Boot application
- 🛠️ **MCP Tool Generation**: Automatically converts REST endpoints to MCP tool definitions with JSON schemas
- 🚀 **Runtime Execution**: Execute any discovered endpoint through MCP interface
- 📝 **Full Documentation**: Comprehensive Javadoc for all public APIs
- 🐛 **Debug Logging**: Detailed DEBUG-level logging for troubleshooting
- ✅ **Tested**: Includes unit tests and integration tests

## What is MCP?

The Model Context Protocol (MCP) is a standardized way to expose tools and resources that Large Language Models (LLMs) can interact with. This library implements MCP for Spring Boot REST endpoints, allowing LLMs to:
- Discover available API endpoints as tools
- Understand endpoint parameters through JSON schemas
- Execute endpoints with proper parameter mapping

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.shaibachar</groupId>
    <artifactId>spring-boot-mcp-lib</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```gradle
implementation 'com.shaibachar:spring-boot-mcp-lib:1.0.0-SNAPSHOT'
```

## Usage

### Basic Setup

1. Add the dependency to your Spring Boot project
2. The library auto-configures itself - no additional configuration needed!
3. Your REST endpoints are automatically exposed as MCP tools

### Example Application

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}

@RestController
@RequestMapping("/api")
public class UserController {
    
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable String id) {
        return userService.findById(id);
    }
    
    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        return userService.create(user);
    }
}
```

### MCP Endpoints

Once the library is added, the following MCP endpoints are automatically available:

#### List Available Tools

```http
GET /mcp/tools
```

Returns all discovered REST endpoints as MCP tools with their schemas.

**Response Example:**
```json
{
  "tools": [
    {
      "name": "get_api_users_id_getUser",
      "description": "Calls GET /api/users/{id} (Controller: UserController, Method: getUser)",
      "inputSchema": {
        "type": "object",
        "properties": {
          "id": {
            "type": "string"
          }
        },
        "required": ["id"]
      }
    }
  ]
}
```

#### Execute a Tool

```http
POST /mcp/tools/execute
Content-Type: application/json

{
  "name": "get_api_users_id_getUser",
  "arguments": {
    "id": "123"
  }
}
```

**Response Example:**
```json
{
  "content": [
    {
      "type": "text",
      "text": "{\"id\":\"123\",\"name\":\"John Doe\"}"
    }
  ],
  "isError": false
}
```

#### Refresh Tools Cache

```http
POST /mcp/tools/refresh
```

Forces a refresh of the discovered endpoints (useful if controllers are dynamically registered).

## Configuration

### Logging

The library uses SLF4J for logging with DEBUG level by default. To configure logging, add to your `application.properties`:

```properties
# Set to DEBUG to see detailed endpoint discovery and execution logs
logging.level.com.shaibachar.springbootmcplib=DEBUG

# Set to INFO for production
logging.level.com.shaibachar.springbootmcplib=INFO
```

### Parameter Names

To ensure parameter names are preserved in compiled code (important for proper parameter mapping), add this to your `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <parameters>true</parameters>
    </configuration>
</plugin>
```

## How It Works

1. **Endpoint Discovery**: On application startup, the library scans Spring's `RequestMappingHandlerMapping` to discover all registered REST endpoints
2. **Tool Generation**: Each endpoint is converted to an MCP tool with:
   - A unique name based on HTTP method, path, and handler method name
   - A description of what the endpoint does
   - A JSON schema describing the input parameters
3. **Runtime Execution**: When a tool is executed:
   - The library finds the corresponding endpoint
   - Maps the provided arguments to method parameters
   - Invokes the actual controller method
   - Returns the result as JSON

## Architecture

```
┌─────────────────────────────────────────┐
│   LLM / MCP Client                      │
└────────────┬────────────────────────────┘
             │
             │ HTTP Requests
             │
┌────────────▼────────────────────────────┐
│   McpController                         │
│   - GET /mcp/tools                      │
│   - POST /mcp/tools/execute             │
└────────────┬────────────────────────────┘
             │
        ┌────┴─────┐
        │          │
┌───────▼──────┐ ┌▼─────────────────────┐
│ToolMapping   │ │ToolExecution        │
│Service       │ │Service              │
└───────┬──────┘ └┬────────────────────┘
        │         │
        │    ┌────▼──────┐
        │    │Endpoint   │
        └────►Discovery  │
             │Service    │
             └───────────┘
                  │
        ┌─────────▼──────────┐
        │ Spring MVC         │
        │ Controllers        │
        └────────────────────┘
```

## API Reference

### Models

- `McpTool`: Represents an MCP tool definition
- `McpToolsResponse`: Response containing list of available tools
- `McpToolExecutionRequest`: Request to execute a tool
- `McpToolExecutionResponse`: Response from tool execution
- `EndpointMetadata`: Internal representation of a REST endpoint

### Services

- `EndpointDiscoveryService`: Discovers REST endpoints from Spring MVC
- `McpToolMappingService`: Maps endpoints to MCP tools
- `McpToolExecutionService`: Executes tools by invoking endpoints

### Configuration

- `McpAutoConfiguration`: Auto-configuration for the library

## Development

### Building

```bash
mvn clean install
```

### Running Tests

```bash
mvn test
```

### Test Coverage

The library includes:
- Unit tests for all core components
- Integration tests with a test Spring Boot application
- Tests for endpoint discovery, tool mapping, and execution

## Requirements

- Java 17 or higher
- Spring Boot 3.2.0 or higher
- Maven 3.6+ (for building)

## License

This project is licensed under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions, please open an issue on GitHub.
