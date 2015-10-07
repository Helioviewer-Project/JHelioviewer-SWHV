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

    private void retrieveOcculterLinearCutOff(MetaDataContainer m) {
        if (detector.equals("C2")) {
            cutOffValue = (float) this.getPhysicalUpperLeft().y;
            double maskRotation = 2 * Math.toRadians(m.tryGetDouble("CROTA")) / MathUtils.radeg;
            cutOffDirection = new GL3DVec3d(Math.sin(maskRotation), Math.cos(maskRotation), 0);
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
            innerRadius *= 1.05;
        // outerRadius *= 0.9625;
        if (instrument.equals("MDI") || instrument.equals("HMI"))
            outerRadius = 1;
    }

    private void identifyObservation(MetaDataContainer m) {
        instrument = m.get("INSTRUME");
        if (instrument == null)
            return;
        instrument = instrument.split("_")[0];

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
            fullName = "HMI " + measurement.substring(0, 1) + measurement.substring(1, 3).toLowerCase();
        } else if (detector.equals("C2") || detector.equals("C3")) {
            String measurement1 = m.get("FILTER");
            String measurement2 = m.get("POLAR");
            measurement = measurement1 + " " + measurement2;
            fullName = "LASCO " + detector;
        } else if (instrument.equals("MDI")) {
            measurement = m.get("DPC_OBSR");
            fullName = "MDI " + measurement.substring(3, 6);
        } else if (detector.equals("COR1") || detector.equals("COR2")) {
            observatory = m.get("OBSRVTRY");
            fullName = instrument + " " + detector;
        } else if (detector.equals("EUVI")) {
            observatory = m.get("OBSRVTRY");
            if (measurement == null) {
                measurement = "" + m.tryGetDouble("WAVELNTH");
            }
            fullName = instrument + " " + detector + " " + measurement;
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

        double newSolarPixelRadius = -1.0;

        if (instrument.contains("HMI") || instrument.contains("AIA") || instrument.contains("SWAP") || instrument.contains("VSM") || instrument.contains("NRH") || instrument.contains("GONG") || instrument.contains("H-alpha")) {
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
            newSolarPixelRadius = radiusSunInArcsec / arcsecPerPixelX;
        } else if (instrument.equals("EIT")) {
            newSolarPixelRadius = m.tryGetDouble("SOLAR_R");
            if (newSolarPixelRadius == 0) {
                if (pixelWidth == 1024) {
                    newSolarPixelRadius = 360;
                } else if (pixelWidth == 512) {
                    newSolarPixelRadius = 180;
                }
            }
        } else if (detector.equals("C2") || detector.equals("C3")) {
            newSolarPixelRadius = m.tryGetDouble("RSUN");

            if (newSolarPixelRadius == 0) {
                if (detector.equals("C2")) {
                    newSolarPixelRadius = 80.814221;
                } else if (detector.equals("C3")) {
                    newSolarPixelRadius = 17.173021;
                }

            }
        } else if (instrument.equals("MDI")) {
            newSolarPixelRadius = m.tryGetDouble("R_SUN");
        } else if (detector.equals("COR1") || detector.equals("COR2") || detector.equals("EUVI")) {
            double solarRadiusArcSec = m.tryGetDouble("RSUN");
            double arcSecPerPixel = m.tryGetDouble("CDELT1");
            double arcSecPerPixel2 = m.tryGetDouble("CDELT2");
            if (arcSecPerPixel != arcSecPerPixel2) {
                Log.warn("HelioviewerMetaData.retrievePixelParameters(): Inconsistent STEREO metadata: resolution not the same in x and y direction (1: " + arcSecPerPixel + ", 2: " + arcSecPerPixel2 + ")");
            }
            double solarRadiusPixel = solarRadiusArcSec / arcSecPerPixel;
            newSolarPixelRadius = solarRadiusPixel;
        // pixel based
        } else if (instrument.equals("CALLISTO")) {
            setPhysicalLowerLeftCorner(new GL3DVec2d(0, 0));
            setPhysicalSize(new GL3DVec2d(pixelWidth, pixelHeight));
            return;
        }

        double sunX = m.tryGetDouble("CRPIX1") - 1;
        double sunY = m.tryGetDouble("CRPIX2") - 1;
        sunPixelPosition = new GL3DVec2d(sunX, pixelHeight - 1 - sunY);

        unitPerPixel = Sun.Radius / newSolarPixelRadius;
        setPhysicalLowerLeftCorner(new GL3DVec2d(-unitPerPixel * sunX, -unitPerPixel * sunY));
        setPhysicalSize(new GL3DVec2d(pixelWidth * unitPerPixel, pixelHeight * unitPerPixel));
    }

    public Region roiToRegion(SubImage roi, double zoompercent) {
        return new Region((roi.x / zoompercent - sunPixelPosition.x) * unitPerPixel,
                          (roi.y / zoompercent - sunPixelPosition.y) * unitPerPixel,
                          roi.width * unitPerPixel / zoompercent,
                          roi.height * unitPerPixel / zoompercent);
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
