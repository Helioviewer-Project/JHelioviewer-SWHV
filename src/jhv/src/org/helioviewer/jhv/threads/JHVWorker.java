package org.helioviewer.jhv.threads;

import javax.swing.SwingWorker;

public abstract class JHVWorker<T, V> extends SwingWorker<T, V> {

    private String name;

    public String getThreadName() {
        return name;
    }

    public void setThreadName(String _name) {
        name = _name;
    }

    @Override
    protected T doInBackground() {
        String currentName = Thread.currentThread().getName();
        if (name != null)
            Thread.currentThread().setName("JHVWorker-" + name);

        T ret = backgroundWork();

        if (name != null)
            Thread.currentThread().setName(currentName);

        return ret;
    }

    protected abstract T backgroundWork();

}
