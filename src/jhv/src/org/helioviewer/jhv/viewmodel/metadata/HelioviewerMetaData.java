package org.helioviewer.jhv.viewmodel.metadata;

import java.util.Locale;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.viewmodel.imagedata.SubImage;

public class HelioviewerMetaData extends AbstractMetaData {

    private static final boolean normalizeRadius = Boolean.parseBoolean(Settings.getSingletonInstance().getProperty("display.normalize"));

    private String instrument = "";
    private String detector = "";
    private String measurement = " ";
    private String observatory = " ";
    private String fullName = "";

    private double unitPerPixelX = 1;
    private double unitPerPixelY = 1;
    private double sunPositionX = 0;
    private double sunPositionY = 0;

    private final Quat centerRotation;

    @Override
    public Quat getCenterRotation() {
        return centerRotation;
    }

    public HelioviewerMetaData(MetaDataContainer m, int frame) {
        frameNumber = frame;

        identifyObservation(m);

        instrument = instrument.intern();
        detector = detector.intern();
        measurement = measurement.intern();
        observatory = observatory.intern();
        fullName = fullName.intern();

        retrievePosition(m, retrieveDateTime(m));
        centerRotation = retrieveCenterRotation(m);
        retrievePixelParameters(m);

        retrieveOcculterRadii(m);
        retrieveOcculterLinearCutOff(m);

        retrieveResponse();
    }

    private void retrieveResponse() {
        if (instrument.equals("AIA")) {
            responseFactor = AIAResponse.get(viewpoint.time.toString().substring(0, 10), measurement);
        }
    }

    // magic
    private void retrieveOcculterLinearCutOff(MetaDataContainer m) {
        if (detector.equalsIgnoreCase("C2")) {
            double maskRotation = -Math.toRadians(m.tryGetDouble("CROTA"));
            cutOffValue = -region.ulx;
            cutOffDirection = new Vec3(Math.sin(maskRotation) / 0.9625, Math.cos(maskRotation) / 0.9625, 0);
        } else if (instrument.equalsIgnoreCase("SWAP")) {
            double maskRotation = -Math.toRadians(m.tryGetDouble("SOLAR_EP"));
            cutOffValue = -region.ulx;
            cutOffDirection = new Vec3(Math.sin(maskRotation), Math.cos(maskRotation), 0);
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
        if (instrument.equals("MDI") || instrument.equals("HMI") ||
                observatory.equals("Kanzelhoehe") || observatory.equals("ROB-USET") ||
                observatory.equals("NSO-GONG") || observatory.equals("NSO-SOLIS"))
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
            int wvI = m.tryGetInt("WAVELNTH");
            if (wvI == 0) {
                double wvD = m.tryGetDouble("WAVELNTH");
                measurement = wvD == 0 ? " " : String.valueOf(wvD);
            } else {
                measurement = String.valueOf(wvI);
            }
        }

        observatory = m.get("TELESCOP");

        if (instrument.contains("VSM")) {
            fullName = "NSO-SOLIS " + measurement;
        } else if (instrument.contains("HMI")) {
            measurement = m.get("CONTENT");
            String str[] = measurement.split(" ", 2);
            fullName = "HMI " + str[0].toLowerCase(Locale.ENGLISH);
        } else if (detector.equals("C2") || detector.equals("C3")) {
            String measurement1 = m.get("FILTER");
            String measurement2 = m.get("POLAR");
            measurement = measurement1 + ' ' + measurement2;
            fullName = "LASCO " + detector;
        } else if (instrument.equals("MDI")) {
            measurement = m.get("DPC_OBSR");
            fullName = "MDI " + measurement.substring(measurement.indexOf('_') + 1).toLowerCase(Locale.ENGLISH);
        } else if (detector.equals("COR1") || detector.equals("COR2")) {
            observatory = m.get("OBSRVTRY");
            fullName = observatory + ' ' + detector;
        } else if (detector.equals("EUVI")) {
            observatory = m.get("OBSRVTRY");
            fullName = observatory + ' ' + detector + ' ' + measurement;
        } else if (instrument.equals("TRACE")) {
            measurement = m.get("WAVE_LEN");
            fullName = instrument + ' ' + measurement;
        } else if (instrument.equals("XRT")) {
            measurement = m.get("EC_FW1_") + ' ' + m.get("EC_FW2_");
            fullName = instrument + ' ' + measurement;
        } else {
            fullName = instrument + ' ' + measurement;
        }
    }

    private JHVDate retrieveDateTime(MetaDataContainer m) {
        String observedDate;

        // DATE-OBS unusable for MDI and early EIT
        if (instrument.equals("MDI") || instrument.equals("EIT")) {
            observedDate = m.get("DATE_OBS");
        } else {
            observedDate = m.get("DATE-OBS");
            if (observedDate == null) {
                observedDate = m.get("DATE_OBS");
                if (observedDate != null) {
                    if (instrument.equals("LASCO"))
                        observedDate = observedDate.replace('/', '-') + 'T' + m.get("TIME_OBS");
                }
            }
        }
        return observedDate == null ? TimeUtils.EPOCH : new JHVDate(observedDate.substring(0, 19)); // truncate
    }

