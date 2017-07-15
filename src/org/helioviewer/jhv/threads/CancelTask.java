package org.helioviewer.jhv.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class CancelTask extends FutureTask<Boolean> {

    public CancelTask(Future<?> task) {
        super(new TaskCancel(task));
    }

    private static class TaskCancel implements Callable<Boolean> {

        private final Future<?> task;

        private TaskCancel(Future<?> _task) {
            task = _task;
        }

        @Override
        public Boolean call() {
            return task.cancel(true);
        }

    }

}
