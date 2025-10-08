# Quick Start Guide

**Get up and running with lib-common-cqrs in 5 minutes**

## 🚀 Prerequisites

- Java 17+
- Spring Boot 3.1+
- Maven or Gradle

## 📦 Installation

### Maven

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-cqrs</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.firefly:lib-common-cqrs:1.0.0-SNAPSHOT'
```

## ⚙️ Basic Configuration

### Enable Auto-Configuration

Add to your `application.yml`:

```yaml
firefly:
  cqrs:
    enabled: true
    command:
      timeout: 30s
      retries: 3
      metrics-enabled: true
    query:
      caching-enabled: true
      cache-ttl: 5m
      metrics-enabled: true
```

### Spring Boot Application

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
        // ✅ CQRS framework is automatically configured!
        // ✅ CommandBus and QueryBus beans are available
        // ✅ Handlers are automatically discovered
    }
}
```

## 🎯 Step 1: Create Your First Command

```java
package com.example.commands;

import com.firefly.common.cqrs.command.Command;
import com.firefly.common.cqrs.validation.ValidationResult;
import reactor.core.publisher.Mono;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateAccountCommand implements Command<AccountResult> {
    
    @NotNull(message = "Customer ID is required")
    private final String customerId;
    
    @NotNull(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private final String email;
    
    @NotNull(message = "Initial deposit is required")
    private final BigDecimal initialDeposit;
    
    private final String accountType; // CHECKING, SAVINGS
    
    // Custom business validation
    @Override
    public Mono<ValidationResult> customValidate() {
        if (initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            return Mono.just(ValidationResult.failure("initialDeposit", 
                "Initial deposit cannot be negative"));
        }
        
        if (initialDeposit.compareTo(new BigDecimal("1000000")) > 0) {
            return Mono.just(ValidationResult.failure("initialDeposit", 
                "Initial deposit exceeds maximum allowed"));
        }
        
        return Mono.just(ValidationResult.success());
    }
}
```

## 🛠️ Step 2: Create Your Command Handler

```java
package com.example.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.example.commands.CreateAccountCommand;
import com.example.results.AccountResult;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

@CommandHandlerComponent(
    timeout = 30000,        // 30 second timeout
    retries = 3,            // 3 retry attempts  
    metrics = true,         // Enable metrics
    tracing = true,         // Enable distributed tracing
    description = "Creates a new customer account"
)
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountResult> {
    
    @Autowired
    private AccountService accountService;
    
    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command) {
        // Only business logic needed - everything else is automatic!
        return accountService.createAccount(
            command.getCustomerId(),
            command.getEmail(),
            command.getAccountType(),
            command.getInitialDeposit()
        ).map(account -> new AccountResult(
            account.getAccountId(),
            account.getCustomerId(),
            "CREATED",
            account.getBalance()
        ));
    }
    
    // ✅ NO BOILERPLATE NEEDED:
    // - No getCommandType() - automatically detected from generics
    // - No validation setup - handled by Jakarta Bean Validation + customValidate()
    // - No metrics setup - handled by @CommandHandlerComponent
    // - No handler registration - automatic component scanning
}
```

## 📊 Step 3: Create Your First Query

```java
package com.example.queries;

import com.firefly.common.cqrs.query.Query;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GetAccountBalanceQuery implements Query<AccountBalance> {
    
    @NotBlank(message = "Account ID is required")
    private final String accountId;
    
    // Override caching behavior if needed
    @Override
    public String getCacheKey() {
        // Note: The framework will automatically prefix this with ":cqrs:"
        // Final key will be "firefly:cache:default::cqrs:account_balance:..." after
        // lib-common-cache adds its "firefly:cache:{cacheName}:" prefix
        return "account_balance:" + accountId;
    }
    
    @Override
    public boolean isCacheable() {
        return true; // Enable caching for this query
    }
}
```

## 🔍 Step 4: Create Your Query Handler

```java
package com.example.handlers;

import com.firefly.common.cqrs.annotations.QueryHandlerComponent;
import com.firefly.common.cqrs.query.QueryHandler;
import com.example.queries.GetAccountBalanceQuery;
import com.example.results.AccountBalance;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

@QueryHandlerComponent(
    cacheable = true,                    // Enable result caching
    cacheTtl = 300,                      // Cache for 5 minutes
    metrics = true,                      // Enable metrics
    tracing = true,                      // Enable tracing
    description = "Retrieves account balance information"
)
public class GetAccountBalanceHandler extends QueryHandler<GetAccountBalanceQuery, AccountBalance> {
    
    @Autowired
    private AccountService accountService;
    
    @Override
    protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query) {
        // Only business logic - caching and metrics handled automatically!
        return accountService.getAccountBalance(query.getAccountId())
            .map(balance -> new AccountBalance(
                query.getAccountId(),
                balance,
                java.time.Instant.now()
            ));
    }
    
    // ✅ NO CACHING BOILERPLATE NEEDED:
    // - Cache key generation is automatic (or use query.getCacheKey())
    // - TTL is configured via annotation
    // - Cache eviction is handled automatically
}
```

