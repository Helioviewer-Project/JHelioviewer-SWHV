package org.helioviewer.jhv.metadata;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;

public class HelioviewerMetaData extends BaseMetaData {

    private static final boolean normalizeAIA = Boolean.parseBoolean(Settings.getProperty("display.normalizeAIA"));
    private static final boolean normalizeRadius = Boolean.parseBoolean(Settings.getProperty("display.normalize"));

    private String instrument = "";
    private String detector = "";
    private String measurement = "";
    private String observatory = "";

    private double sunPositionX = 0;
    private double sunPositionY = 0;

    private final Quat centerRotation;

    @Nonnull
    @Override
    public Quat getCenterRotation() {
        return centerRotation;
    }

    public HelioviewerMetaData(@Nonnull MetaDataContainer m, int frame, boolean normalizeResponse) {
        frameNumber = frame;

        identifyObservation(m);

        instrument = instrument.trim().intern();
        detector = detector.trim().intern();
        measurement = measurement.trim().intern();
        observatory = observatory.trim().intern();
        displayName = displayName.trim().intern();

        retrievePosition(m, retrieveTime(m));
        centerRotation = retrieveCenterRotation(m);
        retrievePixelParameters(m);

        retrieveOcculterRadii(m);
        retrieveOcculterLinearCutOff(m);
        retrieveSector(m);

        retrieveUnit(m);

        if (normalizeResponse)
            retrieveResponse();

        if (instrument.equals("Euhforia"))
            calculateDepth = true;
    }

    private void retrieveSector(MetaDataContainer m) {
        double s0 = m.getDouble("HV_SECT0").map(Math::toRadians).orElse(0.);
        double s1 = m.getDouble("HV_SECT1").map(Math::toRadians).orElse(0.);
        sector0 = (float) s0;
        sector1 = (float) s1;
    }

    private void retrieveUnit(MetaDataContainer m) {
        unit = m.getString("BUNIT").orElse(unit);
        unit = unit.replace("-1", "\u207B\u00B9").replace("-2", "\u207B\u00B2").replace("-3", "\u207B\u00B3").replace(" ", "").intern();

        // a linear physical LUT
        Optional<Double> mZero = m.getDouble("HV_ZERO");
        Optional<Double> mScale = m.getDouble("HV_SCALE");
        Optional<Double> mDatamax = m.getDouble("DATAMAX");
        if (mZero.isPresent() && mScale.isPresent() && mDatamax.isPresent()) {
            double zero = mZero.get();
            double scale = mScale.get();
            int size = MathUtils.clip((int) Math.ceil(mDatamax.get()) + 1, 0, 65535);
            physLUT = new float[size];
            for (int i = 0; i < size; i++)
                physLUT[i] = (float) (zero + i * scale);
        }
    }

    private void retrieveResponse() {
        if (normalizeAIA && instrument.equals("AIA")) {
            responseFactor = (float) AIAResponse.get(viewpoint.time.toString().substring(0, 10), measurement);
        }
    }

    // magic
    private void retrieveOcculterLinearCutOff(MetaDataContainer m) {
        if (detector.equals("C2")) {
            cutOffValue = (float) -region.ulx;
            double maskRotation = -m.getDouble("CROTA").map(Math::toRadians).orElse(0.); // C2 JP2 already rotated
            cutOffX = (float) (Math.sin(maskRotation) / 0.9625);
            cutOffY = (float) (Math.cos(maskRotation) / 0.9625);
        }/* else if (instrument.equals("SWAP")) {
            cutOffValue = (float) -region.ulx;
            double maskRotation = -m.getDouble("SOLAR_EP").map(Math::toRadians).orElse(0.);
            cutOffX = (float) Math.sin(maskRotation);
            cutOffY = (float) Math.cos(maskRotation);
        }*/
    }

