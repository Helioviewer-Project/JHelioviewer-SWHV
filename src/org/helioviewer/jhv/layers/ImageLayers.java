package org.helioviewer.jhv.layers;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.ImageRequestSettings;
import org.helioviewer.jhv.math.Region;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.movie.ExportMovie;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.thread.UiTimer;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.wcs.ImageBounds;
import org.helioviewer.jhv.wcs.ImageGeometry;

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
            ImageGeometry geometry = layer.getMetaData().getGeometry();
            // Surface maps are rendered on the unit sphere, including partial maps.
            size = Math.max(size, geometry.wcs.projection.isSurfaceMap() ? 2 : geometry.bounds.height);
        }
        return size;
    }

    public static double getLargestRadialSize(Position viewpoint) {
        double size = 0;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (!layer.isEnabled())
                continue;
            MetaData metaData = layer.getMetaData();
            ImageGeometry geometry = metaData.getGeometry();
            double distance = geometry.tracingViewpoint(viewpoint, metaData.getViewpoint()).distance;
            size = Math.max(size, ImageBounds.radial(geometry, distance));
        }
        return size;
    }

    public static Region computeHpcScaleBounds(Position viewpoint) {
        Region bounds = getHpcImageBounds(viewpoint);
        double halfWidth = Math.max(Math.abs(bounds.llx), Math.abs(bounds.urx));
        double halfHeight = Math.max(Math.abs(bounds.lly), Math.abs(bounds.ury));
        if (halfWidth <= 0)
            halfWidth = 5;
        if (halfHeight <= 0)
            halfHeight = 5;
        return new Region(-halfWidth, -halfHeight, 2 * halfWidth, 2 * halfHeight);
    }

    private static Region getHpcImageBounds(Position viewpoint) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (!layer.isEnabled())
                continue;

            MetaData metaData = layer.getMetaData();
            ImageGeometry geometry = metaData.getGeometry();
            double distance = geometry.tracingViewpoint(viewpoint, metaData.getViewpoint()).distance;
            Region bounds = ImageBounds.hpc(geometry, distance);
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

    // Reloads every remote layer with the requested interval and sampling.
    public static void syncLayersSpan() {
        ImageRequestSettings settings = ImageRequestSettings.instance();
        for (ImageLayer layer : Layers.getImageLayers())
            layer.reload(settings.start(), settings.requestEnd(), settings.cadence());
    }

    // Takes the interval first: the timeline lock and a layer's own range sync through here.
    public static void syncLayersSpan(long start, long end) {
        ImageRequestSettings.instance().setInterval(start, end);
        syncLayersSpan();
    }

    private static boolean diffRotationMode;

    public static boolean getDiffRotationMode() {
        return diffRotationMode;
    }

    public static void setDiffRotationMode(boolean b) {
        diffRotationMode = b;
    }

    private static final UiTimer refreshTimer = new UiTimer(15 * (int) TimeUtils.MINUTE_IN_MILLIS, ImageLayers::refreshLayersSpan);

    public static boolean getRefreshMode() {
        return refreshTimer.isRunning();
    }

    public static void setRefreshMode(boolean b) {
        if (b)
            refreshTimer.start();
        else
            refreshTimer.stop();
    }

    public static void refreshLayersSpan() {
        long now = System.currentTimeMillis();
        for (ImageLayer layer : Layers.getImageLayers()) {
            APIRequest req = layer.getAPIRequest();
            if (req != null)
                layer.load(req.endingAt(now));
        }
    }

    private ImageLayers() {}
}
