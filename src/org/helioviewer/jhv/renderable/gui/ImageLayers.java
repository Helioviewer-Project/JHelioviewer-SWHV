package org.helioviewer.jhv.renderable.gui;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.MetaData;

///

import java.util.ArrayList;
import java.util.HashMap;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.SampUtils;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.view.View;

public class ImageLayers {

    public static void arrangeMultiView(boolean multiview) {
        if (multiview) {
            int ct = 0;
            for (ImageLayer layer : RenderableContainer.getImageLayers()) {
                if (layer.isEnabled()) {
                    layer.setVisible(ct);
                    ct++;
                }
            }
        } else {
            for (ImageLayer layer : RenderableContainer.getImageLayers()) {
                if (layer.isEnabled())
                    layer.setVisible(0);
            }
        }
        Displayer.reshapeAll();
        Displayer.render(1);
    }

    public static ImageLayer getImageLayerInViewport(int idx) {
        for (ImageLayer layer : RenderableContainer.getImageLayers()) {
            if (layer.isVisible(idx))
                return layer;
        }
        return null;
    }

    public static int getNumEnabledImageLayers() {
        int ct = 0;
        for (ImageLayer layer : RenderableContainer.getImageLayers()) {
            if (layer.isEnabled())
                ct++;
        }
        return ct;
    }

    public static double getLargestPhysicalHeight() {
        double size = 0;
        MetaData m;
        for (ImageLayer layer : RenderableContainer.getImageLayers()) {
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
        for (ImageLayer layer : RenderableContainer.getImageLayers()) {
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
        for (ImageLayer layer : RenderableContainer.getImageLayers()) {
            if (layer.isEnabled() && (m = layer.getMetaData()) instanceof HelioviewerMetaData) {
                HelioviewerMetaData hm = (HelioviewerMetaData) m;
                if (hm.getObservatory().contains("SDO") && hm.getInstrument().contains("AIA"))
                    str.append(',').append(hm.getMeasurement());
            }
        }
        return str.toString();
    }

    public static void syncLayersSpan() {
        View activeView = Layers.getActiveView();
        if (activeView == null)
            return;

        APIRequest areq = activeView.getImageLayer().getAPIRequest();
        long startTime, endTime;
        int cadence;
        if (areq != null) {
            startTime = areq.startTime;
            endTime = areq.endTime;
            cadence = areq.cadence;
        } else {
            startTime = activeView.getFirstTime().milli;
            endTime = activeView.getLastTime().milli;
            cadence = ObservationDialog.getInstance().getObservationPanel().getCadence();
        }

        for (ImageLayer layer : RenderableContainer.getImageLayers()) {
            APIRequest vreq = layer.getAPIRequest();
            if (!layer.isActiveImageLayer() && vreq != null) {
                layer.load(new APIRequest(vreq.server, vreq.sourceId, startTime, endTime, cadence));
            }
        }
    }

    public static void getSAMPMessage(Message msg) {
        View activeView = Layers.getActiveView();
        if (activeView == null)
            return;

        ImageLayer activeLayer = activeView.getImageLayer();
        if (activeLayer.getAPIRequest() == null || activeLayer.getImageData() == null)
            return;

        ImageData id = activeLayer.getImageData();
        MetaData m = id.getMetaData();

        msg.addParam("timestamp", m.getViewpoint().time.toString());
        msg.addParam("start", activeView.getFirstTime().toString());
        msg.addParam("end", activeView.getFirstTime().toString());
        msg.addParam("cadence", SampUtils.encodeLong(activeLayer.getAPIRequest().cadence * 1000L));
        msg.addParam("cutout.set", SampUtils.encodeBoolean(true));

        Region region = Region.scale(id.getRegion(), 1 / m.getUnitPerArcsec());
        msg.addParam("cutout.x0", SampUtils.encodeFloat(region.llx + region.width / 2.));
        msg.addParam("cutout.y0", SampUtils.encodeFloat(-(region.lly + region.height / 2.)));
        msg.addParam("cutout.w", SampUtils.encodeFloat(region.width));
        msg.addParam("cutout.h", SampUtils.encodeFloat(region.height));

        ArrayList<HashMap<String, String>> layersData = new ArrayList<>();
        for (ImageLayer layer : RenderableContainer.getImageLayers()) {
            if (layer.isEnabled()) {
                id = layer.getImageData();
                if (id == null)
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
