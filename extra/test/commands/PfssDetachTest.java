package org.helioviewer.jhv.plugins.pfss;

import java.util.List;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.thread.UiThread;

public final class PfssDetachTest {

    public static void main(String[] args) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        UiThread.postAndWait(() -> {
            DisplayController.setRenderRequestHandler(_ -> {});
            Layers.restore(List.of());
            for (boolean restore : new boolean[]{false, true}) {
                Probe layer = new Probe();
                try {
                    Layers.add(layer);
                    layer.setEnabled(true);
                    if (restore)
                        Layers.restore(List.of());
                    else
                        Layers.remove(layer);
                    int requests = layer.requests;
                    Layers.setActiveImageLayer(null);
                    check(layer.requests == requests, "Removed PFSS layer received a range update");
                    check(!layer.isEnabled(), "Removed PFSS layer stayed enabled");
                } finally {
                    layer.setEnabled(false);
                    Layers.remove(layer);
                }
            }
        });
        System.out.println("PfssDetachTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    private static final class Probe extends PfssLayer {
        private int requests;

        Probe() { super(null); }

        @Override
        public void timeRangeChanged(long start, long end) { requests++; }
    }
}
