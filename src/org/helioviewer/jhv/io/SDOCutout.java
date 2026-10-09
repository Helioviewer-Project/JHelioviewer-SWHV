package org.helioviewer.jhv.io;

import org.helioviewer.jhv.layers.ImageData;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.metadata.FitsMetaData;
import org.helioviewer.jhv.metadata.Region;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.time.TimeUtils;

// The LMSAL cut-out request for the enabled AIA layers, the active layer's region and the player's span.
public final class SDOCutout {

    public static String url() {
        StringBuilder str = new StringBuilder("https://www.lmsal.com/get_aia_data/?&wavelengths=");
        for (ImageLayer layer : Layers.getImageLayers()) {
            if (!layer.isEnabled())
                continue;
            if (layer.getMetaData() instanceof FitsMetaData fm && fm.getObservatory().contains("SDO") && fm.getInstrument().contains("AIA"))
                str.append(',').append(fm.getMeasurement());
        }

        ImageLayer activeLayer = Layers.getActiveImageLayer();
        APIRequest req = activeLayer.getAPIRequest();
        if (req != null) {
            str.append("&cadence=").append(req.cadence()).append("&cadenceUnits=s");
        }
        ImageData id = activeLayer.getImageData();
        if (id != null) {
            Region region = Region.scale(id.region(), 1 / id.metaData().getUnitPerArcsec());
            str.append(String.format("&xCen=%.1f", region.llx + region.width / 2.));
            str.append(String.format("&yCen=%.1f", -(region.lly + region.height / 2.)));
            str.append(String.format("&width=%.1f", region.width));
            str.append(String.format("&height=%.1f", region.height));
        }

        long start = Player.getStartTime();
        str.append("&startDate=").append(TimeUtils.formatDate(start));
        str.append("&startTime=").append(TimeUtils.formatTime(start));
        long end = Player.getEndTime();
        str.append("&stopDate=").append(TimeUtils.formatDate(end));
        str.append("&stopTime=").append(TimeUtils.formatTime(end));
        return str.toString();
    }

    private SDOCutout() {}
}
