package org.helioviewer.viewmodel.metadata;

import java.util.Date;

import org.helioviewer.base.Region;
import org.helioviewer.base.astronomy.Position;
import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.base.math.MathUtils;
import org.helioviewer.base.time.ImmutableDateTime;
import org.helioviewer.viewmodel.view.jp2view.image.SubImage;

/**
 * Implementation of MetaData representing solar images.
 *
 * <p>
 * This class is supposed to be for solar images. Currently, it supports the
 * observatory SOHO with its instruments EIT, LASCO and MDI, as well as some
 * instruments on board of the observatory STEREO.
 *
 * @author Ludwig Schmidt
 * @author Andre Dau
 *
 */
public class HelioviewerMetaData extends AbstractMetaData implements ObserverMetaData {

    private String instrument = "";
    private String detector = "";
    private String measurement = " ";
    private String observatory = " ";
    private String fullName = "";

    private GL3DVec2d sunPixelPosition;

    /**
     * Default constructor.
     *
     * Tries to read all informations required.
     *
     * @param m
     *            Meta data container serving as a base for the construction
     */
    public HelioviewerMetaData(MetaDataContainer m) {
        identifyObservation(m);
        retrieveDateTime(m);
        retrievePosition(m);
        retrievePixelParameters(m);
        retrieveOcculterRadii(m);
        retrieveOcculterLinearCutOff(m);
    }

    // magic
    private void retrieveOcculterLinearCutOff(MetaDataContainer m) {
        if (detector.equals("C2")) {
            double maskRotation = -Math.toRadians(m.tryGetDouble("CROTA"));
            cutOffValue = (float) (-this.getPhysicalUpperLeft().x);
            cutOffDirection = new GL3DVec3d(Math.sin(maskRotation) / 0.9625, Math.cos(maskRotation) / 0.9625, 0);
        }
    }

    private void retrieveOcculterRadii(MetaDataContainer m) {
        innerRadius = m.tryGetDouble("HV_ROCC_INNER") * Sun.Radius;
        outerRadius = m.tryGetDouble("HV_ROCC_OUTER") * Sun.Radius;

        if (innerRadius == 0) {
            if (detector.equalsIgnoreCase("C2")) {
                innerRadius = 2.3 * Sun.Radius;
                outerRadius = 8.0 * Sun.Radius;
            } else if (detector.equalsIgnoreCase("C3")) {
                innerRadius = 4.4 * Sun.Radius;
                outerRadius = 31.5 * Sun.Radius;
            } else if (observatory.equalsIgnoreCase("STEREO_A") && detector.equalsIgnoreCase("COR1")) {
                innerRadius = 1.36 * Sun.Radius;
                outerRadius = 4.5 * Sun.Radius;
            } else if (observatory.equalsIgnoreCase("STEREO_A") && detector.equalsIgnoreCase("COR2")) {
                innerRadius = 2.4 * Sun.Radius;
                outerRadius = 15.6 * Sun.Radius;
            } else if (observatory.equalsIgnoreCase("STEREO_B") && detector.equalsIgnoreCase("COR1")) {
                innerRadius = 1.5 * Sun.Radius;
                outerRadius = 4.9 * Sun.Radius;
            } else if (observatory.equalsIgnoreCase("STEREO_B") && detector.equalsIgnoreCase("COR2")) {
                innerRadius = 3.25 * Sun.Radius;
                outerRadius = 17 * Sun.Radius;
            }
        }
        if (outerRadius == 0) {
            outerRadius = Double.MAX_VALUE;
        }

        // magic
        if (detector.equalsIgnoreCase("C3"))
            innerRadius *= 1.07;
        if (instrument.equals("MDI") || instrument.equals("HMI"))
            outerRadius = 1;
    }

    private void identifyObservation(MetaDataContainer m) {
        instrument = m.get("INSTRUME");
        if (instrument == null)
            return;
        instrument = instrument.split("_", 2)[0];

        detector = m.get("DETECTOR");
        if (detector == null) {
            detector = " ";
        }

        measurement = m.get("WAVELNTH");
        if (measurement == null) {
            measurement = "" + m.tryGetInt("WAVELNTH");
        }
        if (measurement == null) {
            measurement = "" + m.tryGetDouble("WAVELNTH");
        }
        observatory = m.get("TELESCOP");

        if (instrument.contains("VSM")) {
            fullName = "NSO-SOLIS " + measurement;
        } else if (instrument.contains("HMI")) {
            measurement = m.get("CONTENT");
            String str[] = measurement.split(" ", 2);
            fullName = "HMI " + str[0].toLowerCase();
        } else if (detector.equals("C2") || detector.equals("C3")) {
            String measurement1 = m.get("FILTER");
            String measurement2 = m.get("POLAR");
            measurement = measurement1 + " " + measurement2;
            fullName = "LASCO " + detector;
        } else if (instrument.equals("MDI")) {
            measurement = m.get("DPC_OBSR");
            fullName = "MDI " + measurement.substring(measurement.indexOf('_') + 1).toLowerCase();
        } else if (detector.equals("COR1") || detector.equals("COR2")) {
            observatory = m.get("OBSRVTRY");
            fullName = observatory + " " + detector;
        } else if (detector.equals("EUVI")) {
            observatory = m.get("OBSRVTRY");
            if (measurement == null) {
                measurement = "" + m.tryGetDouble("WAVELNTH");
            }
            fullName = observatory + " " + detector + " " + measurement;
        } else {
            fullName = instrument + " " + measurement;
        }
    }

