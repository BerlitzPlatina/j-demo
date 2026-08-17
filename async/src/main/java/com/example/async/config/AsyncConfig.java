package com.example.async.config;

import com.example.async.store.AsyncEventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <p>
 * Async infrastructure. {@code @EnableAsync} is what makes {@code @Async} do anything at all:
 * without it the annotation is ignored and every call runs on the caller's thread.
 * </p>
 *
 * @author NamHoang
 */
@Configuration
@EnableAsync
@RequiredArgsConstructor
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * The default pool, deliberately small so the queueing behaviour is easy to observe: two
     * threads work, the next four wait in the queue, and anything beyond that runs on the caller's
     * thread because of the CallerRunsPolicy.
     */
    public static final int CORE_POOL_SIZE = 2;

    public static final int MAX_POOL_SIZE = 4;

    public static final int QUEUE_CAPACITY = 4;

    private final AsyncEventStore store;

    /**
     * Used by {@code @Async} when no bean name is given, because it is what
     * {@link #getAsyncExecutor()} returns.
     */
    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("async-");
        // A ThreadPoolExecutor only grows past the core size once the queue is full, so with an
        // unbounded queue maxPoolSize would never be reached. The queue is bounded here for that
        // reason, and the caller runs the task itself when even that is full: slower, but nothing
        // is silently dropped.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(mdcTaskDecorator());
        // Let in-flight tasks finish on shutdown instead of killing them mid-way.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * One thread, so tasks submitted to it are serialized. Handy to show that "async" does not
     * imply "parallel": it only means "not on the caller's thread".
     */
    @Bean("singleThreadExecutor")
    public ThreadPoolTaskExecutor singleThreadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("single-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * A new virtual thread per task (Java 21). There is no pool to size and no queue to overflow,
     * which suits blocking IO work; it does not make CPU bound work faster.
     */
    @Bean("virtualThreadExecutor")
    public SimpleAsyncTaskExecutor virtualThreadExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("virtual-");
        executor.setVirtualThreads(true);
        // A virtual thread is a new thread too, so it starts with an empty MDC just like a pooled
        // one: the decorator is needed here as well.
        executor.setTaskDecorator(mdcTaskDecorator());
        return executor;
    }

    /**
     * The task runs on another thread, so it does not inherit the caller's MDC: a trace id put in
     * by a filter or a controller would be missing from the async log lines. The decorator copies
     * the caller's map into the worker thread and clears it afterwards, so a pooled thread does not
     * leak context into the next task it picks up.
     */
    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> callerContext = MDC.getCopyOfContextMap();
            return () -> {
                if (callerContext == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(callerContext);
                }
                try {
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        };
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }

    /**
     * An exception thrown by an {@code @Async} method that returns void has nowhere to go: the
     * caller is long gone and there is no Future to carry it. Without this handler it is logged by
     * the framework and that is it. Methods returning a Future are different, their exception
     * surfaces when the caller reads the result.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("Async method {} failed with params {}", method.getName(), Arrays.toString(params), ex);
            store.add("failed", method.getName(), Thread.currentThread().getName(), ex.getMessage());
        };
    }
}
