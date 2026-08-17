package com.example.async.controller;

import com.example.async.config.AsyncConfig;
import com.example.async.model.TaskResult;
import com.example.async.store.AsyncEventStore;
import com.example.async.task.TaskFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>
 * One endpoint per idea, so each can be called on its own and compared with the others. Every
 * response reports the thread names involved and the elapsed time, which is what makes the
 * difference between the variants visible.
 * </p>
 *
 * @author NamHoang
 */
@RestController
@RequestMapping("/api/async")
@RequiredArgsConstructor
@Slf4j
public class AsyncController {

    private final TaskFactory taskFactory;

    private final AsyncEventStore store;

    @Qualifier("taskExecutor")
    private final ThreadPoolTaskExecutor taskExecutor;

    // --------------------------------------------------------------------
    // 1. The baseline: sequential vs parallel
    // --------------------------------------------------------------------

    /**
     * Three blocking tasks one after another on the request thread: 5 + 2 + 3, so about 10s. The
     * request thread is held for the whole time and can serve nobody else.
     */
    @GetMapping("/sync")
    public Map<String, Object> sync() throws InterruptedException {
        long start = System.currentTimeMillis();
        List<TaskResult> results = List.of(taskFactory.task1(), taskFactory.task2(), taskFactory.task3());
        return timed("sync", start, results);
    }

    /**
     * The same three tasks started asynchronously and waited on afterwards. The calls return
     * immediately, the work overlaps on the pool, so the total is the longest task (about 5s)
     * rather than their sum.
     *
     * <p>Note the ordering: all three must be started <em>before</em> the first join, otherwise
     * each would be awaited before the next one starts and it would be sequential again.</p>
     */
    @GetMapping("/parallel")
    public Map<String, Object> parallel() throws InterruptedException {
        long start = System.currentTimeMillis();

        CompletableFuture<TaskResult> f1 = taskFactory.asyncTask1();
        CompletableFuture<TaskResult> f2 = taskFactory.asyncTask2();
        CompletableFuture<TaskResult> f3 = taskFactory.asyncTask3();

        // Waits for all three; each join() then returns without blocking further.
        CompletableFuture.allOf(f1, f2, f3).join();

        return timed("parallel", start, List.of(f1.join(), f2.join(), f3.join()));
    }

    // --------------------------------------------------------------------
    // 2. Fire and forget
    // --------------------------------------------------------------------

