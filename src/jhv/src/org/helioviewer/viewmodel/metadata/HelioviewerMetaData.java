package org.helioviewer.viewmodel.metadata;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.base.math.MathUtils;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;
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
public class HelioviewerMetaData extends AbstractMetaData implements ObserverMetaData, ImageSizeMetaData {

    private double dobs;
    private final boolean carringtonAvailable = false;

    private double refb0;
    private double refl0;
    private double innerRadius = 0.;
    private double outerRadius = 40.;

    private String instrument = "";
    private String detector = "";
    private String measurement = " ";
    private String observatory = " ";
    private String fullName = "";

    private Vector2dInt pixelImageSize = new Vector2dInt();

    private double meterPerPixel;
    private GL3DQuatd localRotation;
    private GL3DVec2d sunPixelPositionImage = new GL3DVec2d();

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
    }

    private void retrieveOcculterRadii(MetaDataContainer m) {
        innerRadius = m.tryGetDouble("HV_ROCC_INNER") * Constants.SunRadius;
        outerRadius = m.tryGetDouble("HV_ROCC_OUTER") * Constants.SunRadius;

        if (innerRadius == 0.0 && getDetector() != null) {
            if (getDetector().equalsIgnoreCase("C2")) {
                innerRadius = 2.3 * Constants.SunRadius;
                outerRadius = 8.0 * Constants.SunRadius;
            } else if (getDetector().equalsIgnoreCase("C3")) {
                innerRadius = 4.4 * Constants.SunRadius;
                outerRadius = 31.5 * Constants.SunRadius;
            } else if (getObservatory().equalsIgnoreCase("STEREO_A") && getDetector().equalsIgnoreCase("COR1")) {
                innerRadius = 1.36 * Constants.SunRadius;
                outerRadius = 4.5 * Constants.SunRadius;
            } else if (getObservatory().equalsIgnoreCase("STEREO_A") && getDetector().equalsIgnoreCase("COR2")) {
                innerRadius = 2.4 * Constants.SunRadius;
                outerRadius = 15.6 * Constants.SunRadius;
            } else if (getObservatory().equalsIgnoreCase("STEREO_B") && getDetector().equalsIgnoreCase("COR1")) {
                innerRadius = 1.5 * Constants.SunRadius;
                outerRadius = 4.9 * Constants.SunRadius;
            } else if (getObservatory().equalsIgnoreCase("STEREO_B") && getDetector().equalsIgnoreCase("COR2")) {
                innerRadius = 3.25 * Constants.SunRadius;
                outerRadius = 17 * Constants.SunRadius;
            }
        }
        if (outerRadius == 0.)
            outerRadius = Double.MAX_VALUE;
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
        String observedDate = m.get("DATE-OBS");
        if (observedDate == null) {
            observedDate = m.get("DATE_OBS");
            if (observedDate != null && instrument.equals("LASCO")) {
                observedDate += "T" + m.get("TIME_OBS");
            }
        }
        dateTime = ImmutableDateTime.parseDateTime(observedDate);
    }

    private void retrievePosition(MetaDataContainer m) {
        this.dobs = m.tryGetDouble("DSUN_OBS");
        if (this.dobs == 0.) {
            this.dobs = Astronomy.getDistanceMeters(this.getDateTime().getTime());
        }

        this.refb0 = m.tryGetDouble("REF_B0");
        this.refl0 = m.tryGetDouble("REF_L0");

        double stonyhurstLatitude = m.tryGetDouble("HGLT_OBS");
        if (stonyhurstLatitude == 0) {
            stonyhurstLatitude = m.tryGetDouble("CRLT_OBS");
            if (stonyhurstLatitude == 0) {
                stonyhurstLatitude = this.refb0;
            }
        }
        double stonyhurstLongitude = m.tryGetDouble("HGLN_OBS");
        if (this.refl0 != 0.) {
            stonyhurstLongitude = this.refl0 - Astronomy.getL0Degree(this.getDateTime().getTime());
        }

        if (this.getInstrument().contains("GONG") || this.getObservatory().contains("USET") || this.getObservatory().contains("SOLIS")) {
            stonyhurstLongitude = 0.0;
        }

        double theta = -Astronomy.getB0InRadians(this.getDateTime().getTime());
        double phi = Astronomy.getL0Radians(this.getDateTime().getTime());
        phi -= stonyhurstLongitude / MathUtils.radeg;
        theta = stonyhurstLatitude / MathUtils.radeg;

        localRotation = GL3DQuatd.createRotation(theta, GL3DVec3d.XAxis);
        localRotation.rotate(GL3DQuatd.createRotation(phi, GL3DVec3d.YAxis));
    }

    private void retrievePixelParameters(MetaDataContainer m) {
        int pixelImageWidth = m.getPixelWidth();
        int pixelImageHeight = m.getPixelHeight();

        double newSolarPixelRadius = -1.0;

        if (instrument.contains("HMI") || instrument.contains("AIA") || instrument.contains("SWAP") || instrument.contains("VSM") || instrument.contains("NRH") || instrument.contains("GONG") || instrument.contains("H-alpha") || instrument.contains("CALLISTO")) {
            double arcsecPerPixelX = m.tryGetDouble("CDELT1");
            double arcsecPerPixelY = m.tryGetDouble("CDELT2");
            if (Double.isNaN(arcsecPerPixelX)) {
                if (Double.isNaN(arcsecPerPixelY)) {
                    Log.warn(">> HelioviewerMetaData.readPixelParameters() > Both CDELT1 and CDELT2 are NaN. Use 0.6 as default value.");
                    arcsecPerPixelX = 0.6;
                } else {
                    Log.warn(">> HelioviewerMetaData.readPixelParameters() > CDELT1 is NaN. CDELT2 is used.");
                    arcsecPerPixelX = arcsecPerPixelY;
                }
            }
            if (Math.abs(arcsecPerPixelX - arcsecPerPixelY) > arcsecPerPixelX * 0.0001) {
                Log.warn(">> HelioviewerMetaData.readPixelParameters() > CDELT1 and CDELT2 have different values. CDELT1 is used.");
            }
            // distance to sun in meters
            double radiusSunInArcsec = Math.atan(Constants.SunRadiusInMeter / this.dobs) * MathUtils.radeg * 3600;
            newSolarPixelRadius = radiusSunInArcsec / arcsecPerPixelX;
        } else if (instrument.equals("EIT")) {
            newSolarPixelRadius = m.tryGetDouble("SOLAR_R");
            if (newSolarPixelRadius == 0) {
                if (pixelImageWidth == 1024) {
                    newSolarPixelRadius = 360;
                } else if (pixelImageWidth == 512) {
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
                Log.warn("HelioviewerMetaData: STEREO Meta Data inconsistent! Resolution not the same in x and y direction! (1: " + arcSecPerPixel + ", 2: " + arcSecPerPixel2 + ")");
            }
            double solarRadiusPixel = solarRadiusArcSec / arcSecPerPixel;
            newSolarPixelRadius = solarRadiusPixel;
        }

        double sunX = m.tryGetDouble("CRPIX1") - 1;
        double sunY = m.tryGetDouble("CRPIX2") - 1;

        this.pixelImageSize = new Vector2dInt(pixelImageWidth, pixelImageHeight);
        this.sunPixelPositionImage = new GL3DVec2d(sunX, pixelImageHeight - 1 - sunY);

        GL3DVec2d sunPixelPosition = new GL3DVec2d(sunX, sunY);
        meterPerPixel = Constants.SunRadius / newSolarPixelRadius;
        setPhysicalLowerLeftCorner(GL3DVec2d.scale(sunPixelPosition, -meterPerPixel));
        setPhysicalImageSize(new GL3DVec2d(pixelImageWidth * meterPerPixel, pixelImageHeight * meterPerPixel));
    }

    public Region roiToRegion(SubImage roi, double zoompercent) {
        return StaticRegion.createAdaptedRegion((roi.x / zoompercent - sunPixelPositionImage.x) * meterPerPixel, (roi.y / zoompercent - sunPixelPositionImage.y) * meterPerPixel, roi.width * meterPerPixel / zoompercent, roi.height * meterPerPixel / zoompercent);
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

    @Override
    public String getFullName() {
        return fullName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector2dInt getResolution() {
        return pixelImageSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getUnitsPerPixel() {
        return meterPerPixel;
    }

    public GL3DQuatd getLocalRotation() {
        return this.localRotation;
    }

    public double getDobs() {
        return dobs;
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
