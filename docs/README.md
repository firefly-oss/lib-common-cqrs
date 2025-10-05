# lib-common-cqrs - Detailed Documentation

**Comprehensive CQRS implementation with reactive programming, zero-boilerplate handlers, and enterprise-grade features.**

## 📑 Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Core Components](#core-components)
3. [Handler Implementation Patterns](#handler-implementation-patterns)
4. [Validation Framework](#validation-framework)
5. [Authorization System](#authorization-system)
6. [Caching Strategy](#caching-strategy)
7. [Metrics & Observability](#metrics--observability)
8. [Configuration Reference](#configuration-reference)
9. [Advanced Usage](#advanced-usage)
10. [Performance Tuning](#performance-tuning)
11. [Testing Strategies](#testing-strategies)
12. [Complete Examples](#complete-examples)

## 🏗️ Architecture Overview

### CQRS Pattern Implementation

The lib-common-cqrs library implements the Command Query Responsibility Segregation (CQRS) pattern with a focus on developer productivity and operational excellence.

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    CQRS Framework Architecture                                              │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                                             │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐   │
│  │   Command Side      │    │   Query Side        │    │   Execution         │    │   Cross-Cutting     │   │
│  │                     │    │                     │    │   Context           │    │   Concerns          │   │
│  │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │   │
│  │ │ CommandBus      │ │    │ │ QueryBus        │ │    │ │ ExecutionContext│ │    │ │ Authorization   │ │   │
│  │ │ - Send          │ │    │ │ - Query         │ │    │ │ - User Context  │ │    │ │ Service         │ │   │
│  │ │ - Validation    │ │    │ │ - Caching       │ │    │ │ - Tenant Info   │ │    │ │ - lib-common-   │ │   │
│  │ │ - Authorization │ │    │ │ - Authorization │ │    │ │ - Feature Flags │ │    │ │   auth          │ │   │
│  │ │ - Metrics       │ │    │ │ - Metrics       │ │    │ │ - Session Data  │ │    │ │ - Custom Logic  │ │   │
│  │ └─────────────────┘ │    │ └─────────────────┘ │    │ └─────────────────┘ │    │ └─────────────────┘ │   │
│  │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │ ┌─────────────────┐ │   │
│  │ │ CommandHandler  │ │    │ │ QueryHandler    │ │    │ │ Context         │ │    │ │ Validation      │ │   │
│  │ │ Registry        │ │    │ │ Registry        │ │    │ │ Propagation     │ │    │ │ Framework       │ │   │
│  │ └─────────────────┘ │    │ └─────────────────┘ │    │ └─────────────────┘ │    │ │ - Jakarta       │ │   │
│  │ ┌─────────────────┐ │    │ ┌─────────────────┐ │    │                     │    │ │ - Custom Rules  │ │   │
│  │ │ @CommandHandler │ │    │ │ @QueryHandler   │ │    │                     │    │ └─────────────────┘ │   │
│  │ │ Components      │ │    │ │ Components      │ │    │                     │    │ ┌─────────────────┐ │   │
│  │ └─────────────────┘ │    │ └─────────────────┘ │    │                     │    │ │ Metrics &       │ │   │
│  └─────────────────────┘    └─────────────────────┘    └─────────────────────┘    │ │ Tracing         │ │   │
│                                                                                   │ │ - Micrometer    │ │   │
│                                                                                   │ │ - Spring        │ │   │
│                                                                                   │ │   Actuator      │ │   │
│                                                                                   │ └─────────────────┘ │   │
│                                                                                   └─────────────────────┘   │
│                                                                                                             │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Key Architectural Principles

1. **Separation of Concerns**: Commands handle state changes, queries handle data retrieval
2. **Zero Boilerplate**: Annotations and base classes eliminate repetitive code
3. **Reactive First**: Built on Project Reactor for non-blocking operations
4. **Type Safety**: Generic type resolution ensures compile-time safety
5. **Extensibility**: Hook points for custom validation, authorization, and processing

## 🔧 Core Components

### Command Interface

The Command interface is a marker interface that all commands must implement:

```java
public interface Command<R> {
    
    // Metadata methods with intelligent defaults
    default String getCommandId() { return UUID.randomUUID().toString(); }
    default Instant getTimestamp() { return Instant.now(); }
    default String getCorrelationId() { return null; }
    default String getInitiatedBy() { return null; }
    default Map<String, Object> getMetadata() { return null; }
    
    // Type resolution
    default Class<R> getResultType() { return (Class<R>) Object.class; }
    
    // Validation hooks
    default Mono<ValidationResult> validate() { return customValidate(); }
    default Mono<ValidationResult> customValidate() { return Mono.just(ValidationResult.success()); }
    
    // Authorization hooks
    default Mono<AuthorizationResult> authorize() { return Mono.just(AuthorizationResult.success()); }
    default Mono<AuthorizationResult> authorize(ExecutionContext context) { return authorize(); }
}
```

### Query Interface

The Query interface provides caching capabilities and authorization:

```java
public interface Query<R> {
    
    // Metadata methods
    default String getQueryId() { return UUID.randomUUID().toString(); }
    default Instant getTimestamp() { return Instant.now(); }
    default String getCorrelationId() { return null; }
    default String getInitiatedBy() { return null; }
    default Map<String, Object> getMetadata() { return null; }
    
    // Type resolution
    default Class<R> getResultType() { return (Class<R>) Object.class; }
    
    // Caching support
    default boolean isCacheable() { return true; }
    default String getCacheKey() { /* intelligent key generation */ }
    
    // Authorization hooks
    default Mono<AuthorizationResult> authorize() { return Mono.just(AuthorizationResult.success()); }
    default Mono<AuthorizationResult> authorize(ExecutionContext context) { return authorize(); }
}
```

## 📚 Examples

For comprehensive examples and usage patterns, see the main [README.md](../README.md) which includes:

- Banking application example with complete command and query handlers
- Multi-tenant application patterns
- Advanced authorization scenarios
- Performance optimization techniques
- Testing strategies
- Integration patterns with other Firefly libraries

This detailed documentation provides the architectural foundation and core concepts. The main README contains extensive working examples demonstrating all features in real-world scenarios.