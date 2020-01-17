package org.helioviewer.jhv.plugins.pfss;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.plugins.pfss.data.PfssCache;
import org.helioviewer.jhv.threads.JHVThread;
import org.json.JSONObject;

public class PfssPlugin extends Plugin {

    private static final PfssCache pfssCache = new PfssCache();
    private static final PfssLayer layer = new PfssLayer(null);

    public static int downloads;

    private static final BlockingQueue<Runnable> newLoadBlockingQueue = new ArrayBlockingQueue<>(1);
    public static final ExecutorService pfssNewLoadPool = new ThreadPoolExecutor(0, 1, 10L, TimeUnit.MINUTES, newLoadBlockingQueue, new JHVThread.NamedThreadFactory("PFSS NewLoad"), new ThreadPoolExecutor.DiscardPolicy()) {
        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            JHVThread.afterExecute(r, t);
        }
    };

    public static final ScheduledExecutorService pfssReaperPool = new ScheduledThreadPoolExecutor(1, new JHVThread.NamedThreadFactory("PFSS Reaper"), new ThreadPoolExecutor.DiscardPolicy());

    public PfssPlugin() {
        super("PFSS", "Visualize PFSS model data");
    }

    public static PfssCache getPfsscache() {
        return pfssCache;
    }

    @Override
    public void install() {
        downloads = 0;
        JHVFrame.getLayers().add(layer);
    }

    @Override
    public void uninstall() {
        JHVFrame.getLayers().remove(layer);
        pfssCache.clear();
    }

    @Override
    public void saveState(JSONObject jo) {
    }

    @Override
    public void loadState(JSONObject jo) {
    }

}
