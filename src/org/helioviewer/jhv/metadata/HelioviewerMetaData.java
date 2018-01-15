package org.helioviewer.jhv.metadata;

import java.util.Locale;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVDate;

public class HelioviewerMetaData extends AbstractMetaData {

    private static final boolean normalizeAIA = Boolean.parseBoolean(Settings.getSingletonInstance().getProperty("display.normalizeAIA"));
    private static final boolean normalizeRadius = Boolean.parseBoolean(Settings.getSingletonInstance().getProperty("display.normalize"));

    private String instrument = "";
    private String detector = "";
    private String measurement = "";
    private String observatory = "";
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

        instrument = instrument.trim().intern();
        detector = detector.trim().intern();
        measurement = measurement.trim().intern();
        observatory = observatory.trim().intern();
        fullName = fullName.trim().intern();

        retrievePosition(m, retrieveDateTime(m));
        centerRotation = retrieveCenterRotation(m);
        retrievePixelParameters(m);

        retrieveOcculterRadii(m);
        retrieveOcculterLinearCutOff(m);

        retrieveResponse();
    }

    private void retrieveResponse() {
        if (normalizeAIA && instrument.equals("AIA")) {
            responseFactor = AIAResponse.get(viewpoint.time.toString().substring(0, 10), measurement);
        }
    }

    // magic
    private void retrieveOcculterLinearCutOff(MetaDataContainer m) {
        if (detector.equalsIgnoreCase("C2")) {
            double maskRotation = -Math.toRadians(m.getDouble("CROTA").orElse(0.));
            cutOffValue = -region.ulx;
            cutOffDirection = new Vec3(Math.sin(maskRotation) / 0.9625, Math.cos(maskRotation) / 0.9625, 0);
        }/* else if (instrument.equalsIgnoreCase("SWAP")) {
            double maskRotation = -Math.toRadians(m.getDouble("SOLAR_EP").orElse(0.));
            cutOffValue = -region.ulx;
            cutOffDirection = new Vec3(Math.sin(maskRotation), Math.cos(maskRotation), 0);
        }*/
    }

    private void retrieveOcculterRadii(MetaDataContainer m) {
        innerRadius = m.getDouble("HV_ROCC_INNER").orElse(innerRadius);
        outerRadius = m.getDouble("HV_ROCC_OUTER").orElse(outerRadius);
        innerRadius = m.getDouble("HV_INNER").orElse(innerRadius); // Euhforia
        outerRadius = m.getDouble("HV_OUTER").orElse(outerRadius);
        innerRadius *= Sun.Radius;
        outerRadius *= Sun.Radius;

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
        // magic
        if (detector.equalsIgnoreCase("C3"))
            innerRadius *= 1.07;
        if (instrument.equals("MDI") || instrument.equals("HMI") ||
            observatory.equals("Kanzelhoehe") || observatory.equals("ROB-USET") ||
            observatory.equals("NSO-GONG") || observatory.equals("NSO-SOLIS"))
            outerRadius = 1;
    }

    private void identifyObservation(MetaDataContainer m) {
        observatory = m.getString("TELESCOP").orElse("");
        instrument = m.getString("INSTRUME").orElse("");
        instrument = instrument.split("_", 2)[0];
        detector = m.getString("DETECTOR").orElse("");
        measurement = m.getString("WAVELNTH").orElse("");

        if (instrument.contains("VSM")) {
            fullName = "NSO-SOLIS " + measurement;
        } else if (instrument.contains("HMI")) {
            measurement = m.getString("CONTENT").orElse("");
            String str[] = measurement.split(" ", 2);
            fullName = "HMI " + str[0].toLowerCase(Locale.ENGLISH);
        } else if (detector.equals("C2") || detector.equals("C3")) {
            measurement = m.getString("FILTER").orElse("") + ' ' + m.getString("POLAR").orElse("");
            fullName = "LASCO " + detector;
        } else if (instrument.equals("MDI")) {
            measurement = m.getString("DPC_OBSR").orElse("");
            fullName = "MDI " + measurement.substring(measurement.indexOf('_') + 1).toLowerCase(Locale.ENGLISH);
        } else if (detector.equals("COR1") || detector.equals("COR2")) {
            observatory = m.getString("OBSRVTRY").orElse("").replace('_', '-');
            fullName = observatory + ' ' + detector;
        } else if (detector.equals("EUVI")) {
            observatory = m.getString("OBSRVTRY").orElse("").replace('_', '-');
            fullName = observatory + ' ' + detector + ' ' + measurement;
        } else if (instrument.equals("TRACE")) {
            measurement = m.getString("WAVE_LEN").orElse("");
            fullName = instrument + ' ' + measurement;
        } else if (instrument.equals("XRT")) {
            measurement = m.getString("EC_FW1_").orElse("") + ' ' + m.getString("EC_FW2_").orElse("");
            fullName = instrument + ' ' + measurement;
        } else {
            fullName = instrument + ' ' + measurement;
        }
    }

    private JHVDate retrieveDateTime(MetaDataContainer m) {
        String observedDate;
        // DATE-OBS unusable for MDI and early EIT
        if (instrument.equals("MDI") || instrument.equals("EIT")) {
            observedDate = m.getRequiredString("DATE_OBS");
        } else {
            observedDate = m.getString("DATE-OBS").orElse(null);
            if (observedDate == null) {
                observedDate = m.getRequiredString("DATE_OBS");
                if (instrument.equals("LASCO")) {
                    observedDate = observedDate.replace('/', '-') + 'T' + m.getRequiredString("TIME_OBS");
                }
            }
        }
        return new JHVDate(observedDate.substring(0, 19)); // truncate
    }

    private void retrievePosition(MetaDataContainer m, JHVDate dateObs) {
        Position.L p = Sun.getEarth(dateObs);
        double distanceObs = m.getDouble("DSUN_OBS").map(d -> d / Sun.RadiusMeter).orElse(p.rad);
        if (observatory.equals("SOHO"))
            distanceObs *= Sun.L1Factor;

        double stonyhurstLatitude = m.getDouble("HGLT_OBS").map(Math::toRadians).orElse(Double.NaN);
        if (Double.isNaN(stonyhurstLatitude))
            stonyhurstLatitude = m.getDouble("CRLT_OBS").map(Math::toRadians).orElse(Double.NaN);
        if (Double.isNaN(stonyhurstLatitude))
            stonyhurstLatitude = m.getDouble("REF_B0").map(Math::toRadians).orElse(Double.NaN);
        double theta = Double.isNaN(stonyhurstLatitude) ? p.lat : stonyhurstLatitude;

        double stonyhurstLongitude = m.getDouble("HGLN_OBS").map(Math::toRadians).orElse(Double.NaN);
        if (Double.isNaN(stonyhurstLongitude))
            stonyhurstLongitude = m.getDouble("CRLN_OBS").map(v -> Math.toRadians(v) + p.lon).orElse(Double.NaN);
        if (Double.isNaN(stonyhurstLongitude))
            stonyhurstLongitude = m.getDouble("REF_L0").map(v -> Math.toRadians(v) + p.lon).orElse(Double.NaN);
        double phi = Double.isNaN(stonyhurstLongitude) ? p.lon : p.lon - stonyhurstLongitude;

        viewpoint = new Position.Q(dateObs, distanceObs, new Quat(theta, phi));
        viewpointL = new Position.L(dateObs, distanceObs, phi, theta);
    }

    private Quat retrieveCenterRotation(MetaDataContainer m) {
        if (instrument.equals("AIA") || instrument.equals("SWAP")) {
            crota = m.getDouble("CROTA").map(Math::toRadians).orElse(Double.NaN);
            if (Double.isNaN(crota))
                crota = m.getDouble("CROTA1").map(Math::toRadians).orElse(Double.NaN);
            if (Double.isNaN(crota))
                crota = m.getDouble("CROTA2").map(Math::toRadians).orElse(0.);
            return Quat.rotate(Quat.createRotation(-crota, Vec3.ZAxis), viewpoint.orientation);
        }
        return viewpoint.orientation;
    }

    private void retrievePixelParameters(MetaDataContainer m) {
        if (m.getInteger("ZNAXIS").isPresent()) {
            pixelW = m.getRequiredInteger("ZNAXIS1");
            pixelH = m.getRequiredInteger("ZNAXIS2");
        } else {
            pixelW = m.getRequiredInteger("NAXIS1");
            pixelH = m.getRequiredInteger("NAXIS2");
        }

        if (instrument.equals("CALLISTO")) { // pixel based
            region = new Region(0, 0, pixelW, pixelH);
        } else {
            double arcsecPerPixelX = m.getRequiredDouble("CDELT1");
            double arcsecPerPixelY = m.getRequiredDouble("CDELT2");
            double radiusSunInArcsec = Math.toDegrees(Math.atan2(Sun.Radius * getSolarRadiusFactor(), viewpoint.distance)) * 3600;
            unitPerArcsec = Sun.Radius / radiusSunInArcsec;

            unitPerPixelX = arcsecPerPixelX * unitPerArcsec;
            unitPerPixelY = arcsecPerPixelY * unitPerArcsec;

            double sunX = m.getDouble("CRPIX1").orElse((pixelW + 1) / 2.) - .5;
            double sunY = m.getDouble("CRPIX2").orElse((pixelH + 1) / 2.) - .5;

            if (instrument.equals("XRT")) { // until CRVALx of all datasets can be tested
                double crval1 = m.getDouble("CRVAL1").orElse(0.) / arcsecPerPixelX;
                double crval2 = m.getDouble("CRVAL2").orElse(0.) / arcsecPerPixelY;

                sunX -= crval1;
                sunY -= crval2;
            }

            sunPositionX = unitPerPixelX * sunX;
            sunPositionY = unitPerPixelY * (pixelH - 1 - sunY);

            region = new Region(-sunX * unitPerPixelX, -sunY * unitPerPixelY, pixelW * unitPerPixelX, pixelH * unitPerPixelY);
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
