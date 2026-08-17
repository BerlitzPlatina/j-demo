package com.example.async.task;

import com.example.async.model.TaskResult;
import com.example.async.store.AsyncEventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Task factory: the work the demo endpoints call. Every method reports the thread it ran on, which
 * is the whole point, because that is what tells a synchronous call apart from an asynchronous one.
 * </p>
 *
 * <p>
 * {@code @Async} works through a proxy: Spring wraps this bean and the annotated methods are only
 * handed to an executor when they are called <em>through</em> that proxy, that is from another
 * bean. See {@link #runViaSelfInvocation(int)}.
 * </p>
 *
 * @author yangkai.shen, NamHoang
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskFactory {

    private final AsyncEventStore store;

    // --------------------------------------------------------------------
    // Blocking versions, for the sequential baseline
    // --------------------------------------------------------------------

    /**
     * Simulates a 5 second blocking task.
     */
    public TaskResult task1() throws InterruptedException {
        return doTask("task1", 5);
    }

    /**
     * Simulates a 2 second blocking task.
     */
    public TaskResult task2() throws InterruptedException {
        return doTask("task2", 2);
    }

    /**
     * Simulates a 3 second blocking task.
     */
    public TaskResult task3() throws InterruptedException {
        return doTask("task3", 3);
    }

    // --------------------------------------------------------------------
    // Async versions of the same three tasks
    // --------------------------------------------------------------------

    /**
     * Simulates a 5 second async task. CompletableFuture is the return type to prefer: it can be
     * combined and composed, while the older {@code Future} can only be blocked on.
     */
    @Async
    public CompletableFuture<TaskResult> asyncTask1() throws InterruptedException {
        return CompletableFuture.completedFuture(doTask("asyncTask1", 5));
    }

    /**
     * Simulates a 2 second async task.
     */
    @Async
    public CompletableFuture<TaskResult> asyncTask2() throws InterruptedException {
        return CompletableFuture.completedFuture(doTask("asyncTask2", 2));
    }

    /**
     * Simulates a 3 second async task.
     */
    @Async
    public CompletableFuture<TaskResult> asyncTask3() throws InterruptedException {
        return CompletableFuture.completedFuture(doTask("asyncTask3", 3));
    }

    // --------------------------------------------------------------------
    // The individual behaviours the endpoints demonstrate
    // --------------------------------------------------------------------

    /**
     * Generic async task on the default pool.
     */
    @Async
    public CompletableFuture<TaskResult> asyncSleep(String name, int seconds) throws InterruptedException {
        return CompletableFuture.completedFuture(doTask(name, seconds));
    }

    /**
     * Returns void, so the caller cannot wait for it and cannot see it fail: fire and forget. The
     * outcome is recorded in the event store instead.
     */
    @Async
    public void fireAndForget(String name, int seconds) throws InterruptedException {
        TaskResult result = doTask(name, seconds);
        store.add("done", name, result.thread(), "took " + result.tookMs() + "ms");
    }

    /**
     * Fails on purpose. Void return, so the exception reaches the AsyncUncaughtExceptionHandler
     * configured in AsyncConfig and nothing propagates to the caller.
     */
    @Async
    public void failVoid() {
        log.info("failVoid running on [{}]", Thread.currentThread().getName());
        throw new IllegalStateException("void async task failed on purpose");
    }

    /**
     * Fails on purpose too, but returns a future, so the caller sees the exception when reading the
     * result: wrapped in ExecutionException by get(), in CompletionException by join().
     */
    @Async
    public CompletableFuture<TaskResult> failFuture() throws InterruptedException {
        log.info("failFuture running on [{}]", Thread.currentThread().getName());
        TimeUnit.MILLISECONDS.sleep(300);
        return CompletableFuture.failedFuture(new IllegalStateException("async task failed on purpose"));
    }

    /**
     * Pinned to the single thread executor, so concurrent calls queue up behind each other.
     */
    @Async("singleThreadExecutor")
    public CompletableFuture<TaskResult> onSingleThread(String name, int seconds) throws InterruptedException {
        return CompletableFuture.completedFuture(doTask(name, seconds));
    }

    /**
     * Runs on a fresh virtual thread instead of a pooled platform thread.
     */
    @Async("virtualThreadExecutor")
    public CompletableFuture<TaskResult> onVirtualThread(String name, int seconds) throws InterruptedException {
        return CompletableFuture.completedFuture(doTask(name, seconds));
    }

    /**
     * The classic trap: calling an {@code @Async} method from inside the same bean goes straight to
     * the method, not through the proxy, so the annotation has no effect and the task runs on the
     * caller's thread. The returned result shows the caller's thread name to prove it.
     */
    public TaskResult runViaSelfInvocation(int seconds) throws InterruptedException {
        return asyncSleep("selfInvocation", seconds).join();
    }

    private TaskResult doTask(String taskName, int seconds) throws InterruptedException {
        Thread current = Thread.currentThread();
        long start = System.currentTimeMillis();
        log.info("{} started on thread [{}]", taskName, current.getName());

        // Sleep stands in for blocking IO: a database call, an HTTP call, a file write.
        TimeUnit.SECONDS.sleep(seconds);

        long took = System.currentTimeMillis() - start;
        log.info("{} finished on thread [{}] in {}ms", taskName, current.getName(), took);
        return new TaskResult(taskName, current.getName(), current.isVirtual(), MDC.get("traceId"), took);
    }
}