    private void retrievePosition(MetaDataContainer m, JHVDate dateObs) {
        double distanceObs;
        Position.L p = Sun.getEarth(dateObs);

        if ((distanceObs = m.tryGetDouble("DSUN_OBS") / Sun.RadiusMeter) == 0) {
            distanceObs = p.rad;
            if (observatory.equals("SOHO")) {
                distanceObs *= Sun.L1Factor;
            }
        }

        double stonyhurstLatitude = m.tryGetDouble("HGLT_OBS");
        if (Double.isNaN(stonyhurstLatitude) || stonyhurstLatitude == 0 /* not found */) {
            if ((stonyhurstLatitude = m.tryGetDouble("CRLT_OBS")) == 0 && (stonyhurstLatitude = m.tryGetDouble("REF_B0")) == 0) {
                // presumably not found
                stonyhurstLatitude = p.lat * MathUtils.radeg;
            }
        }
        double theta = stonyhurstLatitude / MathUtils.radeg;

        double stonyhurstLongitude = m.tryGetDouble("HGLN_OBS");
        if (Double.isNaN(stonyhurstLongitude) /* HMI */ || stonyhurstLongitude == 0 /* not found */) {
            stonyhurstLongitude = m.tryGetDouble("REF_L0");
            if (stonyhurstLongitude != 0) {
                stonyhurstLongitude += p.lon * MathUtils.radeg;
            }
        }
        double phi = p.lon - stonyhurstLongitude / MathUtils.radeg;

        viewpoint = new Position.Q(dateObs, distanceObs, new Quat(theta, phi));
        viewpointL = new Position.L(dateObs, distanceObs, phi, theta);
    }

    private Quat retrieveCenterRotation(MetaDataContainer m) {
        if (instrument.equals("AIA")) {
            double crota = m.tryGetDouble("CROTA");
            if (crota == 0) {
                crota = m.tryGetDouble("CROTA1");
                if (crota == 0)
                    crota = m.tryGetDouble("CROTA2");
            }
            if (!Double.isNaN(crota))
                return Quat.rotate(Quat.createRotation(-crota / MathUtils.radeg, new Vec3(0, 0, 1)), viewpoint.orientation);
        }
        return viewpoint.orientation;
    }

    private void retrievePixelParameters(MetaDataContainer m) {
        pixelWidth = m.tryGetInt("NAXIS1");
        pixelHeight = m.tryGetInt("NAXIS2");

        if (instrument.equals("CALLISTO")) { // pixel based
            region = new Region(0, 0, pixelWidth, pixelHeight);
        } else {
            double arcsecPerPixelX = m.tryGetDouble("CDELT1");
            double arcsecPerPixelY = m.tryGetDouble("CDELT2");
            if (Double.isNaN(arcsecPerPixelX) || Double.isNaN(arcsecPerPixelY)) {
                Log.warn("HelioviewerMetaData.retrievePixelParameters() > CDELT1 or CDELT2 are NaN. Use 0.6 as default value.");
                arcsecPerPixelX = arcsecPerPixelY = 0.6;
            }
            if (Math.abs(arcsecPerPixelX - arcsecPerPixelY) > arcsecPerPixelX * 0.0001) {
                Log.warn("HelioviewerMetaData.retrievePixelParameters() > CDELT1 and CDELT2 have different values. CDELT1 is used.");
            }

            double radiusSunInArcsec = Math.atan2(Sun.Radius * getSolarRadiusFactor(), viewpoint.distance) * MathUtils.radeg * 3600;
            double unitPerArcsec = Sun.Radius / radiusSunInArcsec;
            unitPerPixelX = arcsecPerPixelX * unitPerArcsec;
            unitPerPixelY = arcsecPerPixelY * unitPerArcsec;

            double sunX = m.tryGetDouble("CRPIX1") - 0.5;
            double sunY = m.tryGetDouble("CRPIX2") - 0.5;

            if (instrument.equals("XRT")) { // until CRVALx of all datasets can be tested
                double crval1 = m.tryGetDouble("CRVAL1") / arcsecPerPixelX;
                double crval2 = m.tryGetDouble("CRVAL2") / arcsecPerPixelY;

                sunX -= crval1;
                sunY -= crval2;
            }

            sunPositionX = unitPerPixelX * sunX;
            sunPositionY = unitPerPixelY * (pixelHeight - 1 - sunY);

            region = new Region(-sunX * unitPerPixelX, -sunY * unitPerPixelY, pixelWidth * unitPerPixelX, pixelHeight * unitPerPixelY);
        }
    }

    private double getSolarRadiusFactor() {
        if (!normalizeRadius)
            return 1;

        if (measurement.toLowerCase(Locale.ENGLISH).contains("continuum"))
            return Sun.RadiusFactor_6173;

        int wv;
        try {
            wv = Integer.parseInt(measurement);
        } catch (Exception e) {
            return 1;
        }

        if (wv == 0)
            return 1;
        if (wv < 304)
            return Sun.RadiusFactor_171;
        if (wv < 1600)
            return Sun.RadiusFactor_304;
        if (wv < 1700)
            return Sun.RadiusFactor_1600;
        if (wv < 4500)
            return Sun.RadiusFactor_1700;
        if (wv < 6173)
            return 1;
        if (wv < 6562)
            return Sun.RadiusFactor_6173;
        return Sun.RadiusFactor_6562;
    }

    @Override
    public Region roiToRegion(SubImage roi, double factorX, double factorY) {
        return new Region(roi.x * factorX * unitPerPixelX - sunPositionX, roi.y * factorY * unitPerPixelY - sunPositionY,
                          roi.width * factorX * unitPerPixelX, roi.height * factorY * unitPerPixelY);
    }

    public String getDetector() {
        return detector;
    }

    public String getInstrument() {
        return instrument;
    }

    public String getMeasurement() {
        return measurement;
    }

    public String getObservatory() {
        return observatory;
    }

    public String getFullName() {
        return fullName;
    }

}
