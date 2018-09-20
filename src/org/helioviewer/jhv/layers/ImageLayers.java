package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.time.TimeUtils;

///

import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.Nullable;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.SampUtils;

public class ImageLayers {

    public static void decode(int serialNo, double factor) {
        Viewport[] vp = Display.getViewports();
        double cameraWidth = Display.getCamera().getWidth();
        Quat q = Display.getCamera().getViewpoint().toQuat();
        for (ImageLayer layer : Layers.getImageLayers()) {
            int i;
            if ((i = layer.isVisibleIdx()) != -1 && vp[i] != null) {
                double pixFactor = vp[i].height / (2 * cameraWidth);
                layer.getView().decode(serialNo, q, pixFactor, factor);
            }
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
        Display.render(1);
    }

    @Nullable
    public static ImageLayer getImageLayerInViewport(int idx) {
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (layer.isVisible(idx))
                return layer;
        }
        return null;
    }

    public static int getNumEnabledImageLayers() {
        int ct = 0;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (layer.isEnabled())
                ct++;
        }
        return ct;
    }

    public static boolean getSyncedImageLayers(int serialNo) {
        for (ImageLayer layer : Layers.getImageLayers()) {
            ImageData id;
            if (layer.isEnabled() && (id = layer.getImageData()) != null && serialNo != id.getSerial())
                return false;
        }
        return true;
    }

    public static double getLargestPhysicalHeight() {
        double size = 0;
        MetaData m;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (layer.isEnabled() && (m = layer.getMetaData()) != null) {
                double newSize = m.getPhysicalRegion().height;
                if (newSize > size)
                    size = newSize;
            }
        }
        return size;
    }

    public static double getLargestPhysicalSize() {
        double size = 0;
        MetaData m;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (layer.isEnabled() && (m = layer.getMetaData()) != null) {
                Region r = m.getPhysicalRegion();
                double newSize = Math.sqrt(r.height * r.height + r.width * r.width);
                if (newSize > size)
                    size = newSize;
            }
        }
        return size;
    }

    public static void syncLayersSpan(long startTime, long endTime, int cadence) {
        for (ImageLayer layer : Layers.getImageLayers()) {
            APIRequest req = layer.getAPIRequest();
            if (req != null) {
                layer.load(new APIRequest(req.server, req.sourceId, startTime, endTime, cadence));
            }
        }
    }

    public static void shiftLayersSpan(long delta) {
        for (ImageLayer layer : Layers.getImageLayers()) {
            APIRequest req = layer.getAPIRequest();
            if (req != null) {
                layer.load(new APIRequest(req.server, req.sourceId, req.startTime + delta, req.endTime + delta, req.cadence));
            }
        }
    }

    public static String getSDOCutoutString() {
        StringBuilder str = new StringBuilder("&wavelengths=");
        MetaData m;
        for (ImageLayer layer : Layers.getImageLayers()) {
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
                str.append("&cadence=").append(req.cadence).append("&cadenceUnits=s");
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
        msg.addParam("start", TimeUtils.formatDate(Movie.getStartTime()));
        msg.addParam("end", TimeUtils.formatDate(Movie.getEndTime()));
        msg.addParam("cadence", SampUtils.encodeLong(activeLayer.getAPIRequest().cadence * 1000L));
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

                MetaData m = id.getMetaData();
                if (m instanceof HelioviewerMetaData) {
                    HelioviewerMetaData hm = (HelioviewerMetaData) m;
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

}
