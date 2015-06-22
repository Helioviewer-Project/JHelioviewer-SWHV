package org.helioviewer.jhv.threads;

import java.util.concurrent.FutureTask;

public class CancelTask extends FutureTask<Void> {

    public CancelTask(FutureTask<?> abolishTask) {
        super(new Runnable() {
                private FutureTask<?> abolishTask;

                public Runnable init(FutureTask<?> abolishTask) {
                    this.abolishTask = abolishTask;
                    return this;
                }

                @Override
                public void run() {
                    abolishTask.cancel(true);
                }
            }.init(abolishTask), null);
    }

}
