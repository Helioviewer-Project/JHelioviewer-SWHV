package org.helioviewer.jhv.metadata;

import java.util.Optional;

import org.helioviewer.jhv.math.Mat2;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.wcs.WcsHeader;

final class WcsInterpreter {

    private static final double ARCSEC_PER_RAD = 180. * 3600. / Math.PI;

    record Result(
            WcsHeader.Projection projection,
            float[] pv2,
            Vec2 crval,
            Mat2 imageToPlane,
            double unitPerPixelX,
            double unitPerPixelY,
            double unitsPerRad) {}

    private record AxisUnits(
            double arcsecX,
            double arcsecY) {}

    private record LinearTransform(
            Mat2 imageToPlane,
            double unitPerPixelX,
            double unitPerPixelY) {}

    private record MatrixKeywords(boolean present, Mat2 matrix) {}

    static Result read(MetaDataContainer m) {
        String ctype1 = m.getString("CTYPE1").orElse("");
        String ctype2 = m.getString("CTYPE2").orElse("");
        WcsHeader.Projection projection = WcsHeader.Projection.fromCtype(ctype1, ctype2);
        boolean isSurfaceMap = projection.isSurfaceMap();

        AxisUnits units = readAxisUnits(m, isSurfaceMap);
        double pv2_1 = m.getDouble("PV2_1").orElse(1.);
        float[] pv2 = readPv2(m, projection, pv2_1);
        LinearTransform transform = computeLinearTransform(m, units, projection);
        double crval1 = m.getDouble("CRVAL1").orElse(0.);
        double crval2 = m.getDouble("CRVAL2").orElse(0.);
        Vec2 crval;
        double unitsPerRad;

        if (isSurfaceMap) {
            boolean isCea = projection == WcsHeader.Projection.CEA;
            double longitude = crval1 * units.arcsecX / ARCSEC_PER_RAD;
            double latitude = crval2 * units.arcsecY / ARCSEC_PER_RAD;
            crval = new Vec2(longitude, isCea ? ceaLatitudeCoordinate(latitude, pv2_1) : latitude);
            unitsPerRad = 1;
        } else {
            crval = new Vec2(crval1 * units.arcsecX, crval2 * units.arcsecY);
            unitsPerRad = ARCSEC_PER_RAD;
        }

        return new Result(projection, pv2, crval, transform.imageToPlane, transform.unitPerPixelX, transform.unitPerPixelY, unitsPerRad);
    }

    private static MatrixKeywords readMatrix(MetaDataContainer m, String prefix, double diagonalDefault) {
        Optional<Double> m11 = m.getDouble(prefix + "1_1");
        Optional<Double> m12 = m.getDouble(prefix + "1_2");
        Optional<Double> m21 = m.getDouble(prefix + "2_1");
        Optional<Double> m22 = m.getDouble(prefix + "2_2");
        boolean present = m11.isPresent() || m12.isPresent() || m21.isPresent() || m22.isPresent();
        return new MatrixKeywords(present, new Mat2(
                m11.orElse(diagonalDefault), m12.orElse(0.),
                m21.orElse(0.), m22.orElse(diagonalDefault)));
    }

    private static AxisUnits readAxisUnits(MetaDataContainer m, boolean isSurfaceMap) {
        return new AxisUnits(
                readAngularAxisScaleArcsec(m, "CUNIT1", isSurfaceMap),
                readAngularAxisScaleArcsec(m, "CUNIT2", isSurfaceMap));
    }

    private static double readAngularAxisScaleArcsec(MetaDataContainer m, String cunitKey, boolean defaultDegrees) {
        return m.getString(cunitKey)
                .map(WcsInterpreter::arcsecPerUnit)
                .orElse(defaultDegrees ? 3600. : 1.);
    }

    private static double arcsecPerUnit(String unit) {
        return switch (unit.strip().toLowerCase()) {
            // Not FITS-standard, but written in the wild: IDL-produced synoptic maps emit
            // "degrees". Without these they reach default and the image is scaled by 1/3600.
            case "deg", "degree", "degrees" -> 3600.;
            case "arcmin" -> 60.;
            case "mas" -> .001;
            case "rad" -> 180. * 3600. / Math.PI;
            default -> 1.;
        };
    }

