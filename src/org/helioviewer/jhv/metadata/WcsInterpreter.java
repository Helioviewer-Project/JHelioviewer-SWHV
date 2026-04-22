package org.helioviewer.jhv.metadata;

import org.helioviewer.jhv.wcs.WcsHeader;

final class WcsInterpreter {

    record Result(
            WcsHeader.Projection projection,
            float[] pv2,
            double internalCrvalX,
            double internalCrvalY,
            double crotaRad,
            double unitPerPixelX,
            double unitPerPixelY,
            double arcsecPerPixelX,
            double arcsecPerPixelY) {}

    private record PixelAxes(
            double arcsecX,
            double arcsecY,
            double arcsecPerPixelX,
            double arcsecPerPixelY,
            double pc11,
            double pc12,
            double pc21,
            double pc22) {}

    private record SurfaceCd(
            double cd11,
            double cd12,
            double cd21,
            double cd22) {}

    private record WcsInput(
            double cdelt1,
            double cdelt2,
            double crval1,
            double crval2,
            double pv2_1,
            boolean hasPc,
            double pc11,
            double pc12,
            double pc21,
            double pc22) {}

    private WcsInterpreter() {}

    static Result read(MetaDataContainer m) {
        String ctype1 = m.getString("CTYPE1").orElse("");
        String ctype2 = m.getString("CTYPE2").orElse("");
        WcsHeader.Projection projection = WcsHeader.Projection.fromCtype(ctype1, ctype2);
        boolean isSurfaceMap = projection.isSurfaceMap();

        WcsInput wcs = readWcsInput(m);
        PixelAxes axes = computePixelAxes(wcs, m, isSurfaceMap);
        float[] pv2 = readPv2(m, wcs, projection);
        double crvalX;
        double crvalY;
        double crotaRad;
        double unitPerPixelX;
        double unitPerPixelY;

        if (isSurfaceMap) {
            boolean isCea = projection == WcsHeader.Projection.CEA;
            SurfaceCd surfaceCd = computeSurfaceCd(wcs, axes, isCea);
            crvalX = Math.toRadians(wcs.crval1);
            crvalY = isCea ? readCeaLatitudeY(wcs) : Math.toRadians(wcs.crval2);
            crotaRad = Math.atan2(surfaceCd.cd21, surfaceCd.cd11);
            unitPerPixelX = Math.hypot(surfaceCd.cd11, surfaceCd.cd21);
            unitPerPixelY = Math.hypot(surfaceCd.cd12, surfaceCd.cd22);
        } else {
            crvalX = wcs.crval1 * axes.arcsecX;
            crvalY = wcs.crval2 * axes.arcsecY;
            crotaRad = readObserverImageCrota(m, wcs, axes);
            unitPerPixelX = 0;
            unitPerPixelY = 0;
        }

        return new Result(
                projection,
                pv2,
                crvalX,
                crvalY,
                crotaRad,
                unitPerPixelX,
                unitPerPixelY,
                axes.arcsecPerPixelX,
                axes.arcsecPerPixelY);
    }

    private static WcsInput readWcsInput(MetaDataContainer m) {
        return new WcsInput(
                m.getRequiredDouble("CDELT1"),
                m.getRequiredDouble("CDELT2"),
                m.getDouble("CRVAL1").orElse(0.),
                m.getDouble("CRVAL2").orElse(0.),
                m.getDouble("PV2_1").orElse(1.),
                m.getDouble("PC1_1").isPresent() || m.getDouble("PC1_2").isPresent() ||
                        m.getDouble("PC2_1").isPresent() || m.getDouble("PC2_2").isPresent(),
                m.getDouble("PC1_1").orElse(1.),
                m.getDouble("PC1_2").orElse(0.),
                m.getDouble("PC2_1").orElse(0.),
                m.getDouble("PC2_2").orElse(1.));
    }

    private static PixelAxes computePixelAxes(WcsInput wcs, MetaDataContainer m, boolean isSurfaceMap) {
        double arcsecX = readAngularAxisScaleArcsec(m, "CUNIT1", isSurfaceMap);
        double arcsecY = readAngularAxisScaleArcsec(m, "CUNIT2", isSurfaceMap);
        return new PixelAxes(arcsecX, arcsecY, wcs.cdelt1 * arcsecX, wcs.cdelt2 * arcsecY, wcs.pc11, wcs.pc12, wcs.pc21, wcs.pc22);
    }

    private static double readAngularAxisScaleArcsec(MetaDataContainer m, String cunitKey, boolean defaultDegrees) {
        return m.getString(cunitKey)
                .map(u -> u.equalsIgnoreCase("deg") ? 3600. : 1.)
                .orElse(defaultDegrees ? 3600. : 1.);
    }

    private static float[] readPv2(MetaDataContainer m, WcsInput wcs, WcsHeader.Projection projection) {
        float[] pv2 = new float[6];
        for (int i = 0; i < pv2.length; i++)
            pv2[i] = m.getDouble("PV2_" + i).map(Double::floatValue).orElse(0f);
        if (projection == WcsHeader.Projection.CEA) // Thompson (2006): CEA defaults PV2_1 to 1 when omitted.
            pv2[1] = (float) wcs.pv2_1;
        return pv2;
    }

    private static SurfaceCd computeSurfaceCd(WcsInput wcs, PixelAxes axes, boolean isCea) {
        // Surface-map X is angular longitude. Y is angular latitude for CAR and equal-area Y for CEA.
        double cdelt1Rad = Math.toRadians(axes.arcsecPerPixelX / 3600.);
        double cdelt2Surface = isCea ? wcs.cdelt2 : Math.toRadians(axes.arcsecPerPixelY / 3600.);
        return new SurfaceCd(
                cdelt1Rad * axes.pc11,
                cdelt1Rad * axes.pc12,
                cdelt2Surface * axes.pc21,
                cdelt2Surface * axes.pc22);
    }

    private static double readCeaLatitudeY(WcsInput wcs) {
        // JHV stores the CEA second axis as the equal-area latitude coordinate y = sin(lat) / lambda.
        double latitude = Math.toRadians(wcs.crval2);
        double lambda = Math.max(wcs.pv2_1, 1e-12);
        return Math.sin(latitude) / lambda;
    }

    private static double readObserverImageCrota(MetaDataContainer m, WcsInput wcs, PixelAxes axes) {
        if (wcs.hasPc) {
            return Math.atan2(axes.pc21 * axes.arcsecPerPixelY / axes.arcsecPerPixelX, axes.pc11);
        }
        return m.getDouble("CROTA").map(Math::toRadians)
                .or(() -> m.getDouble("CROTA1").map(Math::toRadians))
                .or(() -> m.getDouble("CROTA2").map(Math::toRadians))
                .orElse(0.);
    }
}
