package org.helioviewer.viewmodel.metadata;

import org.helioviewer.base.physics.Constants;

/**
 * Implementation of MetaData, extends HelioviewerMetaData.
 * 
 * <p>
 * This special implementation also provides informations about the occulting
 * disc. The {@link MetaDataConstructor} should only produce this
 * implementation, if there is there is actual information about the occulting
 * disc present, so it is possible to test this via instanceof.
 * 
 * @author Markus Langenberg
 * 
 */
public class HelioviewerOcculterMetaData extends HelioviewerMetaData implements OcculterMetaData {

    private double innerRadius;
    private double outerRadius;
    private double flatDistance;
    private double maskRotation;

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

        if (getDetector().equals("C2")) {
            flatDistance = 6.2 * Constants.SunRadius;
        } else if (getDetector().equals("C3")) {
            flatDistance = 38 * Constants.SunRadius;
        }

        maskRotation = Math.toRadians(m.tryGetDouble("CROTA"));
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

    /**
     * {@inheritDoc}
     */
    public double getPhysicalFlatOcculterSize() {
        return flatDistance;
    }

    /**
     * {@inheritDoc}
     */
    public double getMaskRotation() {
        return maskRotation;
    }

    /**
     * {@inheritDoc}
     * 
     * In this case, also the mask rotation is checked.
     */
    public boolean checkForModifications() {
        boolean changed = super.checkForModifications();

        double currentMaskRotation = Math.toRadians(metaDataContainer.tryGetDouble("CROTA"));
        if (changed || Math.abs(maskRotation - currentMaskRotation) > Math.toRadians(1)) {
            maskRotation = currentMaskRotation;
            changed = true;
        }

        return changed;
    }
}
