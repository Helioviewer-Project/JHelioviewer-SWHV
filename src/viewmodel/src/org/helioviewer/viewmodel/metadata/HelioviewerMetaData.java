package org.helioviewer.viewmodel.metadata;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.MathUtils;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.jhv.gui.states.StateController;
import org.helioviewer.jhv.gui.states.ViewStateEnum;
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
public class HelioviewerMetaData extends AbstractMetaData implements SunMetaData, ObserverMetaData, ImageSizeMetaData {

    protected MetaDataContainer m;
    private String instrument = "";
    private String detector = "";
    private String measurement = " ";
    private String observatory = " ";
    private String fullName = "";
    private Vector2dInt pixelImageSize = new Vector2dInt();
    private double solarPixelRadius = -1;
    private Vector2dDouble sunPixelPosition = new Vector2dDouble();

    private double meterPerPixel;
    protected double theta;
    protected double phi;
    private Vector2dDouble sunPixelPositionImage = new Vector2dDouble();

    /**
     * Default constructor.
     *
     * Tries to read all informations required.
     *
     * @param m
     *            Meta data container serving as a base for the construction
     */
    public HelioviewerMetaData(MetaDataContainer metaDataContainer) {
        m = metaDataContainer;

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
        } else if (instrument.contains("SWAP")) {
            instrument = "SWAP";
            measurement = m.get("WAVELNTH");
            observatory = m.get("TELESCOP");
            fullName = "SWAP " + measurement;
        } else if (instrument.contains("VSM")) {
            instrument = "VSM";
            measurement = m.get("WAVELNTH");
            observatory = m.get("TELESCOP");
            fullName = "NSO-SOLIS " + measurement;
        } else if (instrument.contains("GONG")) {
            instrument = "GONG";
            measurement = m.get("WAVELNTH");
            observatory = m.get("TELESCOP");
            fullName = "GONG " + measurement;
        } else if (instrument.contains("H-alpha")) {
            instrument = "H-alpha";
            measurement = m.get("WAVELNTH");
            observatory = m.get("TELESCOP");
            fullName = "H-alpha " + measurement;
        } else if (instrument.contains("HMI")) {
            instrument = "HMI";
            measurement = m.get("CONTENT");
            observatory = m.get("TELESCOP");
            fullName = "HMI " + measurement.substring(0, 1) + measurement.substring(1, 3).toLowerCase();
        } else if (instrument.contains("NRH")) {
            instrument = "NRH";
            measurement = m.get("WAVELNTH");
            observatory = m.get("TELESCOP");
            fullName = "NRH " + measurement;
        } else if (instrument.equals("EIT")) {
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

        else if (detector.equals("COR1") || detector.equals("COR2")) {
            observatory = m.get("OBSRVTRY");
            measurement = m.get("WAVELNTH");
            if (measurement == null) {
                measurement = "" + m.tryGetDouble("WAVELNTH");
            }
            fullName = instrument + " " + detector;
        } else if (detector.equals("EUVI")) {
            observatory = m.get("OBSRVTRY");
            measurement = m.get("WAVELNTH");
            if (measurement == null) {
                measurement = "" + m.tryGetDouble("WAVELNTH");
            }
            fullName = instrument + " " + detector + " " + measurement;
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
    protected boolean updatePixelParameters() {
        boolean changed = false;

        if (pixelImageSize.getX() != m.getPixelWidth() || pixelImageSize.getY() != m.getPixelHeight()) {
            pixelImageSize = new Vector2dInt(m.getPixelWidth(), m.getPixelHeight());
            changed = true;
        }

        int pixelImageWidth = pixelImageSize.getX();
        int pixelImageHeight = pixelImageSize.getY();

        double newSolarPixelRadius = -1.0;
        double allowedRelativeDifference = 0.005;

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
            double distanceToSun = m.tryGetDouble("DSUN_OBS");
            double radiusSunInArcsec = Math.atan(Constants.SunRadiusInMeter / distanceToSun) * MathUtils.radeg * 3600;
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
            allowedRelativeDifference = 0.05;

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

        if (newSolarPixelRadius > 0) {
            if (Math.abs(solarPixelRadius - newSolarPixelRadius) > solarPixelRadius * allowedRelativeDifference) {
                changed = true;
            }
        }

        double allowedCenterPixelDistance = 1.;
        double sunX = m.tryGetDouble("CRPIX1") - 1;
        double sunY = m.tryGetDouble("CRPIX2") - 1;
        double dX = sunPixelPosition.getX() - sunX;
        double dY = sunPixelPosition.getY() - sunY;

        if (dX * dX + dY * dY > allowedCenterPixelDistance * allowedCenterPixelDistance) {
            changed = true;
            sunPixelPosition = new Vector2dDouble(sunX, sunY);
            sunPixelPositionImage = new Vector2dDouble(sunX, pixelImageHeight - 1 - sunY);
        }

        if (changed) {
            solarPixelRadius = newSolarPixelRadius;
            meterPerPixel = Constants.SunRadius / solarPixelRadius;
            setPhysicalLowerLeftCorner(sunPixelPosition.scale(-meterPerPixel));
            setPhysicalImageSize(new Vector2dDouble(pixelImageWidth * meterPerPixel, pixelImageHeight * meterPerPixel));
        }

        return changed;
    }

    public Region roiToRegion(SubImage roi, double zoompercent) {
        return StaticRegion.createAdaptedRegion((roi.x / zoompercent - sunPixelPositionImage.getX()) * meterPerPixel, (roi.y / zoompercent - sunPixelPositionImage.getY()) * meterPerPixel, roi.width * meterPerPixel / zoompercent, roi.height * meterPerPixel / zoompercent);
    }

    private void updateDateTime() {
        String observedDate = m.get("DATE-OBS");
        if (observedDate == null) {
            observedDate = m.get("DATE_OBS");

            if (observedDate != null && instrument.equals("LASCO")) {
                observedDate += "T" + m.get("TIME_OBS");
            }
        }

        dateTime = parseDateTime(observedDate);
    }

    private static ImmutableDateTime parseDateTime(String dateTime) {
        int year = 0, month = 0, day = 0, hour = 0, minute = 0, second = 0;

        if (dateTime != null) {
            try {
                String[] firstDivide = dateTime.split("T");
                String[] secondDivide1 = firstDivide[0].split("[-/]");
                String[] secondDivide2 = firstDivide[1].split(":");
                String[] thirdDivide = secondDivide2[2].split("\\.");
                year = Integer.valueOf(secondDivide1[0]);
                month = Integer.valueOf(secondDivide1[1]);
                day = Integer.valueOf(secondDivide1[2]);
                hour = Integer.valueOf(secondDivide2[0]);
                minute = Integer.valueOf(secondDivide2[1]);
                second = Integer.valueOf(thirdDivide[0]);
            } catch (Exception e) {
                year = 0;
                month = 0;
                day = 0;
                hour = 0;
                minute = 0;
                second = 0;
            }
        }

        return new ImmutableDateTime(year, month != 0 ? month - 1 : 0, day, hour, minute, second);
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
    public double getSunPixelRadius() {
        return solarPixelRadius;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector2dDouble getSunPixelPosition() {
        return sunPixelPosition;
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

    public double getPhi() {
        if (StateController.getInstance().getCurrentState() == ViewStateEnum.View3D.getState())
            return this.phi;
        return 0.;
    }

    public double getTheta() {
        if (StateController.getInstance().getCurrentState() == ViewStateEnum.View3D.getState())
            return this.theta;
        return 0.;
    }

}
