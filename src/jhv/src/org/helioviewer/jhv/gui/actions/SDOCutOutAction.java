package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;

@SuppressWarnings("serial")
public class SDOCutOutAction extends AbstractAction {

    private static final String baseURL = "http://www.lmsal.com/get_aia_data/?";
    private static final double AIA_CDELT = 0.6;

    public SDOCutOutAction(boolean small, boolean useIcon) {
        super("SDO Cut-out", useIcon ? IconBank.getIcon(JHVIcon.SDO_CUTOUT) : null);
        putValue(SHORT_DESCRIPTION, "SDO cut-out service");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        StringBuilder url = new StringBuilder(baseURL);

        String start = Layers.getStartDate().toString();
        String startDate = start.substring(0, 10);
        String startTime = start.substring(11, 16);
        url.append("startDate=").append(startDate);
        url.append("&startTime=").append(startTime);

        String end = Layers.getEndDate().toString();
        String endDate = end.substring(0, 10);
        String endTime = end.substring(11, 16);
        url.append("&stopDate=").append(endDate);
        url.append("&stopTime=").append(endTime);

        url.append("&wavelengths=").append(Layers.getSDOCutoutString());
        url.append("&cadence=").append(ObservationDialog.getInstance().getObservationPanel().getCadence()).append("&cadenceUnits=s");

        View v = Layers.getActiveView();
        if (v != null) {
            ImageData imd = v.getImageLayer().getImageData();
            if (imd != null) {
                Region region = imd.getRegion();
                MetaData md = imd.getMetaData();
                Region fullregion = md.getPhysicalRegion();
                double arcsec_in_image = AIA_CDELT * 4096;
                double centr_x = region.llx + region.width / 2.;
                double centr_y = region.lly + region.height / 2.;
                double arc_w = arcsec_in_image / fullregion.width;
                double arc_h = arcsec_in_image / fullregion.height;

                url.append(String.format("&width=%.1f", region.width * arc_w));
                url.append(String.format("&height=%.1f", region.height * arc_h));
                url.append(String.format("&xCen=%.1f", centr_x * arc_w - AIA_CDELT / 2.));
                url.append(String.format("&yCen=%.1f", -centr_y * arc_h + AIA_CDELT / 2.));
            }
        }

        JHVGlobals.openURL(url.toString());
    }

}
