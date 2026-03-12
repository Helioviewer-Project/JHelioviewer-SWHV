package org.helioviewer.jhv.layers;

import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.Nullable;
import javax.swing.Timer;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.TimeUtils;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.SampUtils;

public class ImageLayers {

    static boolean areEnabled() {
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (layer.isEnabled())
                return true;
        }
        return false;
    }

    static void decode(float factor) {
        Camera camera = Display.getCamera();
        Position viewpoint = camera.getViewpoint();

        Layers.forEachImageLayer(layer -> {
            int idx = layer.isVisibleIdx();
            if (idx != -1) {
                double pixFactor = CameraHelper.getImagePixelFactor(camera, Display.getViewport(idx));
                layer.getView().decode(viewpoint, pixFactor, factor);
            }
        });
    }

    static void displaySynced(Position viewpoint) { // coalesce layers
        for (ImageLayer layer : Layers.getImageLayers()) {
            ImageData id;
            if (layer.isEnabled() && (id = layer.getImageData()) != null && viewpoint != id.getViewpoint() /* deliberate on reference */)
                return;
        }
        MovieDisplay.display();
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
        MovieDisplay.render(1);
    }

    @Nullable
    static ImageLayer getImageLayerInViewport(int idx) {
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (layer.isVisible(idx))
                return layer;
        }
        return null;
    }

    public static double getLargestPhysicalHeight() {
        double size = 0;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (layer.isEnabled()) {
                double newSize = layer.getMetaData().getPhysicalRegion().height;
                if (newSize > size)
                    size = newSize;
            }
        }
        return size;
    }

    public static double getLargestPhysicalSize() {
        double size = 0;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (layer.isEnabled()) {
                Region r = layer.getMetaData().getPhysicalRegion();
                double newSize = Math.sqrt(r.height * r.height + r.width * r.width);
                if (newSize > size)
                    size = newSize;
            }
        }
        return size;
    }

    public static Vec2 getLargestHpcExtentsDegrees() {
        double extentX = 0;
        double extentY = 0;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (layer.isEnabled()) {
                Vec2 extent = hpcExtentsDegrees(layer.getMetaData());
                extentX = Math.max(extentX, extent.x);
                extentY = Math.max(extentY, extent.y);
            }
        }
        if (extentX <= 0)
            extentX = 5;
        if (extentY <= 0)
            extentY = 5;
        return new Vec2(extentX, extentY);
    }

    private static Vec2 hpcExtentsDegrees(MetaData metaData) {
        Region region = metaData.getPhysicalRegion();
        double x0 = region.llx;
        double x1 = region.llx + region.width;
        double y0 = region.lly;
        double y1 = region.lly + region.height;
        double xm = 0.5 * (x0 + x1);
        double ym = 0.5 * (y0 + y1);
        Vec2 extent = Vec2.ZERO;
        extent = maxExtent(extent, hpcExtentAt(metaData, x0, y0));
        extent = maxExtent(extent, hpcExtentAt(metaData, x1, y0));
        extent = maxExtent(extent, hpcExtentAt(metaData, x0, y1));
        extent = maxExtent(extent, hpcExtentAt(metaData, x1, y1));
        extent = maxExtent(extent, hpcExtentAt(metaData, xm, y0));
        extent = maxExtent(extent, hpcExtentAt(metaData, xm, y1));
        extent = maxExtent(extent, hpcExtentAt(metaData, x0, ym));
        extent = maxExtent(extent, hpcExtentAt(metaData, x1, ym));
        return extent;
    }

    private static Vec2 maxExtent(Vec2 a, Vec2 b) {
        return new Vec2(Math.max(a.x, b.x), Math.max(a.y, b.y));
    }

    private static Vec2 hpcExtentAt(MetaData metaData, double x, double y) {
        Vec3 plane = metaData.getCROTA().rotateVector(new Vec3(x, y, 0));
        Vec2 helioprojective = inverseWcsPlaneToHpc(metaData, plane.x, plane.y);
        return new Vec2(Math.abs(Math.toDegrees(helioprojective.x)), Math.abs(Math.toDegrees(helioprojective.y)));
    }

    private static Vec2 inverseWcsPlaneToHpc(MetaData metaData, double planeX, double planeY) {
        double unitsPerRad = metaData.getWCSPlaneUnitsPerRad();
        Vec2 crval = metaData.getCRVAL();
        double phi0 = crval.x / unitsPerRad;
        double theta0 = crval.y / unitsPerRad;

        if (metaData.getWCSProjection() == MetaData.WCSProjection.AZP && Math.abs(metaData.getPV2()[2]) < 1e-6f) {
            double x = planeX / unitsPerRad;
            double y = planeY / unitsPerRad;
            double r = Math.hypot(x, y);
            if (r == 0)
                return new Vec2(phi0, theta0);

            double mu = metaData.getPV2()[1];
            double muPlus1 = mu + 1;
            double t;
            if (mu == 1) {
                t = 0.5 * r;
            } else {
                double discriminant = muPlus1 * muPlus1 - r * r * (mu * mu - 1);
                discriminant = Math.max(discriminant, 0);
                t = r * muPlus1 / (muPlus1 + Math.sqrt(discriminant));
            }
            double eta = 2 * Math.atan(t);
            double alpha = Math.atan2(x, y);
            double sinEta = Math.sin(eta);
            double cosEta = Math.cos(eta);
            double a = sinEta * Math.sin(alpha);
            double b = sinEta * Math.cos(alpha);
            return new Vec2(
                    phi0 + Math.atan2(a, cosEta * Math.cos(theta0) - b * Math.sin(theta0)),
                    Math.asin(cosEta * Math.sin(theta0) + b * Math.cos(theta0)));
        }

        double x = planeX / unitsPerRad;
        double y = planeY / unitsPerRad;
        double rho = Math.hypot(x, y);
        if (rho == 0)
            return new Vec2(phi0, theta0);
        double c = Math.atan(rho);
        double sinc = Math.sin(c);
        double cosc = Math.cos(c);
        return new Vec2(
                phi0 + Math.atan2(x * sinc, rho * Math.cos(theta0) * cosc - y * Math.sin(theta0) * sinc),
                Math.asin(cosc * Math.sin(theta0) + y * sinc * Math.cos(theta0) / rho));
    }

    public static void syncLayersSpan(long startTime, long endTime, int cadence) {
        for (ImageLayer layer : Layers.getImageLayers()) {
            APIRequest req = layer.getAPIRequest();
            if (req != null) {
                layer.load(new APIRequest(req.server(), req.sourceId(), startTime, endTime, cadence));
            }
        }
    }

    public static String getSDOCutoutString() {
        StringBuilder str = new StringBuilder("&wavelengths=");
        for (ImageLayer layer : Layers.getImageLayers()) {
            MetaData m;
            if (layer.isEnabled() && (m = layer.getMetaData()) instanceof HelioviewerMetaData) {
                HelioviewerMetaData hm = (HelioviewerMetaData) m;
                if (hm.getObservatory().contains("SDO") && hm.getInstrument().contains("AIA"))
                    str.append(',').append(hm.getMeasurement());
            }
        }

        ImageLayer activeLayer = Layers.getActiveImageLayer();
        if (activeLayer != null) {
            APIRequest req;
            if ((req = activeLayer.getAPIRequest()) != null) {
                str.append("&cadence=").append(req.cadence()).append("&cadenceUnits=s");
            }
            ImageData id;
            if ((id = activeLayer.getImageData()) != null) {
                Region region = Region.scale(id.getRegion(), 1 / id.getMetaData().getUnitPerArcsec());
                str.append(String.format("&xCen=%.1f", region.llx + region.width / 2.));
                str.append(String.format("&yCen=%.1f", -(region.lly + region.height / 2.)));
                str.append(String.format("&width=%.1f", region.width));
                str.append(String.format("&height=%.1f", region.height));
            }
        }

        long start = Movie.getStartTime();
        str.append("&startDate=").append(TimeUtils.formatDate(start));
        str.append("&startTime=").append(TimeUtils.formatTime(start));
        long end = Movie.getEndTime();
        str.append("&stopDate=").append(TimeUtils.formatDate(end));
        str.append("&stopTime=").append(TimeUtils.formatTime(end));
        return str.toString();
    }

    public static void getSAMPMessage(Message msg) {
        ImageData id;
        ImageLayer activeLayer = Layers.getActiveImageLayer();
        if (activeLayer == null || activeLayer.getAPIRequest() == null || (id = activeLayer.getImageData()) == null)
            return;

        msg.addParam("timestamp", Movie.getTime().toString());
        msg.addParam("start", TimeUtils.format(Movie.getStartTime()));
        msg.addParam("end", TimeUtils.format(Movie.getEndTime()));
        msg.addParam("cadence", SampUtils.encodeLong(activeLayer.getAPIRequest().cadence() * 1000L));
        msg.addParam("cutout.set", SampUtils.encodeBoolean(true));

        Region region = Region.scale(id.getRegion(), 1 / id.getMetaData().getUnitPerArcsec());
        msg.addParam("cutout.x0", SampUtils.encodeFloat(region.llx + region.width / 2.));
        msg.addParam("cutout.y0", SampUtils.encodeFloat(-(region.lly + region.height / 2.)));
        msg.addParam("cutout.w", SampUtils.encodeFloat(region.width));
        msg.addParam("cutout.h", SampUtils.encodeFloat(region.height));

        ArrayList<HashMap<String, String>> layersData = new ArrayList<>();
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (layer.isEnabled()) {
                if ((id = layer.getImageData()) == null)
                    continue;

                if (id.getMetaData() instanceof HelioviewerMetaData hm) {
                    HashMap<String, String> layerMsg = new HashMap<>();
                    layerMsg.put("observatory", hm.getObservatory());
                    layerMsg.put("instrument", hm.getInstrument());
                    layerMsg.put("detector", hm.getDetector());
                    layerMsg.put("measurement", hm.getMeasurement());
                    layerMsg.put("timestamp", hm.getViewpoint().time.toString());
                    layersData.add(layerMsg);
                }
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

    private static final Timer refreshTimer;
    private static final int timerDelay = 15 * (int) TimeUtils.MINUTE_IN_MILLIS;

    static {
        refreshTimer = new Timer(timerDelay, e -> refreshLayersSpan());
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
            if (req != null) {
                layer.load(new APIRequest(req.server(), req.sourceId(), now - (req.endTime() - req.startTime()), now, req.cadence()));
            }
        }
    }

}
