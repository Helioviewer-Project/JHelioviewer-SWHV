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

    private double stonyhurstLongitude;
    private double stonyhurstLatitude;
    private boolean stonyhurstAvailable = false;
    private double refb0;
    private double refl0;
    private boolean refAvailable;
    private final double innerRadius = 0.;
    private final double outerRadius = 40.;

    private String instrument = "";
    private String detector = "";
    private String measurement = " ";
    private String observatory = " ";
    private String fullName = "";

    private Vector2dInt pixelImageSize = new Vector2dInt();
    private GL3DVec2d sunPixelPosition = new GL3DVec2d();

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

        instrument = m.get("INSTRUME");
        if (instrument == null)
            return;
        instrument = instrument.split("_")[0];

        detector = m.get("DETECTOR");
        if (detector == null) {
            detector = " ";
        }

        updateDateTime(m);
        updatePosition(m);
        updatePixelParameters(m);

        setPhysicalLowerLeftCorner(GL3DVec2d.scale(sunPixelPosition, -meterPerPixel));
        setPhysicalImageSize(new GL3DVec2d(pixelImageSize.getX() * meterPerPixel, pixelImageSize.getY() * meterPerPixel));
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

    private void updatePixelParameters(MetaDataContainer m) {
        if (pixelImageSize.getX() != m.getPixelWidth() || pixelImageSize.getY() != m.getPixelHeight()) {
            pixelImageSize = new Vector2dInt(m.getPixelWidth(), m.getPixelHeight());
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

        double allowedCenterPixelDistance = 1.;
        double sunX = m.tryGetDouble("CRPIX1") - 1;
        double sunY = m.tryGetDouble("CRPIX2") - 1;
        double dX = sunPixelPosition.x - sunX;
        double dY = sunPixelPosition.y - sunY;

        if (dX * dX + dY * dY > allowedCenterPixelDistance * allowedCenterPixelDistance) {
            sunPixelPosition = new GL3DVec2d(sunX, sunY);
            sunPixelPositionImage = new GL3DVec2d(sunX, pixelImageHeight - 1 - sunY);
        }

        meterPerPixel = Constants.SunRadius / newSolarPixelRadius;
        setPhysicalLowerLeftCorner(GL3DVec2d.scale(sunPixelPosition, -meterPerPixel));
        setPhysicalImageSize(new GL3DVec2d(pixelImageWidth * meterPerPixel, pixelImageHeight * meterPerPixel));
    }

    public Region roiToRegion(SubImage roi, double zoompercent) {
        return StaticRegion.createAdaptedRegion((roi.x / zoompercent - sunPixelPositionImage.x) * meterPerPixel, (roi.y / zoompercent - sunPixelPositionImage.y) * meterPerPixel, roi.width * meterPerPixel / zoompercent, roi.height * meterPerPixel / zoompercent);
    }

    private void updateDateTime(MetaDataContainer m) {
        String observedDate = m.get("DATE-OBS");
        if (observedDate == null) {
            observedDate = m.get("DATE_OBS");
            if (observedDate != null && instrument.equals("LASCO")) {
                observedDate += "T" + m.get("TIME_OBS");
            }
        }

        dateTime = ImmutableDateTime.parseDateTime(observedDate);
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

    private void updatePosition(MetaDataContainer m) {
        this.dobs = m.tryGetDouble("DSUN_OBS");

        double crlt = m.tryGetDouble("CRLT_OBS");
        double crln = m.tryGetDouble("CRLN_OBS");
        boolean carringtonAvailable = crlt != 0.0 || crln != 0.0;

        this.refb0 = m.tryGetDouble("REF_B0");
        this.refl0 = m.tryGetDouble("REF_L0");
        this.refAvailable = this.refb0 != 0.0 || this.refl0 != 0.0;

        this.stonyhurstLatitude = m.tryGetDouble("HGLT_OBS");
        if (this.stonyhurstLatitude == 0) {
            this.stonyhurstLatitude = crlt;
            if (this.stonyhurstLatitude == 0) {
                this.stonyhurstLatitude = this.refb0;
            }
        }
        this.stonyhurstLongitude = m.tryGetDouble("HGLN_OBS");
        if (this.refl0 != 0.) {
            this.stonyhurstLongitude = this.refl0 - Astronomy.getL0Degree(this.getDateTime().getTime());
        }

        if (this.getInstrument().contains("GONG") || this.getObservatory().contains("USET") || this.getObservatory().contains("SOLIS")) {
            this.stonyhurstLongitude = 0.0;
        }

        this.stonyhurstAvailable = this.stonyhurstLatitude != 0.0 || this.stonyhurstLongitude != 0.0;

        double theta = -Astronomy.getB0InRadians(this.getDateTime().getTime());
        double phi = Astronomy.getL0Radians(this.getDateTime().getTime());
        phi -= getStonyhurstLongitude() / MathUtils.radeg;
        theta = getStonyhurstLatitude() / MathUtils.radeg;

        localRotation = GL3DQuatd.createRotation(theta, GL3DVec3d.XAxis);
        localRotation.rotate(GL3DQuatd.createRotation(phi, GL3DVec3d.YAxis));
    }

    public double getDobs() {
        return dobs;
    }

    public boolean isCarringtonProvided() {
        return carringtonAvailable;
    }

    public boolean isStonyhurstProvided() {
        return stonyhurstAvailable;
    }

    public double getStonyhurstLatitude() {
        return stonyhurstLatitude;
    }

    public double getStonyhurstLongitude() {
        return stonyhurstLongitude;
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
