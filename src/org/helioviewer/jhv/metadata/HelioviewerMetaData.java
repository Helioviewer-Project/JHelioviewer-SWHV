package org.helioviewer.jhv.metadata;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeMode;

public class HelioviewerMetaData extends BaseMetaData {

    private static final boolean normalizeAIA = Boolean.parseBoolean(Settings.getProperty("display.normalizeAIA"));
    private static final boolean normalizeRadius = Boolean.parseBoolean(Settings.getProperty("display.normalize"));

    private static final TimeMode timeMode;

    static {
        TimeMode setTimeMode = TimeMode.Observer;
        try {
            setTimeMode = TimeMode.valueOf(Settings.getProperty("display.time"));
        } catch (Exception ignore) {
        }
        timeMode = setTimeMode;
    }

    private static final Set<String> SECCHIDetectors = Set.of("EUVI", "COR1", "COR2", "HI1", "HI2");
    private static final Set<String> CROTABlockSet = Set.of("LASCO");

    private static final Map<String, String> unitRepl = Map.of(
            " ", "",
            "-1", "\u207B\u00B9",
            "-2", "\u207B\u00B2",
            "-3", "\u207B\u00B3",
            "-5", "\u207B\u2075");

    private String instrument = "";
    private String detector = "";
    private String measurement = "";
    private String observatory = "";

    private double sunPositionX = 0;
    private double sunPositionY = 0;

    public HelioviewerMetaData(@Nonnull MetaDataContainer m) {
        identifyObservation(m);

        instrument = instrument.trim().intern();
        detector = detector.trim().intern();
        measurement = measurement.trim().intern();
        observatory = observatory.trim().intern();
        displayName = displayName.trim().intern();

        viewpoint = retrievePosition(m, retrieveTime(m));
        retrievePixelParameters(m);

        retrieveOcculterRadii(m);
        retrieveOcculterLinearCutOff(m);
        retrieveSector(m);

        retrieveUnit(m);
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
        // a linear physical LUT
        Optional<Double> mZero = m.getDouble("HV_ZERO");
        Optional<Double> mScale = m.getDouble("HV_SCALE");
        Optional<Double> mDataMax = m.getDouble("DATAMAX");
        if (mZero.isPresent() && mScale.isPresent() && mDataMax.isPresent()) {
            double zero = mZero.get();
            double scale = mScale.get();
            int size = MathUtils.clip((int) Math.ceil(mDataMax.get()) + 1, 0, 65535);
            physLUT = new float[size];
            for (int i = 0; i < size; i++)
                physLUT[i] = (float) (zero + i * scale);
        }

        // only if we have LUT
        if (physLUT != null) {
            unit = m.getString("BUNIT").orElseGet(() -> unit);
            for (Map.Entry<String, String> entry : unitRepl.entrySet())
                unit = unit.replace(entry.getKey(), entry.getValue());
            unit = unit.intern();
        }
    }

    private void retrieveResponse() {
        if (normalizeAIA && instrument.equals("AIA")) {
            responseFactor = (float) AIAResponse.get(viewpoint.time.milli, measurement);
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
                inner = 2.6 * Sun.Radius;
                outer = 15.2 * Sun.Radius;
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
        else if (instrument.equals("Metis")) {
            inner = viewpoint.distance * Math.atan(1.49 / 180 * Math.PI); // 1.6 official
            outer = viewpoint.distance * Math.atan(3.49 / 180 * Math.PI); // 3.4 official
        }

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
        if (measurement.endsWith(".0"))
            measurement = measurement.substring(0, measurement.length() - 2);

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
        } else if (SECCHIDetectors.contains(detector)) {
            observatory = m.getString("OBSRVTRY").orElse("").replace('_', '-');
            displayName = observatory + ' ' + detector;
            if (detector.equals("EUVI"))
                displayName += ' ' + measurement;
        } else if (instrument.equals("TRACE")) {
            measurement = m.getString("WAVE_LEN").orElse("");
            displayName = instrument + ' ' + measurement;
        } else if (instrument.equals("XRT")) {
            measurement = m.getString("EC_FW1_").orElse("") + ' ' + m.getString("EC_FW2_").orElse("");
            displayName = instrument + ' ' + measurement;
        } else if (instrument.startsWith("GOES-R Series Solar Ultraviolet Imager")) {
            instrument = "SUVI";
            displayName = instrument + ' ' + measurement;
        } else if (instrument.equals("COSMO K-Coronagraph")) {
            displayName = "COSMO KCor";
        } else if (instrument.equals("SJI")) {
            measurement = m.getDouble("TWAVE1").map(w -> String.valueOf(w.longValue())).orElse("");
            displayName = observatory + ' ' + instrument + ' ' + measurement;
        } else if (instrument.equals("EUI")) {
            displayName = instrument + ' ' + detector.replace('_', '-') + ' ' + measurement;
        } else if (instrument.equals("PHI")) {
            displayName = instrument + ' ' + detector; // TBD
        } else if (instrument.equals("Metis")) {
            measurement = m.getString("BTYPE").orElse("");
            displayName = instrument + ' ' + measurement; // TBD
        } else if (detector.equals("demregpy")) {
            displayName = "DEM " + instrument;
        } else {
            displayName = instrument + ' ' + measurement;
        }
    }

