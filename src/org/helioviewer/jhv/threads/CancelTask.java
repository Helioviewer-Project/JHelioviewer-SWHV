package org.helioviewer.jhv.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class CancelTask extends FutureTask<Boolean> {

    public CancelTask(FutureTask<?> task) {
        super(new TaskCancel(task));
    }

    private static class TaskCancel implements Callable<Boolean> {

        private final FutureTask<?> task;

        public TaskCancel(FutureTask<?> _task) {
            task = _task;
        }

        @Override
        public Boolean call() {
            return task.cancel(true);
        }

    }

}
