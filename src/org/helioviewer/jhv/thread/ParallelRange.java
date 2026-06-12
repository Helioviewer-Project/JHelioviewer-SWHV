package org.helioviewer.jhv.thread;

import java.util.stream.IntStream;

public final class ParallelRange {
    private static final int PROCESSORS = Runtime.getRuntime().availableProcessors();

    @FunctionalInterface
    public interface Task {
        void run(int from, int to);
    }

    public static void run(int length, Task task) {
        if (length <= 0)
            return;

        int chunks = Math.min(PROCESSORS, length);
        IntStream.range(0, chunks).parallel().forEach(chunk -> {
            int from = chunk * length / chunks;
            int to = (chunk + 1) * length / chunks;
            task.run(from, to);
        });
    }

    private ParallelRange() {}
}