    private void retrieveOcculterRadii(MetaDataContainer m) {
        double inner = innerRadius;
        double outer = outerRadius;

        inner = m.getDouble("HV_ROCC_INNER").orElse(inner);
        outer = m.getDouble("HV_ROCC_OUTER").orElse(outer);
        inner = m.getDouble("HV_INNER").orElse(inner); // Euhforia
        outer = m.getDouble("HV_OUTER").orElse(outer);
        inner *= Sun.Radius;
        outer *= Sun.Radius;

        if (inner == 0) {
            if (detector.equals("C2")) {
                inner = 2.3 * Sun.Radius;
                outer = 8.0 * Sun.Radius;
            } else if (detector.equals("C3")) {
                inner = 4.4 * Sun.Radius;
                outer = 31.5 * Sun.Radius;
            } else if (observatory.equals("STEREO-A") && detector.equals("COR1")) {
                inner = 1.36 * Sun.Radius;
                outer = 4.5 * Sun.Radius;
            } else if (observatory.equals("STEREO-A") && detector.equals("COR2")) {
                inner = 2.4 * Sun.Radius;
                outer = 15.6 * Sun.Radius;
            } else if (observatory.equals("STEREO-B") && detector.equals("COR1")) {
                inner = 1.5 * Sun.Radius;
                outer = 4.9 * Sun.Radius;
            } else if (observatory.equals("STEREO-B") && detector.equals("COR2")) {
                inner = 3.25 * Sun.Radius;
                outer = 17 * Sun.Radius;
            }
        }
        // magic
        if (detector.equals("C3"))
            inner *= 1.07;
        if (instrument.equals("MDI") || instrument.equals("HMI") ||
                observatory.equals("Kanzelhoehe") || observatory.equals("ROB-USET") ||
                observatory.equals("NSO-GONG") || observatory.equals("NSO-SOLIS"))
            outer = 1;

        innerRadius = (float) inner;
        outerRadius = (float) outer;
    }

    private void identifyObservation(MetaDataContainer m) {
        observatory = m.getString("TELESCOP").orElse("");
        instrument = m.getString("INSTRUME").orElse("");
        instrument = instrument.split("_", 2)[0];
        detector = m.getString("DETECTOR").orElse("");
        measurement = m.getString("WAVELNTH").orElse("");

        if (measurement.endsWith("."))
            measurement = measurement.substring(0, measurement.length() - 1);

        if (instrument.contains("VSM")) {
            displayName = "NSO-SOLIS " + measurement;
        } else if (instrument.contains("HMI")) {
            measurement = m.getString("CONTENT").orElse("");
            String[] str = measurement.split(" ", 2);
            displayName = "HMI " + str[0].toLowerCase();
        } else if (detector.equals("C2") || detector.equals("C3")) {
            measurement = m.getString("FILTER").orElse("") + ' ' + m.getString("POLAR").orElse("");
            displayName = "LASCO " + detector;
        } else if (instrument.equals("MDI")) {
            measurement = m.getString("DPC_OBSR").orElse("");
            displayName = "MDI " + measurement.substring(measurement.indexOf('_') + 1).toLowerCase();
        } else if (detector.equals("COR1") || detector.equals("COR2")) {
            observatory = m.getString("OBSRVTRY").orElse("").replace('_', '-');
            displayName = observatory + ' ' + detector;
        } else if (detector.equals("EUVI")) {
            observatory = m.getString("OBSRVTRY").orElse("").replace('_', '-');
            displayName = observatory + ' ' + detector + ' ' + measurement;
        } else if (instrument.equals("TRACE")) {
            measurement = m.getString("WAVE_LEN").orElse("");
            displayName = instrument + ' ' + measurement;
        } else if (instrument.equals("XRT")) {
            measurement = m.getString("EC_FW1_").orElse("") + ' ' + m.getString("EC_FW2_").orElse("");
            displayName = instrument + ' ' + measurement;
        } else if (instrument.equals("GOES-R Series Solar Ultraviolet Imager")) {
            instrument = "SUVI";
            displayName = instrument + ' ' + measurement;
        } else if (instrument.equals("COSMO K-Coronagraph")) {
            displayName = "COSMO KCor";
        } else if (instrument.equals("SJI")) {
            measurement = m.getDouble("TWAVE1").map(w -> String.valueOf(w.longValue())).orElse("");
            displayName = observatory + ' ' + instrument + ' ' + measurement;
        } else if (instrument.equals("EUI")) {
            displayName = instrument + ' ' + detector.replace('_', '-') + ' ' + measurement;
        } else {
            displayName = instrument + ' ' + measurement;
        }
    }

