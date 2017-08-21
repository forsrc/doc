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

    private static final int SECONDS_TO_RUN = 5;

    private static final int RECORDS_PER_SECOND = 200;

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) throws InterruptedException {
        final AtomicLong sequenceNumber = new AtomicLong(0);
        final AtomicLong completed = new AtomicLong(0);
        final FutureCallback<String> callback = new FutureCallback<String>() {
            @Override
            public void onFailure(Throwable t) {
                System.out.println(t.getMessage());
            }

            @Override
            public void onSuccess(String result) {
                completed.getAndIncrement();
                System.out.println(result);
            }
        };

        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

        final Runnable putOneRecord = new Runnable() {
            @Override
            public void run() {

                // TIMESTAMP is our partition key
                ListenableFuture<String> f = executorService.submit(new Callable<String>() {
                    public String call() {
                        return "" + System.currentTimeMillis();
                    }
                });

                Futures.addCallback(f, callback);
            }
        };

        EXECUTOR.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                printRate(sequenceNumber, completed);
            }
        }, 1, 1, TimeUnit.SECONDS);

        executeAtTargetRate(EXECUTOR, putOneRecord, sequenceNumber, SECONDS_TO_RUN, RECORDS_PER_SECOND);

        EXECUTOR.awaitTermination(SECONDS_TO_RUN + 1, TimeUnit.SECONDS);
        printRate(sequenceNumber, completed);
        System.out.println("Finished.");
    }

    private static void printRate(AtomicLong sequenceNumber, AtomicLong completed) {
        long put = sequenceNumber.get();
        long total = RECORDS_PER_SECOND * SECONDS_TO_RUN;
        double putPercent = 100.0 * put / total;
        long done = completed.get();
        double donePercent = 100.0 * done / total;
        System.out.println(String.format("--> %d of %d so far (%.2f %%), %d have completed (%.2f %%)", put,
                total, putPercent, done, donePercent));
    }

    private static void executeAtTargetRate(
            final ScheduledExecutorService exec,
            final Runnable task,
            final AtomicLong counter,
            final int durationSeconds,
            final int ratePerSecond) {

        exec.scheduleWithFixedDelay(new Runnable() {
            final long startTime = System.nanoTime();

            @Override
            public void run() {
                double secondsRun = (System.nanoTime() - startTime) / 1e9;
                double targetCount = Math.min(durationSeconds, secondsRun) * ratePerSecond;

                while (counter.get() < targetCount) {
                    counter.getAndIncrement();
                    try {
                        task.run();
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
