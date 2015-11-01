package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.math.Vec2d;
import org.helioviewer.jhv.base.math.Vec3d;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;

public class HelioviewerMetaData extends AbstractMetaData implements ObserverMetaData {

    private String instrument = "";
    private String detector = "";
    private String measurement = " ";
    private String observatory = " ";
    private String fullName = "";

    private Vec2d sunPixelPosition;

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
        if (detector.equalsIgnoreCase("C2")) {
            double maskRotation = -Math.toRadians(m.tryGetDouble("CROTA"));
            cutOffValue = (float) (-this.getPhysicalUpperLeft().x);
            cutOffDirection = new Vec3d(Math.sin(maskRotation) / 0.9625, Math.cos(maskRotation) / 0.9625, 0);
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
                if (wvD == 0) {
                    measurement = " ";
                } else {
                    measurement = String.valueOf(wvD);
                }
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
            dateObs = JHVDate.parseDateTime(observedDate);
    }

    private void retrievePosition(MetaDataContainer m) {
        Position.Latitudinal p = Sun.getEarth(dateObs.getTime());

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

        rotationObs = new Quatd(theta, phi);
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
            sunPixelPosition = new Vec2d(sunX, pixelHeight - 1 - sunY);

            setPhysicalLowerLeftCorner(new Vec2d(-unitPerPixel * sunX, -unitPerPixel * sunY));
            setPhysicalSize(new Vec2d(pixelWidth * unitPerPixel, pixelHeight * unitPerPixel));
        } else { // pixel based
            setPhysicalLowerLeftCorner(new Vec2d(0, 0));
            setPhysicalSize(new Vec2d(pixelWidth, pixelHeight));
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