    private JHVTime retrieveTime(MetaDataContainer m) {
        String observedDate = m.getString("DATE-AVG").
                or(() -> m.getString("DATE_AVG")).
                or(() -> m.getString("DATE_OBS")). // try first, DATE-OBS unusable for MDI and early EIT
                        orElseGet(() -> m.getRequiredString("DATE-OBS"));
        if (instrument.equals("LASCO")) {
            String observedTime = m.getString("TIME_OBS").orElseGet(() -> m.getRequiredString("TIME-OBS"));
            observedDate = observedDate.replace('/', '-') + 'T' + observedTime;
        }
        if (observedDate.endsWith("Z")) // MDI & EIT
            observedDate = observedDate.substring(0, observedDate.length() - 1);

        return new JHVTime(observedDate);
    }

    private static JHVTime adjustTime(JHVTime dateObs, double distObs, double distEarth) {
        return switch (timeMode) {
            case Observer -> dateObs;
            case Sun -> new JHVTime((long) (dateObs.milli - distObs * Sun.RadiusMilli + .5));
            case Earth ->
                    new JHVTime((long) (dateObs.milli - (distObs - distEarth) * Sun.RadiusMilli + .5)); // shortcut, avoids inconsistent results for Earth based observers
        };
    }

    private static Position earthPosition(JHVTime dateObs, Position earth) {
        JHVTime time = adjustTime(dateObs, earth.distance, earth.distance);
        return new Position(time, earth.distance, earth.lon, earth.lat);
    }

    private Position retrievePosition(MetaDataContainer m, JHVTime dateObs) {
        Position earth = Sun.getEarth(dateObs);
        if (observatory.equals("SDO")) // SDO has slightly wrong position metadata, place it at Earth
            return earthPosition(dateObs, earth);

        double distObs = m.getDouble("DSUN_OBS").map(d -> d / Sun.RadiusMeter).orElseGet(() -> earth.distance);
        if (observatory.equals("SOHO"))
            distObs *= Sun.L1Factor;

        if (distObs < 1) // failure in metadata pipeline like SUVI L1b, place it at Earth
            return earthPosition(dateObs, earth);

        double lon = m.getDouble("HGLN_OBS").map(v -> earth.lon - Math.toRadians(v))
                .orElseGet(() -> m.getDouble("CRLN_OBS").map(v -> -Math.toRadians(v)).orElseGet(() -> earth.lon));
        double lat = m.getDouble("HGLT_OBS").map(Math::toRadians)
                .orElseGet(() -> m.getDouble("CRLT_OBS").map(Math::toRadians).orElseGet(() -> earth.lat));

        JHVTime time = adjustTime(dateObs, distObs, earth.distance);
        return new Position(time, distObs, lon, lat);
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
            double arcsecX = m.getString("CUNIT1").map(u -> u.equals("deg") ? 3600 : 1).orElse(1);
            double arcsecY = m.getString("CUNIT2").map(u -> u.equals("deg") ? 3600 : 1).orElse(1);
            double arcsecPerPixelX = m.getRequiredDouble("CDELT1") * arcsecX;
            double arcsecPerPixelY = m.getRequiredDouble("CDELT2") * arcsecY;

            double radiusSunInArcsec = Math.toDegrees(Math.atan2(Sun.Radius * getSolarRadiusFactor(), viewpoint.distance)) * 3600;
            unitPerArcsec = Sun.Radius / radiusSunInArcsec;

            unitPerPixelX = Math.abs(arcsecPerPixelX * unitPerArcsec);
            unitPerPixelY = Math.abs(arcsecPerPixelY * unitPerArcsec);

            // Pixel center: FITS = integer from 1, OpenGL = half-integer from 0
            double sunX = m.getDouble("CRPIX1").orElseGet(() -> (pixelW + 1) / 2.) - .5;
            double sunY = m.getDouble("CRPIX2").orElseGet(() -> (pixelH + 1) / 2.) - .5;
            sunPositionX = unitPerPixelX * sunX;
            sunPositionY = unitPerPixelY * (pixelH - sunY);

            region = new Region(-sunX * unitPerPixelX, -sunY * unitPerPixelY, pixelW * unitPerPixelX, pixelH * unitPerPixelY);

            crval.x = m.getDouble("CRVAL1").orElse(0.) * arcsecX / arcsecPerPixelX * unitPerPixelX;
            crval.y = m.getDouble("CRVAL2").orElse(0.) * arcsecY / arcsecPerPixelY * unitPerPixelY;

            if (!CROTABlockSet.contains(instrument)) {
                double c;
                try {
                    // Eq.32 Thompson (2006)
                    c = Math.atan2(m.getRequiredDouble("PC2_1") / (arcsecPerPixelX / arcsecPerPixelY), m.getRequiredDouble("PC1_1"));
                } catch (Exception e) {
                    c = m.getDouble("CROTA").map(Math::toRadians)
                            .or(() -> m.getDouble("CROTA1").map(Math::toRadians))
                            .or(() -> m.getDouble("CROTA2").map(Math::toRadians)).orElse(0.);
                }
                crota = Quat.createRotation(c, Vec3.ZAxis);
            }
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
