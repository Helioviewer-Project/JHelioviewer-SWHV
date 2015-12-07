package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.math.Vec3;

public interface MetaData {

    public Region getPhysicalRegion();

    public int getPixelWidth();

    public int getPixelHeight();

    public Position.Q getViewpoint();

    public double getInnerCutOffRadius();

    public double getOuterCutOffRadius();

    public double getCutOffValue();

    public Vec3 getCutOffDirection();

}
