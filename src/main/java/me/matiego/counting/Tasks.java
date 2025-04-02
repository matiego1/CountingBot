package me.matiego.counting;

import me.matiego.counting.utils.Logs;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tasks {
    private static final int THREADS = 4;
    private static ExecutorService executorService = null;

    public static void createExecutorService() {
        if (executorService != null && !executorService.isShutdown()) return;
        executorService = Executors.newFixedThreadPool(THREADS);
    }

    public static void shutdownNow() {
        if (executorService == null) return;
        executorService.shutdownNow();
        executorService = null;
    }

    public static void async(@NotNull Runnable task) {
        try {
            createExecutorService();
            executorService.execute(task);
        } catch (Exception e) {
            Logs.error("Failed to run a task async. The task will be run in the same thread.");
            task.run();
        }
    }
}
