package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.view.View;

@SuppressWarnings("serial")
public class SDOCutOutAction extends AbstractAction {

    private static final String baseURL = "http://www.lmsal.com/get_aia_data/?";

    public SDOCutOutAction() {
        super("SDO Cut-out");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        StringBuilder url = new StringBuilder(baseURL);

        long start = Layers.getStartTime();
        url.append("startDate=").append(TimeUtils.formatDate(start));
        url.append("&startTime=").append(TimeUtils.formatTime(start));

        long end = Layers.getEndTime();
        url.append("&stopDate=").append(TimeUtils.formatDate(end));
        url.append("&stopTime=").append(TimeUtils.formatTime(end));

        url.append("&wavelengths=").append(ImageLayers.getSDOCutoutString());
        url.append("&cadence=").append(ObservationDialog.getInstance().getObservationPanel().getCadence()).append("&cadenceUnits=s");

        View v = Layers.getActiveView();
        if (v != null) {
            ImageData imd = v.getImageLayer().getImageData();
            if (imd != null) {
                Region region = Region.scale(imd.getRegion(), 1 / imd.getMetaData().getUnitPerArcsec());
                url.append(String.format("&width=%.1f", region.width));
                url.append(String.format("&height=%.1f", region.height));
                url.append(String.format("&xCen=%.1f", region.llx + region.width / 2.));
                url.append(String.format("&yCen=%.1f", -(region.lly + region.height / 2.)));
            }
        }

        JHVGlobals.openURL(url.toString());
    }

}
