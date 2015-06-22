package org.helioviewer.jhv.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class CancelTask extends FutureTask<Boolean> {

    public CancelTask(FutureTask<?> cancelTask) {
        super(new Callable<Boolean>() {
                private FutureTask<?> _cancelTask;

                public Callable init(FutureTask<?> _cancelTask) {
                    this._cancelTask = _cancelTask;
                    return this;
                }

                @Override
                public Boolean call() {
                    return _cancelTask.cancel(true);
                }
            }.init(cancelTask));
    }

}
