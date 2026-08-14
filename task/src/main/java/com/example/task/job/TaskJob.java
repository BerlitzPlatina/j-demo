package com.example.task.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Scheduled task examples. Each method logs the gap since its own previous start, so the
 * difference between the three scheduling styles is visible in the log.
 * </p>
 *
 * @author NamHoang
 */
@Component
@Slf4j
public class TaskJob {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * How long each job pretends to work. Both fixed-rate and fixed-delay jobs take this long,
     * which is what makes their different spacing show up.
     */
    private static final long WORK_SECONDS = 2;

    private LocalTime lastCron;
    private LocalTime lastFixedRate;
    private LocalTime lastFixedDelay;

    /**
     * Cron: runs at wall-clock positions, here at second 0, 10, 20, 30, 40 and 50 of every minute.
     * The six fields are second, minute, hour, day-of-month, month, day-of-week.
     */
    @Scheduled(cron = "0/10 * * * * ?")
    public void cronJob() {
        lastCron = logStart("cron       ", lastCron);
    }

    /**
     * Fixed rate: the next run is scheduled 3s after the previous run *started*. Since the work
     * takes 2s, runs start every 3s — the scheduler does not wait for the work to finish.
     * <p>
     * If the work took longer than the period, the next run would not overlap but would queue up
     * and start immediately after, because one task is never run concurrently with itself.
     */
    @Scheduled(fixedRate = 3000)
    public void fixedRateJob() {
        lastFixedRate = logStart("fixedRate  ", lastFixedRate);
        work();
    }

    /**
     * Fixed delay: the next run is scheduled 3s after the previous run *finished*. With 2s of
     * work, runs start every 5s. {@code initialDelay} holds the first run back by 5s after startup.
     */
    @Scheduled(fixedDelay = 3000, initialDelay = 5000)
    public void fixedDelayJob() {
        lastFixedDelay = logStart("fixedDelay ", lastFixedDelay);
        work();
    }

    /**
     * Logs when this run started and how long it has been since the previous one.
     *
     * @return the start time of this run, to be remembered for the next one
     */
    private LocalTime logStart(String name, LocalTime previous) {
        LocalTime now = LocalTime.now();
        String gap = previous == null ? "first run" : Duration.between(previous, now).toMillis() / 1000.0 + "s since last start";
        log.info("[{}] {} ({}) on thread {}", name, now.format(TIME), gap, Thread.currentThread().getName());
        return now;
    }

    /**
     * Stands in for real work so the scheduling styles differ in a visible way.
     */
    private void work() {
        try {
            TimeUnit.SECONDS.sleep(WORK_SECONDS);
        } catch (InterruptedException e) {
            // Restore the flag so the pool can shut the thread down during application shutdown.
            Thread.currentThread().interrupt();
        }
    }
}
