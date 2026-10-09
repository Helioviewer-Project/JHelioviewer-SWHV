package org.helioviewer.jhv.layers;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.Region;
import org.helioviewer.jhv.movie.ExportMovie;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.thread.EDTTimer;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.wcs.ImageBounds;

public final class ImageLayers {

    public static boolean decode(Position viewpoint) {
        boolean recording = ExportMovie.isRecording(); // the finest level, whatever the display needs
        boolean priority = !Player.isPlaying();
        boolean decoded = false;
        for (ImageLayer layer : Layers.getImageLayers()) {
            int idx = layer.isVisibleIdx();
            if (idx != -1) {
                double pixFactor = recording ? Double.POSITIVE_INFINITY : DisplayController.getImagePixelFactor(Display.getViewport(idx));
                layer.decode(viewpoint, pixFactor, priority);
                decoded = true;
            }
        }
        return decoded;
    }

    public static double getLargestPhysicalHeight() {
        double size = 0;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (!layer.isEnabled())
                continue;
            size = Math.max(size, layer.getMetaData().getPhysicalRegion().height);
        }
        return size;
    }

    public static double getLargestRadialSize() {
        double size = 0;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (!layer.isEnabled())
                continue;
            size = Math.max(size, ImageBounds.radial(layer.getMetaData()));
        }
        return size;
    }

    public static Region computeHpcScaleBounds() {
        Region bounds = getHpcImageBounds();
        double halfWidth = Math.max(Math.abs(bounds.llx), Math.abs(bounds.urx));
        double halfHeight = Math.max(Math.abs(bounds.lly), Math.abs(bounds.ury));
        if (halfWidth <= 0)
            halfWidth = 5;
        if (halfHeight <= 0)
            halfHeight = 5;
        return new Region(-halfWidth, -halfHeight, 2 * halfWidth, 2 * halfHeight);
    }

    private static Region getHpcImageBounds() {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (!layer.isEnabled())
                continue;

            Region bounds = ImageBounds.hpc(layer.getMetaData());
            minX = Math.min(minX, bounds.llx);
            maxX = Math.max(maxX, bounds.urx);
            minY = Math.min(minY, bounds.lly);
            maxY = Math.max(maxY, bounds.ury);
        }
        if (!Double.isFinite(minX) || !Double.isFinite(maxX) || !Double.isFinite(minY) || !Double.isFinite(maxY))
            return new Region(-5, -5, 10, 10);
        return new Region(minX, minY, Math.max(Math.nextUp(0.0), maxX - minX), Math.max(Math.nextUp(0.0), maxY - minY));
    }

    static void displaySynced(Position viewpoint) { // coalesce layers
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (layer.isVisibleIdx() == -1)
                continue;
            ImageData data = layer.getImageData();
            if (data != null && viewpoint != data.viewpoint() /* deliberate on reference */)
                return;
        }
        DisplayController.display(viewpoint);
    }

    public static void arrangeMultiView() {
        if (Display.multiview) {
            int ct = 0;
            for (ImageLayer layer : Layers.getImageLayers()) {
                if (layer.isEnabled()) {
                    layer.setVisible(ct);
                    ct++;
                }
            }
        } else {
            for (ImageLayer layer : Layers.getImageLayers()) {
                if (layer.isEnabled())
                    layer.setVisible(0);
            }
        }
        Display.reshapeAll();
        DisplayController.render();
    }

    @Nullable
    static ImageLayer getImageLayerInViewport(int idx) {
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (layer.isVisible(idx))
                return layer;
        }
        return null;
    }

    public static void syncLayersSpan(long startTime, long endTime, int cadence) {
        for (ImageLayer layer : Layers.getImageLayers())
            layer.reload(startTime, endTime, cadence);
    }

    private static boolean diffRotationMode;

    public static boolean getDiffRotationMode() {
        return diffRotationMode;
    }

    public static void setDiffRotationMode(boolean b) {
        diffRotationMode = b;
    }

    private static final EDTTimer refreshTimer;
    private static final int timerDelay = 15 * (int) TimeUtils.MINUTE_IN_MILLIS;

    static {
        refreshTimer = new EDTTimer(timerDelay, ImageLayers::refreshLayersSpan);
        refreshTimer.setInitialDelay(0);
    }

    private static boolean refreshMode;

    public static boolean getRefreshMode() {
        return refreshMode;
    }

    public static void setRefreshMode(boolean b) {
        refreshMode = b;
        if (refreshMode)
            refreshTimer.start();
        else
            refreshTimer.stop();
    }

    private static void refreshLayersSpan() {
        long now = System.currentTimeMillis();
        for (ImageLayer layer : Layers.getImageLayers()) {
            APIRequest req = layer.getAPIRequest();
            if (req != null)
                layer.reload(now - (req.endTime() - req.startTime()), now, req.cadence());
        }
    }

    private ImageLayers() {}
}
