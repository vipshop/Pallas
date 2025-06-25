package com.vip.pallas.thread;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class ExtendableThreadPoolExecutorTest {

    static class SleepTask implements Runnable {
        private final CountDownLatch latch;

        SleepTask(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                TimeUnit.MILLISECONDS.sleep(300); // shorter, non-blocking sleep
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    public void testNormalUsage() throws InterruptedException {
        TaskQueue workQueue = new TaskQueue(1);
        ExtendableThreadPoolExecutor executor = new ExtendableThreadPoolExecutor(
                1, 2, 1, TimeUnit.MINUTES,
                workQueue, new PallasThreadFactory("test-ExtendableThreadPoolExecutor-thread")
        );

        CountDownLatch latch = new CountDownLatch(3);
        executor.submit(new SleepTask(latch));
        executor.submit(new SleepTask(latch));
        executor.submit(new SleepTask(latch));

        boolean finished = latch.await(2, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        assertThat(executor.getActiveCount()).isLessThanOrEqualTo(2);
        assertThat(workQueue.size()).isBetween(0, 1);
        assertThat(executor.getSubmittedCount()).isEqualTo(3);
    }

    @Test(expected = RejectedExecutionException.class)
    public void testThrowException() {
        TaskQueue workQueue = new TaskQueue(1);
        ExtendableThreadPoolExecutor executor = new ExtendableThreadPoolExecutor(
                1, 1, 1, TimeUnit.MINUTES,
                workQueue, new PallasThreadFactory("test-ExtendableThreadPoolExecutor-thread")
        );

        executor.submit(() -> sleepQuietly(300));
        executor.submit(() -> sleepQuietly(300));
        executor.submit(() -> sleepQuietly(300)); // Should throw
    }

    private void sleepQuietly(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