## 🌐 Step 5: Use in Your Controller

```java
package com.example.controllers;

import com.firefly.common.cqrs.command.CommandBus;
import com.firefly.common.cqrs.query.QueryBus;
import com.firefly.common.cqrs.context.ExecutionContext;
import com.example.commands.CreateAccountCommand;
import com.example.queries.GetAccountBalanceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    
    @Autowired
    private CommandBus commandBus;
    
    @Autowired
    private QueryBus queryBus;
    
    @PostMapping
    public Mono<AccountResult> createAccount(
            @RequestBody CreateAccountRequest request,
            @RequestHeader("Authorization") String authToken,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantId,
            ServerHttpRequest httpRequest) {
        
        // Build execution context
        ExecutionContext context = ExecutionContext.builder()
            .withUserId(extractUserIdFromToken(authToken))
            .withTenantId(tenantId != null ? tenantId : "default")
            .withSource("web-app")
            .withClientIp(getClientIp(httpRequest))
            .withFeatureFlag("enhanced-validation", true)
            .build();
        
        // Create and send command
        CreateAccountCommand command = new CreateAccountCommand(
            request.getCustomerId(),
            request.getEmail(),
            request.getInitialDeposit(),
            request.getAccountType()
        );
        
        return commandBus.send(command, context);
    }
    
    @GetMapping("/{accountId}/balance")
    public Mono<AccountBalance> getAccountBalance(
            @PathVariable String accountId,
            @RequestHeader("Authorization") String authToken) {
        
        ExecutionContext context = ExecutionContext.builder()
            .withUserId(extractUserIdFromToken(authToken))
            .withSource("web-app")
            .build();
        
        GetAccountBalanceQuery query = new GetAccountBalanceQuery(accountId);
        
        return queryBus.query(query, context);
    }
    
    private String extractUserIdFromToken(String token) {
        // Extract user ID from JWT token
        return "user-123"; // Simplified for example
    }
    
    private String getClientIp(ServerHttpRequest request) {
        // Extract client IP from request
        return request.getRemoteAddress() != null ? 
            request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }
}
```

## 📋 Step 6: Define Your Result Classes

```java
package com.example.results;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class AccountResult {
    private final String accountId;
    private final String customerId;
    private final String status;
    private final BigDecimal balance;
}

@Data
public class AccountBalance {
    private final String accountId;
    private final BigDecimal balance;
    private final Instant lastUpdated;
}
```

## 🎯 Step 7: Create Your Service

```java
package com.example.services;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AccountService {
    
    public Mono<Account> createAccount(String customerId, String email, 
                                     String accountType, BigDecimal initialDeposit) {
        // Simulate account creation
        Account account = new Account(
            UUID.randomUUID().toString(),
            customerId,
            email,
            accountType,
            initialDeposit
        );
        
        // In real implementation, save to database
        return Mono.just(account);
    }
    
    public Mono<BigDecimal> getAccountBalance(String accountId) {
        // Simulate balance retrieval
        // In real implementation, query from database
        return Mono.just(new BigDecimal("1500.00"));
    }
}
```

## 🔧 Step 8: Advanced Configuration (Optional)

### Redis Caching

```yaml
firefly:
  cqrs:
    query:
      cache-type: REDIS
      cache-ttl: 15m

spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
```

### Authorization Configuration

```yaml
firefly:
  cqrs:
    authorization:
      enabled: true
      custom:
        enabled: true
        timeout-ms: 5000
```

### Metrics Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,cqrs
  endpoint:
    cqrs:
      enabled: true
```

## ✅ You're Ready!

That's it! You now have a fully functional CQRS application with:

- ✅ **Zero-boilerplate handlers** - Just business logic
- ✅ **Automatic validation** - Jakarta Bean Validation + custom rules
- ✅ **Built-in caching** - Queries cached automatically
- ✅ **Comprehensive metrics** - Timing, success/failure rates
- ✅ **Distributed tracing** - Correlation ID propagation
- ✅ **Execution context** - User, tenant, feature flag support

## 🔥 What's Next?

- **[Authorization System](AUTHORIZATION.md)** - Add security and access control
- **[Advanced Features](ADVANCED_FEATURES.md)** - Explore caching, metrics, and more
- **[Testing Strategies](TESTING.md)** - Learn how to test your handlers
- **[Developer Guide](DEVELOPER_GUIDE.md)** - Comprehensive development patterns

---

*Ready to build enterprise-grade applications! 🚀*