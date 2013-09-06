package org.helioviewer.viewmodel.metadata;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.MathUtils;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.viewmodel.view.cache.HelioviewerDateTimeCache;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

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
public class HelioviewerMetaData extends AbstractMetaData implements SunMetaData, ObserverMetaData, ImageSizeMetaData, NonConstantMetaData {

    protected MetaDataContainer metaDataContainer;
    private String instrument = "";
    private String detector = "";
    private String measurement = " ";
    private String observatory = " ";
    private String fullName = "";
    private Vector2dInt pixelImageSize = new Vector2dInt();
    private double solarPixelRadius = -1;
    private Vector2dDouble sunPixelPosition = new Vector2dDouble();

    private double meterPerPixel;
    private ImmutableDateTime time;

    /**
     * Default constructor.
     * 
     * Tries to read all informations required.
     * 
     * @param m
     *            Meta data container serving as a base for the construction
     */
    public HelioviewerMetaData(MetaDataContainer m) {

        metaDataContainer = m;

        if (m.get("INSTRUME") == null)
            return;

        detector = m.get("DETECTOR");
        instrument = m.get("INSTRUME");

        if (detector == null) {
            detector = " ";
        }
        if (instrument == null) {
            instrument = " ";
        }
        updateDateTime();
        updatePixelParameters();

        setPhysicalLowerLeftCorner(sunPixelPosition.scale(-meterPerPixel));
        setPhysicalImageSize(new Vector2dDouble(pixelImageSize.getX() * meterPerPixel, pixelImageSize.getY() * meterPerPixel));

        if (instrument.contains("AIA")) {
            instrument = "AIA";
            measurement = m.get("WAVELNTH");
            observatory = m.get("TELESCOP");
            fullName = "AIA " + measurement;
        }

        else if (instrument.contains("SWAP")) {
            instrument = "SWAP";
            measurement = m.get("WAVELNTH");
            observatory = m.get("TELESCOP");
            fullName = "SWAP " + measurement;
        }

        else if (instrument.contains("HMI")) {
            instrument = "HMI";
            measurement = m.get("CONTENT");
            observatory = m.get("TELESCOP");
            fullName = "HMI " + measurement.substring(0, 1) + measurement.substring(1, 3).toLowerCase();
        }

        else if (instrument.equals("EIT")) {
            measurement = m.get("WAVELNTH");
            if (measurement == null) {
                measurement = "" + m.tryGetInt("WAVELNTH");
            }
            observatory = m.get("TELESCOP");
            fullName = "EIT " + measurement;
        }

        else if (detector.equals("C2") || detector.equals("C3")) {

            String measurement1 = m.get("FILTER");
            String measurement2 = m.get("POLAR");
            measurement = measurement1 + " " + measurement2;

            observatory = m.get("TELESCOP");
            fullName = "LASCO " + detector;
        }

        else if (instrument.equals("MDI")) {
            observatory = m.get("TELESCOP");
            measurement = m.get("DPC_OBSR");
            fullName = "MDI " + measurement.substring(3, 6);
        }

        else if (detector.equals("COR1") || detector.equals("COR2") || detector.equals("EUVI")) {
            observatory = m.get("OBSRVTRY");
            measurement = m.get("WAVELNTH");
            if (measurement == null) {
                measurement = "" + m.tryGetDouble("WAVELNTH");
            }
            fullName = instrument + " " + detector;
        }
    }

