package org.helioviewer.jhv.thread;

import java.awt.EventQueue;

import javax.annotation.Nonnull;

// The AWT event dispatch thread as the UI thread.
final class AwtUiThread implements UiThread.Impl {

    @Override
    public void post(@Nonnull Runnable task) {
        EventQueue.invokeLater(task);
    }

    @Override
    public boolean isUiThread() {
        return EventQueue.isDispatchThread();
    }

}
