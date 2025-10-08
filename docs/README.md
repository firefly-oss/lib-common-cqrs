# lib-common-cqrs Documentation

**Enterprise-grade CQRS framework with reactive programming, zero-boilerplate handlers, and comprehensive observability.**

## 📖 Documentation Index

### Getting Started
- **[Quick Start Guide](QUICKSTART.md)** - Get up and running in 5 minutes
- **[Installation & Configuration](CONFIGURATION.md)** - Detailed setup and configuration options

### Architecture & Design
- **[Architecture Overview](ARCHITECTURE.md)** - Deep dive into framework design and patterns
- **[Core Components](CORE_COMPONENTS.md)** - Detailed documentation of interfaces and classes
- **[Design Patterns](DESIGN_PATTERNS.md)** - CQRS patterns and best practices

### Developer Guides
- **[Developer Guide](DEVELOPER_GUIDE.md)** - Comprehensive development reference
- **[Handler Implementation](HANDLER_GUIDE.md)** - Command and Query handler patterns
- **[Authorization System](AUTHORIZATION.md)** - Security and access control
- **[Validation Framework](VALIDATION.md)** - Input validation and business rules

### Advanced Topics
- **[Advanced Features](ADVANCED_FEATURES.md)** - Caching, metrics, tracing, and more
- **[Performance Tuning](PERFORMANCE.md)** - Optimization strategies and best practices
- **[Testing Strategies](TESTING.md)** - Unit, integration, and end-to-end testing
- **[Integration Patterns](INTEGRATION.md)** - Working with other systems

### Reference
- **[Configuration Reference](CONFIGURATION_REFERENCE.md)** - Complete configuration options
- **[API Reference](API_REFERENCE.md)** - Complete class and method documentation
- **[Migration Guide](MIGRATION.md)** - Upgrading from previous versions

## 🏗️ Framework Overview

lib-common-cqrs is a production-ready CQRS framework that provides:

### ✨ Zero-Boilerplate Development
```java
@CommandHandlerComponent
public class CreateAccountHandler extends CommandHandler<CreateAccountCommand, AccountResult> {
    
    @Override
    protected Mono<AccountResult> doHandle(CreateAccountCommand command) {
        // Only business logic needed - everything else is automatic!
        return accountService.createAccount(command);
    }
    
    // ✅ NO BOILERPLATE:
    // - Type detection from generics
    // - Automatic validation 
    // - Built-in metrics & tracing
    // - Handler registration
}
```

### 🔐 Enterprise Security
- **Custom Authorization**: Flexible custom business authorization logic
- **Context-Aware**: Multi-tenant, user, and feature flag support
- **Reactive**: Non-blocking authorization with Mono return types

### ⚡ High Performance
- **Reactive Streams**: Built on Project Reactor
- **Smart Caching**: Automatic cache key generation and TTL management
- **Circuit Breakers**: Built-in resilience patterns

### 🔍 Full Observability
- **Metrics**: Built-in timing, success/failure, throughput tracking
- **Tracing**: Distributed tracing with correlation IDs
- **Health Checks**: Comprehensive system health monitoring

## 🚀 Quick Example

```java
// 1. Define your command
@Data
public class TransferMoneyCommand implements Command<TransferResult> {
    @NotNull private final String sourceAccountId;
    @NotNull private final String targetAccountId;
    @NotNull @Positive private final BigDecimal amount;
    
    @Override
    public Mono<ValidationResult> customValidate() {
        if (sourceAccountId.equals(targetAccountId)) {
            return Mono.just(ValidationResult.failure("targetAccountId", 
                "Cannot transfer to the same account"));
        }
        return Mono.just(ValidationResult.success());
    }
}

// 2. Create your handler
@CommandHandlerComponent(timeout = 30000, metrics = true)
public class TransferMoneyHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {
    
    @Autowired
    private AccountService accountService;
    
    @Override
    protected Mono<TransferResult> doHandle(TransferMoneyCommand command) {
        return accountService.transferMoney(
            command.getSourceAccountId(),
            command.getTargetAccountId(), 
            command.getAmount()
        );
    }
}

// 3. Use it
@RestController
public class TransferController {
    
    @Autowired
    private CommandBus commandBus;
    
    @PostMapping("/transfer")
    public Mono<TransferResult> transfer(@RequestBody TransferRequest request,
                                       @RequestHeader("Authorization") String token) {
        
        ExecutionContext context = ExecutionContext.builder()
            .withUserId(extractUserFromToken(token))
            .withSource("web-app")
            .build();
            
        TransferMoneyCommand command = new TransferMoneyCommand(
            request.getSourceAccountId(),
            request.getTargetAccountId(),
            request.getAmount()
        );
        
        return commandBus.send(command, context);
    }
}
```

## 🎯 Key Features by Category

### Commands & Queries
- **Type-Safe**: Generic type resolution eliminates casting
- **Validation**: Jakarta Bean Validation + custom business rules
- **Authorization**: Built-in security with context awareness
- **Metadata**: Rich context with correlation IDs and audit trails

### Handlers
- **Auto-Registration**: Annotate and forget - handlers are found automatically  
- **Context-Aware**: Access user, tenant, feature flags in handlers
- **Reactive**: Native Mono/Flux support for non-blocking operations
- **Configurable**: Timeouts, retries, and behavior per handler

### Execution Context
- **Multi-Tenant**: Built-in tenant and organization isolation
- **Feature Flags**: Dynamic feature enablement
- **User Context**: Authentication and authorization context
- **Custom Properties**: Extensible context for any use case

### Observability
- **Actuator Integration**: Health indicators and custom endpoints
- **Micrometer Metrics**: Success/failure rates, timing, throughput
- **Distributed Tracing**: Correlation ID propagation
- **Health Monitoring**: CQRS system health and diagnostics

### Caching
- **Query Caching**: Automatic result caching with configurable TTL
- **Cache Eviction**: Smart eviction based on command relationships
- **Multiple Backends**: Local, Redis, Caffeine support
- **Cache Keys**: Intelligent key generation with customization

## 📋 System Requirements

- **Java**: 21+
- **Spring Boot**: 3.1+
- **Project Reactor**: 3.5+ (included transitively)
- **Optional**: Redis for distributed caching

## 🤝 Getting Help

- **Documentation**: Start with [Quick Start Guide](QUICKSTART.md)
- **Examples**: See [Developer Guide](DEVELOPER_GUIDE.md) for comprehensive examples
- **Configuration**: Check [Configuration Reference](CONFIGURATION_REFERENCE.md)
- **Troubleshooting**: See [Architecture Overview](ARCHITECTURE.md) for internals

## 📚 Related Libraries

This library works seamlessly with other Firefly components:

- **[lib-common-domain](../../lib-common-domain)** - Domain events, service clients, resilience
- **[lib-transactional-engine](../../lib-transactional-engine)** - Saga orchestration

---

*Built with ❤️ by Firefly Software Solutions Inc.*