    private void retrieveDateTime(MetaDataContainer m) {
        String observedDate;

        if (instrument.equals("MDI")) {
            observedDate = m.get("DATE_OBS");
        } else {
            observedDate = m.get("DATE-OBS");
            if (observedDate == null) {
                observedDate = m.get("DATE_OBS");
                if (observedDate != null && instrument.equals("LASCO")) {
                    observedDate += "T" + m.get("TIME_OBS");
                }
            }
        }
        // otherwise default epoch

        if (observedDate != null)
            dateObs = ImmutableDateTime.parseDateTime(observedDate);
    }

    private void retrievePosition(MetaDataContainer m) {
        Date obsDate = dateObs.getDate();
        Position.Latitudinal p = Sun.getEarth(obsDate);

        if ((distanceObs = m.tryGetDouble("DSUN_OBS") / Sun.RadiusMeter) == 0) {
            distanceObs = p.rad;
            if (observatory.equals("SOHO")) {
                distanceObs *= Sun.L1Factor;
            }
        }

        double stonyhurstLatitude, theta;
        if ((stonyhurstLatitude = m.tryGetDouble("HGLT_OBS")) == 0) {
            if ((stonyhurstLatitude = m.tryGetDouble("CRLT_OBS")) == 0) {
                if ((stonyhurstLatitude = m.tryGetDouble("REF_B0")) == 0) {
                    // presumably not found
                    stonyhurstLatitude = p.lat * MathUtils.radeg;
                }
            }
        }
        theta = stonyhurstLatitude / MathUtils.radeg;

        double stonyhurstLongitude, phi;
        if ((stonyhurstLongitude = m.tryGetDouble("HGLN_OBS")) == 0) {
            stonyhurstLongitude = m.tryGetDouble("REF_L0");
            if (stonyhurstLongitude != 0) {
                stonyhurstLongitude += p.lon * MathUtils.radeg;
            }
        }
        phi = p.lon - stonyhurstLongitude / MathUtils.radeg;

        rotationObs = new GL3DQuatd(theta, phi);
    }

    private void retrievePixelParameters(MetaDataContainer m) {
        pixelWidth = m.tryGetInt("NAXIS1");
        pixelHeight = m.tryGetInt("NAXIS2");

        boolean isCallisto = instrument.equals("CALLISTO");
        if (!isCallisto) {
            double arcsecPerPixelX = m.tryGetDouble("CDELT1");
            double arcsecPerPixelY = m.tryGetDouble("CDELT2");
            if (Double.isNaN(arcsecPerPixelX) || Double.isNaN(arcsecPerPixelY)) {
                Log.warn(">> HelioviewerMetaData.retrievePixelParameters() > CDELT1 or CDELT2 are NaN. Use 0.6 as default value.");
                arcsecPerPixelX = arcsecPerPixelY = 0.6;
            }
            if (Math.abs(arcsecPerPixelX - arcsecPerPixelY) > arcsecPerPixelX * 0.0001) {
                Log.warn(">> HelioviewerMetaData.retrievePixelParameters() > CDELT1 and CDELT2 have different values. CDELT1 is used.");
            }
            double radiusSunInArcsec = Math.atan2(Sun.Radius, distanceObs) * MathUtils.radeg * 3600;
            double solarPixelRadius = radiusSunInArcsec / arcsecPerPixelX;
            unitPerPixel = Sun.Radius / solarPixelRadius;

            double sunX = m.tryGetDouble("CRPIX1") - 1;
            double sunY = m.tryGetDouble("CRPIX2") - 1;
            sunPixelPosition = new GL3DVec2d(sunX, pixelHeight - 1 - sunY);

            setPhysicalLowerLeftCorner(new GL3DVec2d(-unitPerPixel * sunX, -unitPerPixel * sunY));
            setPhysicalSize(new GL3DVec2d(pixelWidth * unitPerPixel, pixelHeight * unitPerPixel));
        } else { // pixel based
            setPhysicalLowerLeftCorner(new GL3DVec2d(0, 0));
            setPhysicalSize(new GL3DVec2d(pixelWidth, pixelHeight));
        }
    }

    public Region roiToRegion(SubImage roi, double zoompercent) {
        return new Region((roi.x / zoompercent - sunPixelPosition.x) * unitPerPixel, (roi.y / zoompercent - sunPixelPosition.y) * unitPerPixel,
                          roi.width * unitPerPixel / zoompercent, roi.height * unitPerPixel / zoompercent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDetector() {
        return detector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInstrument() {
        return instrument;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMeasurement() {
        return measurement;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getObservatory() {
        return observatory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFullName() {
        return fullName;
    }

}
