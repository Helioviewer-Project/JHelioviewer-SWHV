package org.helioviewer.jhv.layers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.FitsMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.thread.EDTQueue;
import org.helioviewer.jhv.thread.EDTTimer;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.wcs.ImageBounds;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.SampUtils;

public final class ImageLayers {

    public static boolean decode(float factor, Position viewpoint) {
        boolean decoded = false;
        for (ImageLayer layer : Layers.getImageLayers()) {
            int idx = layer.isVisibleIdx();
            if (idx != -1) {
                double pixFactor = DisplayController.getImagePixelFactor(Display.getViewport(idx));
                layer.getView().decode(viewpoint, pixFactor, factor);
                decoded = true;
            }
        }
        return decoded;
    }

    // R_sun; a disk imager's nearest-edge FOV reaches at most about this far
    private static final double DISK_IMAGER_MAX_RADIUS = 2;

    // A disk imager shows the solar disk; it is rendered flat (no radial warp) in a disk
    // projection, while coronagraphs keep the warp (see ImageLayer.render). JP2/Helioviewer
    // sources carry no occulter metadata (innerRadius/cutoff/mask are FITS-only), so we can't
    // rely on innerRadius alone — a JP2 coronagraph like LASCO reports innerRadius 0, identical
    // to AIA. Distinguish by field of view: a disk imager's FOV is dominated by the disk
    // (nearest edge within ~2 R_sun), while a coronagraph's extends far past the occulted limb.
    // FITS coronagraphs (e.g. PUNCH) also report innerRadius >= 1.
    public static boolean isDiskImager(@Nullable MetaData m) {
        return m != null && m.getInnerRadius() < 1 && ImageBounds.inscribed(m) < DISK_IMAGER_MAX_RADIUS;
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

    // Inscribed (nearest-edge) radius of the widest layer; the meaningful outer radius for
    // the disk view (beyond it the FOV has only corner data), used to size the range slider.
    public static double getLargestDiskRadius() {
        double size = 0;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (!layer.isEnabled())
                continue;
            size = Math.max(size, ImageBounds.inscribed(layer.getMetaData()));
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
            View.ImageData id;
            if (layer.isEnabled() && (id = layer.getImageData()) != null && viewpoint != id.viewpoint() /* deliberate on reference */)
                return;
        }
        DisplayController.display(viewpoint);
    }

    public record WaitUntilLoaded(Collection<ImageLayer> newLayers) implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            for (ImageLayer layer : newLayers) {
                while (isLoadingForState(layer)) {
                    Thread.sleep(1000);
                }
            }
            return null;
        }

        private static boolean isLoadingForState(ImageLayer layer) throws Exception {
            return EDTQueue.invokeAndWait(() -> Layers.getImageLayers().contains(layer) && !layer.isViewLoadFinished());
        }
    }

    public static void arrangeMultiView(boolean multiview) {
        if (multiview) {
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
        DisplayController.render(1);
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
        for (ImageLayer layer : Layers.getImageLayers()) {
            APIRequest req = layer.getView().getAPIRequest();
            if (req == null)
                continue;
            layer.load(new APIRequest(req.server(), req.sourceId(), startTime, endTime, cadence));
        }
    }

    public static String getSDOCutoutString() {
        StringBuilder str = new StringBuilder("&wavelengths=");
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (!layer.isEnabled())
                continue;

            MetaData m = layer.getMetaData();
            if (!(m instanceof FitsMetaData fm))
                continue;
            if (fm.getObservatory().contains("SDO") && fm.getInstrument().contains("AIA"))
                str.append(',').append(fm.getMeasurement());
        }

        ImageLayer activeLayer = Layers.getActiveImageLayer();
        if (activeLayer != null) {
            APIRequest req;
            if ((req = activeLayer.getView().getAPIRequest()) != null) {
                str.append("&cadence=").append(req.cadence()).append("&cadenceUnits=s");
            }
            View.ImageData id;
            if ((id = activeLayer.getImageData()) != null) {
                Region region = Region.scale(id.region(), 1 / id.metaData().getUnitPerArcsec());
                str.append(String.format("&xCen=%.1f", region.llx + region.width / 2.));
                str.append(String.format("&yCen=%.1f", -(region.lly + region.height / 2.)));
                str.append(String.format("&width=%.1f", region.width));
                str.append(String.format("&height=%.1f", region.height));
            }
        }

        long start = Player.getStartTime();
        str.append("&startDate=").append(TimeUtils.formatDate(start));
        str.append("&startTime=").append(TimeUtils.formatTime(start));
        long end = Player.getEndTime();
        str.append("&stopDate=").append(TimeUtils.formatDate(end));
        str.append("&stopTime=").append(TimeUtils.formatTime(end));
        return str.toString();
    }

    public static void getSAMPMessage(Message msg) {
        View.ImageData id;
        ImageLayer activeLayer = Layers.getActiveImageLayer();
        if (activeLayer == null || activeLayer.getView().getAPIRequest() == null || (id = activeLayer.getImageData()) == null)
            return;

        APIRequest req = activeLayer.getView().getAPIRequest();
        msg.addParam("timestamp", Player.getTime().toString());
        msg.addParam("start", TimeUtils.format(Player.getStartTime()));
        msg.addParam("end", TimeUtils.format(Player.getEndTime()));
        msg.addParam("cadence", SampUtils.encodeLong(req.cadence() * 1000L));
        msg.addParam("cutout.set", SampUtils.encodeBoolean(true));

        Region region = Region.scale(id.region(), 1 / id.metaData().getUnitPerArcsec());
        msg.addParam("cutout.x0", SampUtils.encodeFloat(region.llx + region.width / 2.));
        msg.addParam("cutout.y0", SampUtils.encodeFloat(-(region.lly + region.height / 2.)));
        msg.addParam("cutout.w", SampUtils.encodeFloat(region.width));
        msg.addParam("cutout.h", SampUtils.encodeFloat(region.height));

        ArrayList<HashMap<String, String>> layersData = new ArrayList<>();
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (!layer.isEnabled() || (id = layer.getImageData()) == null)
                continue;

            if (id.metaData() instanceof FitsMetaData fm) {
                HashMap<String, String> layerMsg = new HashMap<>();
                layerMsg.put("observatory", fm.getObservatory());
                layerMsg.put("instrument", fm.getInstrument());
                layerMsg.put("detector", fm.getDetector());
                layerMsg.put("measurement", fm.getMeasurement());
                layerMsg.put("timestamp", fm.getViewpoint().time.toString());
                layersData.add(layerMsg);
            }
        }
        msg.addParam("layers", layersData);
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
            APIRequest req = layer.getView().getAPIRequest();
            if (req == null)
                continue;
            layer.load(new APIRequest(req.server(), req.sourceId(), now - (req.endTime() - req.startTime()), now, req.cadence()));
        }
    }

    private ImageLayers() {}
}