    /**
     * Reads the non-constant pixel parameters of the meta data.
     * 
     * This includes the resolution as well as position and size of the sun. The
     * function also checks, whether these values have changed and returns true
     * if so.
     * 
     * @return true, if the pixel parameters have changed, false otherwise
     */
    private boolean updatePixelParameters() {

        boolean changed = false;

        if (pixelImageSize.getX() != metaDataContainer.getPixelWidth() || pixelImageSize.getY() != metaDataContainer.getPixelHeight()) {
            pixelImageSize = new Vector2dInt(metaDataContainer.getPixelWidth(), metaDataContainer.getPixelHeight());
            changed = true;
        }

        double newSolarPixelRadius = -1.0;
        double allowedRelativeDifference = 0.01;

        if (instrument.contains("AIA") || instrument.contains("SWAP")) {
            double arcsecPerPixelX = metaDataContainer.tryGetDouble("CDELT1");
            double arcsecPerPixelY = metaDataContainer.tryGetDouble("CDELT2");
            if (Double.isNaN(arcsecPerPixelX)) {
                if (Double.isNaN(arcsecPerPixelY)) {
                    Log.warn(">> HelioviewerMetaData.readPixelParamters() > Both CDELT1 and CDELT2 are NaN. Use 0.6 as default value.");
                    arcsecPerPixelX = 0.6;
                } else {
                    Log.warn(">> HelioviewerMetaData.readPixelParamters() > CDELT1 is NaN. CDELT2 is used.");
                    arcsecPerPixelX = arcsecPerPixelY;
                }
            }
            if (Math.abs(arcsecPerPixelX - arcsecPerPixelY) > arcsecPerPixelX * 0.0001) {
                Log.warn(">> HelioviewerMetaData.readPixelParamters() > CDELT1 and CDELT2 have different values. CDELT1 is used.");
            }
            // distance to sun in meters
            double distanceToSun = metaDataContainer.tryGetDouble("DSUN_OBS");
            double radiusSunInArcsec = Math.atan(Constants.SunRadius / distanceToSun) * MathUtils.radeg * 3600;
            newSolarPixelRadius = radiusSunInArcsec / arcsecPerPixelX;

        } else if (instrument.contains("HMI")) {
            double arcsecPerPixelX = metaDataContainer.tryGetDouble("CDELT1");
            double arcsecPerPixelY = metaDataContainer.tryGetDouble("CDELT2");
            if (Double.isNaN(arcsecPerPixelX)) {
                if (Double.isNaN(arcsecPerPixelY)) {
                    Log.warn(">> HelioviewerMetaData.readPixelParamters() > Both CDELT1 and CDELT2 are NaN. Use 0.6 as default value.");
                    arcsecPerPixelX = 0.6;
                } else {
                    Log.warn(">> HelioviewerMetaData.readPixelParamters() > CDELT1 is NaN. CDELT2 is used.");
                    arcsecPerPixelX = arcsecPerPixelY;
                }
            }
            if (Math.abs(arcsecPerPixelX - arcsecPerPixelY) > arcsecPerPixelX * 0.0001) {
                Log.warn(">> HelioviewerMetaData.readPixelParamters() > CDELT1 and CDELT2 have different values. CDELT1 is used.");
            }
            // distance to sun in meters
            double distanceToSun = metaDataContainer.tryGetDouble("DSUN_OBS");
            double radiusSunInArcsec = Math.atan(Constants.SunRadius / distanceToSun) * MathUtils.radeg * 3600;
            newSolarPixelRadius = radiusSunInArcsec / arcsecPerPixelX;

        } else if (instrument.equals("EIT")) {
            newSolarPixelRadius = metaDataContainer.tryGetDouble("SOLAR_R");

            if (newSolarPixelRadius == 0) {
                if (pixelImageSize.getX() == 1024) {
                    newSolarPixelRadius = 360;
                } else if (pixelImageSize.getX() == 512) {
                    newSolarPixelRadius = 180;
                }
            }

        } else if (detector.equals("C2") || detector.equals("C3")) {

            newSolarPixelRadius = metaDataContainer.tryGetDouble("RSUN");
            allowedRelativeDifference = 0.05;

            if (newSolarPixelRadius == 0) {
                if (detector.equals("C2")) {
                    newSolarPixelRadius = 80.814221;
                } else if (detector.equals("C3")) {
                    newSolarPixelRadius = 17.173021;
                }

            }

        } else if (instrument.equals("MDI")) {
            newSolarPixelRadius = metaDataContainer.tryGetDouble("R_SUN");

        } else if (detector.equals("COR1") || detector.equals("COR2") || detector.equals("EUVI")) {
            double solarRadiusArcSec = metaDataContainer.tryGetDouble("RSUN");
            double arcSecPerPixel = metaDataContainer.tryGetDouble("CDELT1");
            double arcSecPerPixel2 = metaDataContainer.tryGetDouble("CDELT2");
            if (arcSecPerPixel != arcSecPerPixel2) {
                Log.warn("HelioviewerMetaData: STEREO Meta Data inconsistent! Resolution not the same in x and y direction! (1: " + arcSecPerPixel + ", 2: " + arcSecPerPixel2 + ")");
            }
            double solarRadiusPixel = solarRadiusArcSec / arcSecPerPixel;
            newSolarPixelRadius = solarRadiusPixel;
            // newSolarPixelRadius = metaDataContainer.tryGetDouble("RSUN");
        }

        if (newSolarPixelRadius > 0) {
            double allowedAbsoluteDifference = newSolarPixelRadius * allowedRelativeDifference;
            if (Math.abs(solarPixelRadius - newSolarPixelRadius) > allowedAbsoluteDifference) {
                changed = true;
            }

            double sunX = metaDataContainer.tryGetDouble("CRPIX1");
            double sunY = metaDataContainer.tryGetDouble("CRPIX2");

            if (changed || Math.abs(sunPixelPosition.getX() - sunX) > allowedAbsoluteDifference || Math.abs(sunPixelPosition.getY() - sunY) > allowedAbsoluteDifference) {
                sunPixelPosition = new Vector2dDouble(sunX, sunY);
                changed = true;
            }
        }

        if (changed) {
            solarPixelRadius = newSolarPixelRadius;
            meterPerPixel = Constants.SunRadius / solarPixelRadius;
            setPhysicalLowerLeftCorner(sunPixelPosition.scale(-meterPerPixel));
            setPhysicalImageSize(new Vector2dDouble(pixelImageSize.getX() * meterPerPixel, pixelImageSize.getY() * meterPerPixel));
        }

        return changed;
    }

