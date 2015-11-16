package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.time.JHVDate;

public interface MetaData {

    /* Returns the physical region of the corresponding image */
    public Region getPhysicalRegion();

    public int getPixelWidth();

    public int getPixelHeight();

    public JHVDate getDateObs();

    public Quat getRotationObs();

    public double getDistanceObs();

    public double getInnerCutOffRadius();

    public double getOuterCutOffRadius();

    float getCutOffValue();

    Vec3 getCutOffDirection();

}
