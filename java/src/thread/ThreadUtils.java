package com.forsrc.utils;

import java.util.concurrent.Callable;

import java.util.concurrent.Executors;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicLong;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class ThreadUtils {

    // private static final int SECONDS_TO_RUN = 1;

    // private static final int RECORDS_PER_SECOND = 10000;

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) throws InterruptedException {
        run(1, 1000, new Callable<String>() {
            @Override
            public String call() throws Exception {

                return System.currentTimeMillis() + "";
            }

        }, new FutureCallback<String>() {
            @Override
            public void onFailure(Throwable t) {
                System.out.println(t.getMessage());
            }
            @Override
            public void onSuccess(String result) {
                // System.out.println(t.getMessage());
            }
        });
    }

    public static <T> void run(int secondsToRun, int pecordsPerSecond, Callable<T> callable, FutureCallback<T> callback)
            throws InterruptedException {
        final AtomicLong sequenceNumber = new AtomicLong(0);
        final AtomicLong completed = new AtomicLong(0);
        final FutureCallback<T> futureCallback = new FutureCallback<T>() {
            @Override
            public void onFailure(Throwable t) {
                System.out.println(t.getMessage());
                callback.onFailure(t);
            }

            @Override
            public void onSuccess(T result) {
                callback.onSuccess(result);
                completed.getAndIncrement();
            }
        };

        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

        final Runnable putOneRecord = new Runnable() {
            @Override
            public void run() {
                ListenableFuture<T> f = executorService.submit(callable);
                Futures.addCallback(f, futureCallback);
            }
        };

        EXECUTOR.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                printRate(secondsToRun, pecordsPerSecond, sequenceNumber, completed);
            }
        }, 100, 100, TimeUnit.MILLISECONDS);

        executeAtTargetRate(EXECUTOR, putOneRecord, sequenceNumber, secondsToRun, pecordsPerSecond);

        EXECUTOR.awaitTermination(secondsToRun + 1, TimeUnit.SECONDS);
        printRate(secondsToRun, pecordsPerSecond, sequenceNumber, completed);
        System.out.println("Finished.");
    }

    private static void printRate(int secondsToRun, int pecordsPerSecond, AtomicLong sequenceNumber,
            AtomicLong completed) {
        long put = sequenceNumber.get();
        long total = pecordsPerSecond * secondsToRun;
        double putPercent = 100.0 * put / total;
        long done = completed.get();
        double donePercent = 100.0 * done / total;
        System.out.println(String.format("--> %d of %d so far (%.2f %%), %d have completed (%.2f %%)", put, total,
                putPercent, done, donePercent));
    }

    private static void executeAtTargetRate(final ScheduledExecutorService exec, final Runnable task,
            final AtomicLong counter, final int durationSeconds, final int ratePerSecond) {

        exec.scheduleWithFixedDelay(new Runnable() {
            final long startTime = System.nanoTime();

            @Override
            public void run() {
                double secondsRun = (System.nanoTime() - startTime) / 1e9;
                double targetCount = Math.min(durationSeconds, secondsRun) * ratePerSecond;

                while (counter.get() < targetCount) {
                    try {
                        task.run();
                        counter.getAndIncrement();
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }

                if (secondsRun >= durationSeconds) {
                    exec.shutdown();
                }
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
    }

}
