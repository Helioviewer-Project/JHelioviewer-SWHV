package org.helioviewer.jhv.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class CancelTask extends FutureTask<Void> {

    public CancelTask(FutureTask<?> abolishTask, String threadName) {
        super(new Runnable() {
                private FutureTask<?> abolishTask;
                private String threadName;

                public Runnable init(FutureTask<?> abolishTask, String threadName) {
                    this.abolishTask = abolishTask;
                    this.threadName = threadName;
                    return this;
                }

                @Override
                public void run() {
                    Thread.currentThread().setName(threadName);
                    abolishTask.cancel(true);
                }
            }.init(abolishTask, threadName), null);
    }

}