    private static float[] readPv2(MetaDataContainer m, WcsHeader.Projection projection, double pv2_1) {
        float[] pv2 = new float[6];
        if (!projection.usesPv2())
            return pv2;
        for (int i = 0; i < pv2.length; i++)
            pv2[i] = m.getDouble("PV2_" + i).map(Double::floatValue).orElse(0f);
        if (projection == WcsHeader.Projection.CEA) // Thompson (2006): CEA defaults PV2_1 to 1 when omitted.
            pv2[1] = (float) pv2_1;
        return pv2;
    }

    private static LinearTransform computeLinearTransform(
            MetaDataContainer m, AxisUnits units, WcsHeader.Projection projection) {
        // Surface-map X is angular longitude. Y is angular latitude for CAR and equal-area Y for CEA.
        boolean isSurfaceMap = projection.isSurfaceMap();
        double axis1Scale = units.arcsecX;
        double axis2Scale = units.arcsecY;
        if (isSurfaceMap) {
            axis1Scale /= ARCSEC_PER_RAD;
            // Historical normalized CEA maps omit CUNIT2 and store the second plane coordinate
            // directly as sin(latitude) / lambda. An explicit CUNIT2 selects the FITS angular
            // convention used by wcslib/Astropy, which JHV converts to the same internal coordinate.
            // This deliberately overrides the FITS default of degrees when CUNIT2 is absent.
            boolean normalizedCeaY = projection == WcsHeader.Projection.CEA && m.getString("CUNIT2").isEmpty();
            axis2Scale = normalizedCeaY ? 1 : axis2Scale / ARCSEC_PER_RAD;
        }

        // FITS WCS/wcslib gives PC precedence over CD when both are present.
        MatrixKeywords pc = readMatrix(m, "PC", 1.);
        if (pc.present)
            return normalizeRows(
                    pc.matrix,
                    m.getRequiredDouble("CDELT1") * axis1Scale,
                    m.getRequiredDouble("CDELT2") * axis2Scale);

        // Any CD card selects a zero-defaulted CD matrix and makes CDELT and CROTA irrelevant.
        MatrixKeywords cd = readMatrix(m, "CD", 0.);
        if (cd.present)
            return normalizeRows(cd.matrix, axis1Scale, axis2Scale);

        double unitPerPixelX = m.getRequiredDouble("CDELT1") * axis1Scale;
        double unitPerPixelY = m.getRequiredDouble("CDELT2") * axis2Scale;
        if (isSurfaceMap)
            return normalize(unitPerPixelX, 0, 0, unitPerPixelY);

        double crota = m.getDouble("CROTA").or(() -> m.getDouble("CROTA1")).or(() -> m.getDouble("CROTA2"))
                .map(Math::toRadians).orElse(0.);
        double c = Math.cos(crota);
        double s = Math.sin(crota);
        return normalize(
                unitPerPixelX * c,
                -unitPerPixelY * s,
                unitPerPixelX * s,
                unitPerPixelY * c);
    }

    private static LinearTransform normalizeRows(Mat2 matrix, double rowScaleX, double rowScaleY) {
        return normalize(
                rowScaleX * matrix.m00,
                rowScaleX * matrix.m01,
                rowScaleY * matrix.m10,
                rowScaleY * matrix.m11);
    }

    private static LinearTransform normalize(double m00, double m01, double m10, double m11) {
        double unitPerPixelX = Math.hypot(m00, m10);
        double unitPerPixelY = Math.hypot(m01, m11);
        double divisorX = unitPerPixelX == 0 ? 1 : unitPerPixelX;
        double divisorY = unitPerPixelY == 0 ? 1 : unitPerPixelY;
        return new LinearTransform(
                new Mat2(m00 / divisorX, m01 / divisorY, m10 / divisorX, m11 / divisorY),
                unitPerPixelX,
                unitPerPixelY);
    }

    private static double ceaLatitudeCoordinate(double latitude, double pv2_1) {
        // JHV stores the CEA second axis as the equal-area latitude coordinate y = sin(lat) / lambda.
        double lambda = Math.max(pv2_1, 1e-12);
        return Math.sin(latitude) / lambda;
    }

    private WcsInterpreter() {}
}
