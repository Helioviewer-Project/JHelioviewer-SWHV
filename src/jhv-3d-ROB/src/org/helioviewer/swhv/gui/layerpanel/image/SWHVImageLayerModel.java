package org.helioviewer.swhv.gui.layerpanel.image;

import java.util.ArrayList;
import java.util.Date;

import org.helioviewer.swhv.gui.layerpanel.SWHVAbstractLayerModel;
import org.helioviewer.swhv.gui.layerpanel.SWHVRegistrableLayerModel;

public class SWHVImageLayerModel extends SWHVAbstractLayerModel implements SWHVRegistrableLayerModel {
    private final ArrayList<SWHVImageLayerModelListener> listeners = new ArrayList<SWHVImageLayerModelListener>();
    private SWHVImageLayerController controller;
    private double opacity;
    private double sharpen;
    private double contrast;

    @Override
    public void fireLevelChanged() {
    }

    @Override
    public void fireFoldedChanged() {
    }

    @Override
    public SWHVImageLayerOptionPanel getOptionPanel() {
        return new SWHVImageLayerOptionPanel(this);
    }

    @Override
    public SWHVImageLayerController getController() {
        return this.controller;
    }

    public void setController(SWHVImageLayerController controller) {
        this.controller = controller;
    }

    @Override
    public void fireRemoved() {

    }

    /**
     * Sets the date label of the current date
     */
    public void setCurrentDateLabel() {

    }

    /**
     * Sets the model Opacity
     * 
     * @param opacity
     */
    public void setOpacity(double opacity) {
        this.opacity = opacity;
        for (SWHVImageLayerModelListener l : listeners) {
            l.opacityChanged(opacity);
        }
    }

    /**
     * Sets sharpen value
     * 
     * @param sharpen
     */
    public void setSharpen(double sharpen) {
        this.sharpen = sharpen;
        for (SWHVImageLayerModelListener l : listeners) {
            l.sharpenChanged(sharpen);
        }
    }

    /**
     * Sets contrast value
     * 
     * @param sharpen
     */
    public void setContrast(double contrast) {
        this.contrast = contrast;
        for (SWHVImageLayerModelListener l : listeners) {
            l.contrastChanged(contrast);
        }
    }

    @Override
    public void setBeginDate(Date beginDate) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setEndDate(Date endDate) {
        // TODO Auto-generated method stub

    }
}
