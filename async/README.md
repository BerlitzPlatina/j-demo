# async

Demo module for `@Async` in Spring Boot. Every endpoint answers one question, and every response
reports the thread names and the elapsed time, because that is what makes the difference between
the variants visible.

```bash
./mvnw spring-boot:run          # http://localhost:8080
```

Base path `/api/async`. Times below are what the calls actually take.

## 1. What async buys you

```bash
curl localhost:8080/api/async/sync        # ~10s
curl localhost:8080/api/async/parallel    # ~5s
```

The same three tasks (5s + 2s + 3s). `/sync` runs them on the request thread one after another;
`/parallel` starts all three on the pool first and only then waits, so the total is the longest
task instead of their sum. `sumOfTasksMs` is 10000 in both cases, `totalMs` is not.

The order matters: starting a task and immediately joining it before starting the next would be
sequential again, just with extra thread hops.

## 2. Fire and forget

```bash
curl -X POST 'localhost:8080/api/async/fire-and-forget?seconds=3'   # 202 in ~0ms
curl localhost:8080/api/async/events                                # the outcome, a few seconds later
```

An `@Async void` method returns immediately and the caller can neither wait for it nor find out
whether it worked. The task records itself in an in-memory event store so the demo has something to
show; in real code that would be a database row, a metric or a notification.

## 3. Composition and non-blocking MVC

```bash
curl localhost:8080/api/async/combine              # thenCombine + thenApply
curl 'localhost:8080/api/async/mvc-future?seconds=3'
```

`/combine` merges two futures without blocking in between; only the final read waits.

`/mvc-future` returns the `CompletableFuture` from the controller. Spring MVC releases the servlet
thread right away and writes the response when the future completes — the client still waits 3s,
but no request thread is parked for it. Compare `servletThread` with `completedOnThread` in the
response.

## 4. Timeouts and failures

```bash
curl -i 'localhost:8080/api/async/timeout?seconds=5&timeoutMs=1000'  # 504 after 1s
curl localhost:8080/api/async/exception-future
curl -i -X POST localhost:8080/api/async/exception-void              # 202, then check /events
```

- `/timeout` stops *waiting* after 1s, but the task keeps running on the pool and still occupies a
  thread — the log shows it finishing later.
- A method returning a future carries its exception to whoever reads the result: `get()` wraps it in
  an `ExecutionException`, the real one is the `cause`.
- A `void` method has nowhere to put an exception, so it goes to the `AsyncUncaughtExceptionHandler`
  in [AsyncConfig.java](src/main/java/com/example/async/config/AsyncConfig.java) and shows up in
  `/events` with type `failed`. The caller still gets a 202.

## 5. The self-invocation trap

```bash
curl localhost:8080/api/async/self-invocation
```

`@Async` works through a proxy. Calling an annotated method from inside the same bean goes straight
to the method and the annotation does nothing. In the response, `selfInvoked.thread` is the
`http-nio-*` request thread while `throughProxy.thread` is `async-*`. Same for a missing
`@EnableAsync`: everything silently runs synchronously.

## 6. Choosing an executor

```bash
curl 'localhost:8080/api/async/single-thread?count=3&seconds=1'   # ~3s, all on single-1
curl 'localhost:8080/api/async/virtual?count=5&seconds=2'         # ~2s, five virtual threads
```

Async means "not on the caller's thread", not "in parallel": the single thread executor serializes
everything sent to it. Virtual threads (Java 21) create one thread per task with no pool to size,
which fits blocking IO; they do not speed up CPU bound work.

## 7. A bounded pool under load

The default pool is deliberately small: core 2, max 4, queue 4, `CallerRunsPolicy`.

```bash
curl -X POST 'localhost:8080/api/async/flood?count=10&seconds=1'
curl localhost:8080/api/async/pool     # call during a flood to watch it fill up
```

A `ThreadPoolExecutor` only grows past the core size once the queue is full, so an unbounded queue
means `maxPoolSize` is never reached and work piles up invisibly. With the bounded queue here, 8
tasks fit (4 running + 4 queued) and the next submit is rejected — `CallerRunsPolicy` then runs it
on the request thread, which is why one submission reports `ranOnCallerThread: true` and blocks for
the full task duration. Slow, but nothing is dropped.

## 8. Context propagation

Each request gets a `traceId` in the MDC ([TraceIdFilter](src/main/java/com/example/async/filter/TraceIdFilter.java)).
A worker thread does not inherit it, so the `TaskDecorator` in `AsyncConfig` copies the caller's MDC
in and clears it afterwards — without the clear, a pooled thread would leak one request's trace id
into the next task it picks up. The `traceId` field in every task result shows whether the copy
worked; the same applies to security context and request-scoped beans, which are not available in
an async thread either.
