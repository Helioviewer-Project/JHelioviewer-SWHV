package org.helioviewer.viewmodel.view.jp2view;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.threads.JHVThread.J2KRenderThreadFactory;

public class J2KRenderThreadExecutor extends ThreadPoolExecutor {
    Object var;

    public J2KRenderThreadExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, J2KRenderThreadFactory namedThreadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    @Override
    public void execute(Runnable command) {
        if (command instanceof J2KRenderThreadFactory) {
            J2KRenderThreadFactory j2kRender = (J2KRenderThreadFactory) command;
        }
        super.execute(command);
    }
}