    private JHVTime retrieveTime(MetaDataContainer m) {
        String observedDate = m.getString("DATE_OBS").orElseGet(() -> m.getRequiredString("DATE-OBS")); // DATE-OBS unusable for MDI and early EIT
        if (instrument.equals("LASCO")) {
            String observedTime = m.getString("TIME_OBS").orElseGet(() -> m.getRequiredString("TIME-OBS"));
            observedDate = observedDate.replace('/', '-') + 'T' + observedTime;
        }
        return new JHVTime(observedDate.substring(0, 19)); // truncate
    }

    private void retrievePosition(MetaDataContainer m, JHVTime dateObs) {
        Position p = Sun.getEarth(dateObs);
        double distanceObs = m.getDouble("DSUN_OBS").map(d -> d / Sun.RadiusMeter).orElse(p.distance);
        if (observatory.equals("SOHO"))
            distanceObs *= Sun.L1Factor;

        double theta = m.getDouble("HGLT_OBS").map(Math::toRadians).orElse(p.lat);
        double phi = m.getDouble("HGLN_OBS").map(v -> p.lon - Math.toRadians(v)).orElse(p.lon);
        viewpoint = new Position(dateObs, distanceObs, phi, theta);
    }

    private Quat retrieveCenterRotation(MetaDataContainer m) {
        if (instrument.equals("AIA") || instrument.equals("HMI") || instrument.equals("SWAP") || instrument.equals("SUVI") || instrument.equals("EUI") || instrument.equals("SoloHI")) {
            double c = m.getDouble("CROTA").map(Math::toRadians)
                    .or(() -> m.getDouble("PC1_1").map(Math::acos))
                    .or(() -> m.getDouble("CROTA1").map(Math::toRadians))
                    .or(() -> m.getDouble("CROTA2").map(Math::toRadians)).orElse(0.);

            crota = (float) c;
            scrota = (float) Math.sin(crota);
            ccrota = (float) Math.cos(crota);
            return Quat.rotate(Quat.createRotation(-c, Vec3.ZAxis), viewpoint.toQuat());
        }
        return viewpoint.toQuat();
    }

    private void retrievePixelParameters(MetaDataContainer m) {
        if (m.getLong("ZNAXIS").isPresent()) {
            pixelW = (int) m.getRequiredLong("ZNAXIS1");
            pixelH = (int) m.getRequiredLong("ZNAXIS2");
        } else {
            pixelW = (int) m.getRequiredLong("NAXIS1");
            pixelH = (int) m.getRequiredLong("NAXIS2");
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

            if (instrument.equals("XRT") || instrument.equals("Euhforia")) { // until CRVALx of all datasets can be tested
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

        if (measurement.toLowerCase().contains("continuum"))
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

    @Nonnull
    @Override
    public Region roiToRegion(int roiX, int roiY, int roiWidth, int roiHeight, double factorX, double factorY) {
        return new Region(roiX * factorX * unitPerPixelX - sunPositionX, roiY * factorY * unitPerPixelY - sunPositionY,
                roiWidth * factorX * unitPerPixelX, roiHeight * factorY * unitPerPixelY);
    }

    @Nonnull
    public String getDetector() {
        return detector;
    }

    @Nonnull
    public String getInstrument() {
        return instrument;
    }

    @Nonnull
    public String getMeasurement() {
        return measurement;
    }

    @Nonnull
    public String getObservatory() {
        return observatory;
    }

}
