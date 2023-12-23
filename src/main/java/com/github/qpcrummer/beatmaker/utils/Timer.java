package com.github.qpcrummer.beatmaker.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class Timer {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    public static void wait(long milliseconds, Runnable task) {
        scheduler.schedule(task, milliseconds, TimeUnit.MILLISECONDS);
    }

    public static void wait(double seconds, Runnable task) {
        scheduler.schedule(task, (long) (seconds * 1000), TimeUnit.MILLISECONDS);
    }
}
