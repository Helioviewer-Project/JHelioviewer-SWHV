package org.helioviewer.viewmodel.metadata;

import org.helioviewer.base.physics.Constants;

/**
 * Implementation of MetaData, extends HelioviewerMetaData.
 * 
 * <p>
 * This special implementation also provides informations about the occulting
 * disc. The {@link MetaDataConstructor} should only produce this
 * implementation if there is there is actual information about the occulting
 * disc present, so it is possible to test this via instanceof.
 * 
 * @author Markus Langenberg
 * 
 */
public class HelioviewerOcculterMetaData extends HelioviewerMetaData implements OcculterMetaData {

    private double innerRadius;
    private double outerRadius;

    /**
     * Default constructor.
     * 
     * Tries to read all informations required.
     * 
     * @param m
     *            Meta data container serving as a base for the construction
     */
    public HelioviewerOcculterMetaData(MetaDataContainer m) {
        super(m);

        innerRadius = m.tryGetDouble("HV_ROCC_INNER") * Constants.SunRadius;
        outerRadius = m.tryGetDouble("HV_ROCC_OUTER") * Constants.SunRadius;

        if (innerRadius == 0.0 && getDetector() != null) {
            if (getDetector().equals("C2")) {
                innerRadius = 2.3 * Constants.SunRadius;
                outerRadius = 8.0 * Constants.SunRadius;
            } else if (getDetector().equals("C3")) {
                innerRadius = 4.4 * Constants.SunRadius;
                outerRadius = 31.5 * Constants.SunRadius;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public double getInnerPhysicalOcculterRadius() {
        return innerRadius;
    }

    /**
     * {@inheritDoc}
     */
    public double getOuterPhysicalOcculterRadius() {
        return outerRadius;
    }

}
