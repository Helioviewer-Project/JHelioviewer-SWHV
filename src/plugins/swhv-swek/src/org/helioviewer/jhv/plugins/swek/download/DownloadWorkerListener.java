package org.helioviewer.jhv.plugins.swek.download;

public interface DownloadWorkerListener {
    /**
     * The worker was started.
     * 
     * @param worker
     *            the worker that was started
     */
    public abstract void workerStarted(DownloadWorker worker);

    /**
     * The worker was forced to stop.
     * 
     * @param worker
     *            the worker that was forced to stop
     */
    public abstract void workerForcedToStop(DownloadWorker worker);

    /**
     * The worker finished.
     * 
     * @param worker
     *            The worker that finished
     */
    public abstract void workerFinished(DownloadWorker worker);
}
