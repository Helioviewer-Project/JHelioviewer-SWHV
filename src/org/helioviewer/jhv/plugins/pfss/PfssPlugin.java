package org.helioviewer.jhv.plugins.pfss;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.pfss.data.PfssCache;
import org.helioviewer.jhv.threads.JHVThread;
import org.json.JSONObject;

public class PfssPlugin implements Plugin {

    private static final PfssCache pfssCache = new PfssCache();
    private static final PfssRenderable renderable = new PfssRenderable();

    private static final BlockingQueue<Runnable> newLoadBlockingQueue = new ArrayBlockingQueue<>(1);
    public static final ExecutorService pfssNewLoadPool = new ThreadPoolExecutor(0, 1, 10L, TimeUnit.MINUTES, newLoadBlockingQueue, new JHVThread.NamedThreadFactory("PFSS NewLoad"), new ThreadPoolExecutor.DiscardPolicy()) {
        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            JHVThread.afterExecute(r, t);
        }
    };

    public static final ScheduledExecutorService pfssReaperPool = new ScheduledThreadPoolExecutor(1, new JHVThread.NamedThreadFactory("PFSS Reaper"), new ThreadPoolExecutor.DiscardPolicy());

    private static final BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(1024);
    public static final ExecutorService pfssDataPool = new ThreadPoolExecutor(0, 5, 10L, TimeUnit.MINUTES, blockingQueue, new JHVThread.NamedThreadFactory("PFSS DataLoad"), new ThreadPoolExecutor.DiscardPolicy()) {
        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            JHVThread.afterExecute(r, t);
        }
    };

    public static PfssCache getPfsscache() {
        return pfssCache;
    }

    @Override
    public void installPlugin() {
        Layers.addTimespanListener(renderable);
        ImageViewerGui.getRenderableContainer().addRenderable(renderable);
    }

    @Override
    public void uninstallPlugin() {
        ImageViewerGui.getRenderableContainer().removeRenderable(renderable);
        Layers.removeTimespanListener(renderable);
    }

    @Override
    public String getDescription() {
        return "This plugin visualizes PFSS model data";
    }

    @Override
    public String getName() {
        return "PFSS Plugin";
    }

    @Override
    public String getAboutLicenseText() {
        return "Mozilla Public License Version 2.0";
    }

    @Override
    public void saveState(JSONObject jo) {
    }

    @Override
    public void loadState(JSONObject jo) {
    }

}
