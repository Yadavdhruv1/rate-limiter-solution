# Pluggable Rate Limiting System for External Resource Usage

A pluggable rate limiter that controls external API calls (not incoming client requests). Supports multiple algorithms and key strategies.

## Problem Statement

Design a rate limiting module for external resource calls made by an internal service. Not every API request consumes quota — rate limiting is applied only when the system is about to call a paid external resource.

## UML Class Diagram

![UML Diagram](image.png)

## Design Overview

### Classes

| Class | Responsibility |
|---|---|
| `RateLimiter` | Facade — takes a Request, resolves the key, and delegates to the policy |
| `RateLimitPolicy` | Strategy interface — any algorithm implements this |
| `FixedWindowCounter` | Fixed time windows, resets counter each window |
| `SlidingWindowCounter` | Weighted combination of current + previous window counts |
| `TokenBucket` | Tokens refill over time, consumed on each request |
| `KeyStrategy` | Interface to resolve rate limit key from a request |
| `CustomerKeyStrategy` | Resolves key by customer ID |
| `TenantKeyStrategy` | Resolves key by tenant ID |
| `ApiKeyStrategy` | Resolves key by API key |
| `ProviderKeyStrategy` | Resolves key by external provider name |
| `RateLimiterConfig` | Holds maxRequests and windowSizeMs |
| `ExternalServiceProxy` | Internal service that checks rate limit before calling external API |
| `Request` | Holds customerId, tenantId, apiKey, provider |
| `Clock` | Interface to abstract time |
| `SystemClock` | Real clock using `System.currentTimeMillis()` |

### Design Patterns Used

- **Strategy Pattern** — `RateLimitPolicy` lets us swap algorithms (Fixed Window, Sliding Window, Token Bucket)
- **Strategy Pattern** — `KeyStrategy` lets us change what the rate limit key is (per customer, tenant, API key, provider)
- **Facade Pattern** — `RateLimiter` hides the key resolution + policy check behind one method
- **Dependency Injection** — `Clock` is injected so tests can use `FakeClock`

### How Algorithm Swapping Works

```java
// switch from FixedWindow to SlidingWindow — no change in business logic
RateLimitPolicy policy = new FixedWindowCounter(config, clock);
// or
RateLimitPolicy policy = new SlidingWindowCounter(config, clock);
// or
RateLimitPolicy policy = new TokenBucket(config, clock);

RateLimiter limiter = new RateLimiter(policy, new CustomerKeyStrategy());
ExternalServiceProxy proxy = new ExternalServiceProxy(limiter);
proxy.handleRequest(request, needsExternalCall);
```

## Sequence Diagram

```
Client -> API -> InternalService
                    |
                    v
            needsExternalCall?
                   / \
                 No   Yes
                 |      |
            return    RateLimiter.allowRequest(request)
                        |
                    KeyStrategy.resolveKey(request) -> key
                        |
                    RateLimitPolicy.allowRequest(key)
                       / \
                    true   false
                     |       |
               call API   REJECTED
```

## Algorithm Trade-offs

### Fixed Window Counter
- **Pros**: simple, O(1) time and space per key, easy to understand
- **Cons**: burst at window boundary — a user can make 2x limit requests if they send requests at the end of one window and start of the next

### Sliding Window Counter
- **Pros**: smooths out the boundary burst problem by weighting the previous window's count
- **Cons**: slightly more complex, still an approximation (not exact sliding log)

### Token Bucket
- **Pros**: allows controlled bursts up to bucket capacity, smooth refill over time
- **Cons**: needs floating point math, slightly harder to reason about exact limits

## How to Run

### Compile

```bash
javac -d out src/*.java
```

### Run Demo

```bash
java -cp out ratelimiter.Main
```

### Run Tests

```bash
java -cp out ratelimiter.RateLimiterTest
```

## Expected Output (Demo)

```
=== Pluggable Rate Limiter Demo ===
Config: RateLimiterConfig{maxRequests=5, windowSizeMs=60000}

--- Using Fixed Window Counter (by customer) ---
  Request 1: External API called successfully for customer: C1
  Request 2: External API called successfully for customer: C1
  Request 3: External API called successfully for customer: C1
  Request 4: External API called successfully for customer: C1
  Request 5: External API called successfully for customer: C1
  Request 6: REJECTED - rate limit exceeded
  Request 7: REJECTED - rate limit exceeded

  Internal request (no rate limit check): Handled internally, no external call needed

--- Using Sliding Window Counter (by tenant) ---
  Request 1: External API called successfully for customer: C1
  ...
  Request 6: REJECTED - rate limit exceeded

--- Using Token Bucket (by api key) ---
  Request 1: External API called successfully for customer: C1
  ...
  Request 6: REJECTED - rate limit exceeded

--- Different Key Strategies (Fixed Window, limit=2) ---
  C1 req1 (by customer): true
  C1 req2 (by customer): true
  C1 req3 (by customer): false
  C2 req1 (by customer): true

  stripe req1 (by provider): true
  stripe req2 (by provider): true
  stripe req3 (by provider): false

=== Done ===
```

## Project Structure

```
rate-limiter-solution/
├── image.png              # UML class diagram
├── README.md
└── src/
    ├── Clock.java
    ├── SystemClock.java
    ├── RateLimitPolicy.java
    ├── RateLimiterConfig.java
    ├── FixedWindowCounter.java
    ├── SlidingWindowCounter.java
    ├── TokenBucket.java
    ├── KeyStrategy.java
    ├── CustomerKeyStrategy.java
    ├── TenantKeyStrategy.java
    ├── ApiKeyStrategy.java
    ├── ProviderKeyStrategy.java
    ├── Request.java
    ├── ExternalServiceProxy.java
    ├── RateLimiter.java
    ├── Main.java
    └── RateLimiterTest.java
```

## Key Design Decisions

1. **Rate limiting at external call point, not API gateway** — `ExternalServiceProxy` checks the rate limiter only when `needsExternalCall` is true
2. **Pluggable algorithms** — `RateLimitPolicy` interface + Strategy pattern lets you swap Fixed Window / Sliding Window / Token Bucket without touching caller code
3. **Pluggable key resolution** — `KeyStrategy` lets the same limiter work per-customer, per-tenant, per-API-key, or per-provider
4. **Thread safety** — `ConcurrentHashMap` for key-to-data mapping, `synchronized` / `ReentrantLock` for per-key mutation
5. **Testability** — `Clock` interface lets tests inject `FakeClock` to control time without `Thread.sleep()`
6. **Extensibility** — adding a new algorithm (Leaky Bucket, Sliding Log) means implementing one interface with one method