    /**
     * Returns straight away with 202: the task is still running when the response is written. Its
     * outcome shows up in {@code GET /events} a few seconds later. Nothing can be returned to the
     * caller about it, and if it fails the caller never finds out.
     */
    @PostMapping("/fire-and-forget")
    public ResponseEntity<Map<String, Object>> fireAndForget(@RequestParam(defaultValue = "backgroundJob") String name,
                                                             @RequestParam(defaultValue = "3") int seconds)
            throws InterruptedException {
        long start = System.currentTimeMillis();
        taskFactory.fireAndForget(name, seconds);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accepted", true);
        body.put("task", name);
        body.put("requestThread", Thread.currentThread().getName());
        body.put("returnedAfterMs", System.currentTimeMillis() - start);
        body.put("hint", "the task is still running, check GET /api/async/events in " + seconds + "s");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    // --------------------------------------------------------------------
    // 3. Composition
    // --------------------------------------------------------------------

    /**
     * Composition instead of blocking: thenCombine merges two results when both are ready, and
     * thenApply maps that without anyone waiting in between. Only the final get blocks.
     */
    @GetMapping("/combine")
    public Map<String, Object> combine(@RequestParam(defaultValue = "2") int first,
                                       @RequestParam(defaultValue = "3") int second) throws InterruptedException {
        long start = System.currentTimeMillis();

        CompletableFuture<TaskResult> a = taskFactory.asyncSleep("combine-a", first);
        CompletableFuture<TaskResult> b = taskFactory.asyncSleep("combine-b", second);

        // The callback runs on whichever thread finishes last, not on the request thread.
        CompletableFuture<String> combined = a.thenCombine(b,
                        (ra, rb) -> ra.task() + "(" + ra.tookMs() + "ms) + " + rb.task() + "(" + rb.tookMs() + "ms)")
                .thenApply(String::toUpperCase);

        Map<String, Object> response = timed("combine", start, List.of(a.join(), b.join()));
        response.put("combined", combined.join());
        return response;
    }

    /**
     * The controller returns the future itself. Spring MVC releases the request thread immediately
     * and only writes the response once the future completes, so the container can serve other
     * requests meanwhile. The client still waits, but no thread is parked for it.
     */
    @GetMapping("/mvc-future")
    public CompletableFuture<Map<String, Object>> mvcFuture(@RequestParam(defaultValue = "3") int seconds)
            throws InterruptedException {
        long start = System.currentTimeMillis();
        String servletThread = Thread.currentThread().getName();

        return taskFactory.asyncSleep("mvcFuture", seconds).thenApply(result -> {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("servletThread", servletThread);
            response.put("completedOnThread", Thread.currentThread().getName());
            response.put("task", result);
            response.put("totalMs", System.currentTimeMillis() - start);
            response.put("hint", "the servlet thread was released as soon as this future was returned");
            return response;
        });
    }

    // --------------------------------------------------------------------
    // 4. Timeout and failures
    // --------------------------------------------------------------------

    /**
     * Gives up waiting after {@code timeoutMs}. Worth understanding: the timeout only ends the
     * <em>waiting</em>. The task keeps running on the pool and still occupies a thread, as the
     * log line it prints when it finishes shows.
     */
    @GetMapping("/timeout")
    public Map<String, Object> timeout(@RequestParam(defaultValue = "5") int seconds,
                                       @RequestParam(defaultValue = "1000") long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        CompletableFuture<TaskResult> future = taskFactory.asyncSleep("timeoutTask", seconds);

        try {
            return timed("timeout", start, List.of(future.get(timeoutMs, TimeUnit.MILLISECONDS)));
        } catch (TimeoutException e) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                    "gave up after " + timeoutMs + "ms, the task itself keeps running in the background");
        } catch (ExecutionException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, String.valueOf(e.getCause()), e);
        }
    }

    /**
     * An async method that returns a future carries its exception to whoever reads the result, so
     * the failure is still the caller's to handle. get() wraps it in an ExecutionException.
     */
    @GetMapping("/exception-future")
    public Map<String, Object> exceptionFuture() throws InterruptedException {
        long start = System.currentTimeMillis();
        CompletableFuture<TaskResult> future = taskFactory.failFuture();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestThread", Thread.currentThread().getName());
        try {
            response.put("result", future.get());
        } catch (ExecutionException e) {
            // The real exception is the cause; the ExecutionException itself is just the wrapper.
            response.put("caught", e.getClass().getSimpleName());
            response.put("cause", e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
        }
        response.put("totalMs", System.currentTimeMillis() - start);
        return response;
    }

    /**
     * A void async method that throws: the caller sees a normal 202 because there is nothing to
     * carry the exception back. It ends up in the AsyncUncaughtExceptionHandler, which records it
     * under {@code GET /events} with type "failed".
     */
    @PostMapping("/exception-void")
    public ResponseEntity<Map<String, Object>> exceptionVoid() {
        taskFactory.failVoid();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "accepted", true,
                "requestThread", Thread.currentThread().getName(),
                "hint", "the call failed but the caller cannot know, see GET /api/async/events"));
    }

    // --------------------------------------------------------------------
    // 5. Pitfalls and executor choice
    // --------------------------------------------------------------------

    /**
     * Self invocation. Both calls hit the same annotated method; only the one that goes through the
     * proxy is asynchronous. Compare the thread names: the self invoked one ran on the request
     * thread, so the annotation did nothing.
     */
    @GetMapping("/self-invocation")
    public Map<String, Object> selfInvocation() throws InterruptedException {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestThread", Thread.currentThread().getName());
        response.put("selfInvoked", taskFactory.runViaSelfInvocation(1));
        response.put("throughProxy", taskFactory.asyncSleep("throughProxy", 1).join());
        response.put("hint", "selfInvoked ran on the request thread: @Async only applies through the proxy");
        return response;
    }

    /**
     * Async is not the same as parallel. These tasks go to a one thread executor, so they run one
     * after another and the total is their sum, even though every call returned immediately.
     */
    @GetMapping("/single-thread")
    public Map<String, Object> singleThread(@RequestParam(defaultValue = "3") int count,
                                            @RequestParam(defaultValue = "1") int seconds) throws InterruptedException {
        long start = System.currentTimeMillis();
        List<CompletableFuture<TaskResult>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            futures.add(taskFactory.onSingleThread("single-" + i, seconds));
        }
        return timed("singleThread", start, futures.stream().map(CompletableFuture::join).toList());
    }

    /**
     * Same work on virtual threads: a new one per task, no pool limit, so all of them start at
     * once. Suited to blocking IO, not to CPU bound work.
     */
    @GetMapping("/virtual")
    public Map<String, Object> virtual(@RequestParam(defaultValue = "5") int count,
                                       @RequestParam(defaultValue = "2") int seconds) throws InterruptedException {
        long start = System.currentTimeMillis();
        List<CompletableFuture<TaskResult>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            futures.add(taskFactory.onVirtualThread("virtual-" + i, seconds));
        }
        return timed("virtual", start, futures.stream().map(CompletableFuture::join).toList());
    }

    /**
     * Submits more tasks than the pool can take, to show what a bounded executor does under load:
     * {@code corePoolSize} run, {@code queueCapacity} wait, the pool grows to {@code maxPoolSize}
     * only once the queue is full, and the rest hit the CallerRunsPolicy and are executed by the
     * request thread. A submit that took roughly as long as the task itself is one of those.
     */
    @PostMapping("/flood")
    public Map<String, Object> flood(@RequestParam(defaultValue = "12") int count,
                                     @RequestParam(defaultValue = "2") int seconds) throws InterruptedException {
        long start = System.currentTimeMillis();
        String requestThread = Thread.currentThread().getName();

        List<Map<String, Object>> submissions = new ArrayList<>();
        int ranOnCaller = 0;
        for (int i = 0; i < count; i++) {
            long submitStart = System.currentTimeMillis();
            taskFactory.fireAndForget("flood-" + i, seconds);
            long submitMs = System.currentTimeMillis() - submitStart;

            // A submit that blocks for about the task duration means the pool rejected it and the
            // caller ran it inline.
            boolean inline = submitMs > 500;
            if (inline) {
                ranOnCaller++;
            }
            submissions.add(Map.of("task", "flood-" + i, "submitMs", submitMs, "ranOnCallerThread", inline));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count", count);
        response.put("requestThread", requestThread);
        response.put("ranOnCallerThread", ranOnCaller);
        response.put("capacityBeforeRejecting", AsyncConfig.MAX_POOL_SIZE + AsyncConfig.QUEUE_CAPACITY);
        response.put("submissions", submissions);
        response.put("totalMs", System.currentTimeMillis() - start);
        response.put("pool", pool());
        return response;
    }

    /**
     * Live view of the default pool. Call it while /flood or /parallel is running to see the active
     * count and the queue fill up.
     */
    @GetMapping("/pool")
    public Map<String, Object> pool() {
        ThreadPoolExecutor executor = taskExecutor.getThreadPoolExecutor();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("corePoolSize", executor.getCorePoolSize());
        stats.put("maxPoolSize", executor.getMaximumPoolSize());
        stats.put("poolSize", executor.getPoolSize());
        stats.put("activeThreads", executor.getActiveCount());
        stats.put("queued", executor.getQueue().size());
        stats.put("queueCapacity", AsyncConfig.QUEUE_CAPACITY);
        stats.put("completedTasks", executor.getCompletedTaskCount());
        return stats;
    }

    // --------------------------------------------------------------------
    // What the background tasks did after their response was sent
    // --------------------------------------------------------------------

    @GetMapping("/events")
    public List<AsyncEventStore.Event> events() {
        return store.list();
    }

    @DeleteMapping("/events")
    public Map<String, Object> clearEvents() {
        return Map.of("cleared", store.clear());
    }

    private Map<String, Object> timed(String mode, long start, List<TaskResult> results) {
        long total = System.currentTimeMillis() - start;
        log.info("{} finished in {}ms", mode, total);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", mode);
        response.put("requestThread", Thread.currentThread().getName());
        response.put("totalMs", total);
        response.put("sumOfTasksMs", results.stream().mapToLong(TaskResult::tookMs).sum());
        response.put("tasks", results);
        return response;
    }
}
