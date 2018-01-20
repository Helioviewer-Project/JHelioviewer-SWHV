package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.view.View;

///

import java.util.ArrayList;
import java.util.HashMap;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.SampUtils;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.io.APIRequest;

public class ImageLayers {

    public static void setRender(Camera camera, double factor) {
        int i;
        Viewport[] vp = Displayer.getViewports();
        for (ImageLayer layer : Layers.getImageLayers()) {
            if ((i = layer.isVisibleIdx()) != -1 && vp[i] != null)
                layer.getView().render(camera, vp[i], factor);
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
        Displayer.reshapeAll();
        Displayer.render(1);
    }

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

    public static boolean getSyncedImageLayers(long milli) {
        for (ImageLayer layer : Layers.getImageLayers()) {
            ImageData id;
            if (layer.isEnabled() && (id = layer.getImageData()) != null && milli != id.getViewpoint().time.milli)
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

    public static String getSDOCutoutString() {
        StringBuilder str = new StringBuilder();
        MetaData m;
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (layer.isEnabled() && (m = layer.getMetaData()) instanceof HelioviewerMetaData) {
                HelioviewerMetaData hm = (HelioviewerMetaData) m;
                if (hm.getObservatory().contains("SDO") && hm.getInstrument().contains("AIA"))
                    str.append(',').append(hm.getMeasurement());
            }
        }
        return str.toString();
    }

    public static void syncLayersSpan(long startTime, long endTime, int cadence) {
        for (ImageLayer layer : Layers.getImageLayers()) {
            APIRequest areq = layer.getAPIRequest();
            if (areq != null) {
                layer.load(new APIRequest(areq.server, areq.sourceId, startTime, endTime, cadence));
            }
        }
    }

    public static void syncLayersSpan() {
        ImageLayer activeLayer = Layers.getActiveImageLayer();
        if (activeLayer == null)
            return;

        APIRequest areq = activeLayer.getAPIRequest();
        long startTime, endTime;
        int cadence;
        if (areq != null) {
            startTime = areq.startTime;
            endTime = areq.endTime;
            cadence = areq.cadence;
        } else {
            View view = activeLayer.getView();
            startTime = view.getFirstTime().milli;
            endTime = view.getLastTime().milli;
            cadence = ObservationDialog.getInstance().getCadence();
        }

        for (ImageLayer layer : Layers.getImageLayers()) {
            APIRequest vreq = layer.getAPIRequest();
            if (vreq != null && layer != activeLayer) {
                layer.load(new APIRequest(vreq.server, vreq.sourceId, startTime, endTime, cadence));
            }
        }
    }

    public static void getSAMPMessage(Message msg) {
        ImageData id;
        ImageLayer activeLayer = Layers.getActiveImageLayer();
        if (activeLayer == null || activeLayer.getAPIRequest() == null || (id = activeLayer.getImageData()) == null)
            return;

        View view = activeLayer.getView();
        MetaData m = id.getMetaData();
        msg.addParam("timestamp", m.getViewpoint().time.toString());
        msg.addParam("start", view.getFirstTime().toString());
        msg.addParam("end", view.getFirstTime().toString());
        msg.addParam("cadence", SampUtils.encodeLong(activeLayer.getAPIRequest().cadence * 1000L));
        msg.addParam("cutout.set", SampUtils.encodeBoolean(true));

        Region region = Region.scale(id.getRegion(), 1 / m.getUnitPerArcsec());
        msg.addParam("cutout.x0", SampUtils.encodeFloat(region.llx + region.width / 2.));
        msg.addParam("cutout.y0", SampUtils.encodeFloat(-(region.lly + region.height / 2.)));
        msg.addParam("cutout.w", SampUtils.encodeFloat(region.width));
        msg.addParam("cutout.h", SampUtils.encodeFloat(region.height));

        ArrayList<HashMap<String, String>> layersData = new ArrayList<>();
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (layer.isEnabled()) {
                if ((id = layer.getImageData()) == null)
                    continue;

                m = id.getMetaData();
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