    /**
     * {@inheritDoc}
     */
    public void updateDateTime() {
        String observedDate;
        if (instrument.contains("SWAP")) {
            observedDate = metaDataContainer.get("DATE-OBS");
        } else {
            observedDate = metaDataContainer.get("DATE_OBS");
        }
        if (detector.equals("C2") || detector.equals("C3")) {
            observedDate += "T" + metaDataContainer.get("TIME_OBS");
        }

        time = HelioviewerDateTimeCache.parseDateTime(observedDate);
    }

    /**
     * {@inheritDoc}
     */
    public void updateDateTime(ImmutableDateTime newDateTime) {
        time = newDateTime;
    }

    /**
     * {@inheritDoc}
     */
    public String getDetector() {
        return detector;
    }

    /**
     * {@inheritDoc}
     */
    public String getInstrument() {
        return instrument;
    }

    /**
     * {@inheritDoc}
     */
    public String getMeasurement() {
        return measurement;
    }

    /**
     * {@inheritDoc}
     */
    public String getObservatory() {
        return observatory;
    }

    public String getFullName() {
        return fullName;
    }

    /**
     * {@inheritDoc}
     */
    public double getSunPixelRadius() {
        return solarPixelRadius;
    }

    /**
     * {@inheritDoc}
     */
    public Vector2dDouble getSunPixelPosition() {
        return sunPixelPosition;
    }

    /**
     * {@inheritDoc}
     */
    public Vector2dInt getResolution() {
        return pixelImageSize;
    }

    /**
     * {@inheritDoc}
     */
    public double getUnitsPerPixel() {
        return meterPerPixel;
    }

    /**
     * {@inheritDoc}
     */
    public ImmutableDateTime getDateTime() {
        return time;
    }

    /**
     * {@inheritDoc}
     * 
     * In this case, the resolution and the solar pixel position are checked.
     */
    public boolean checkForModifications() {
        return updatePixelParameters();
    }

    /**
     * Get sun radius in arcsec
     * 
     * @author Carlos Martin
     * 
     * @return sun radius (in arcsec)
     */
    public double getRadiusSuninArcsec() {
        double distanceToSun = metaDataContainer.tryGetDouble("DSUN_OBS");
        return Math.atan(Constants.SunRadius / distanceToSun) * MathUtils.radeg * 3600;
    }
}
