package org.helioviewer.jhv.renderable.gui;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.MetaData;

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

}
