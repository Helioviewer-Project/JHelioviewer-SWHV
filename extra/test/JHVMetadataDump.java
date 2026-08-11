package org.helioviewer.jhv.metadata;

import java.io.File;
import java.util.Optional;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.wcs.ImageBounds;

import org.json.JSONArray;
import org.json.JSONObject;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;

public final class JHVMetadataDump {

    private static final class FITSMetaDataContainer implements MetaDataContainer {
        private final Header header;

        private FITSMetaDataContainer(Header header) {
            this.header = header;
        }

        @Override
        public Optional<String> getString(String key) {
            HeaderCard card = header.findCard(key);
            if (card == null)
                return Optional.empty();
            return Optional.ofNullable(card.getValue());
        }

        @Override
        public Optional<Long> getLong(String key) {
            if (!header.containsKey(key))
                return Optional.empty();
            try {
                return Optional.of(header.getLongValue(key));
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        @Override
        public Optional<Double> getDouble(String key) {
            if (!header.containsKey(key))
                return Optional.empty();
            try {
                return Optional.of(header.getDoubleValue(key));
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        @Override
        public String getRequiredString(String key) {
            return getString(key).orElseThrow(() -> new RuntimeException(key + " not found in metadata"));
        }

        @Override
        public long getRequiredLong(String key) {
            return getLong(key).orElseThrow(() -> new RuntimeException(key + " not found in metadata"));
        }

        @Override
        public double getRequiredDouble(String key) {
            return getDouble(key).orElseThrow(() -> new RuntimeException(key + " not found in metadata"));
        }
    }

    private JHVMetadataDump() {}

    private static ImageHDU findImageHdu(Fits fits, Integer requestedHdu) throws Exception {
        BasicHDU<?>[] hdus = fits.read();
        if (requestedHdu != null) {
            BasicHDU<?> hdu = hdus[requestedHdu];
            if (hdu instanceof CompressedImageHDU chdu)
                return chdu.asImageHDU();
            if (hdu instanceof ImageHDU ihdu && ihdu.getAxes() != null)
                return ihdu;
            throw new Exception("HDU " + requestedHdu + " is not an image HDU");
        }

        for (BasicHDU<?> hdu : hdus) {
            if (hdu instanceof CompressedImageHDU chdu)
                return chdu.asImageHDU();
        }
        for (BasicHDU<?> hdu : hdus) {
            if (hdu instanceof ImageHDU ihdu && ihdu.getAxes() != null)
                return ihdu;
        }
        throw new Exception("No image HDU found");
    }

    private static double crotaRad(FitsMetaData meta) {
        return 2.0 * Math.atan2(meta.crota.z, meta.crota.w);
    }

    private static JSONObject dumpMetadata(FitsMetaData meta, Header header) {
        int pixelWidth = (int) header.getLongValue("ZNAXIS1", header.getLongValue("NAXIS1"));
        int pixelHeight = (int) header.getLongValue("ZNAXIS2", header.getLongValue("NAXIS2"));
        double crpix1Gl = header.getDoubleValue("CRPIX1", (pixelWidth + 1) / 2.0) - 0.5;
        double crpix2Gl = header.getDoubleValue("CRPIX2", (pixelHeight + 1) / 2.0) - 0.5;

        JSONArray pv2 = new JSONArray();
        for (float value : meta.pv2)
            pv2.put(value);
        double arcsecPerPixelY = meta.wcsProjection == org.helioviewer.jhv.wcs.WcsHeader.Projection.CEA
                ? meta.unitPerPixelY
                : meta.unitPerPixelY / meta.unitPerArcsec;
        Region hpcBounds = ImageBounds.hpc(meta);
        Vec2 sunShift = meta.getSunShift();

        return new JSONObject()
                .put("pixel_width", pixelWidth)
                .put("pixel_height", pixelHeight)
                .put("crpix1_gl", crpix1Gl)
                .put("crpix2_gl", crpix2Gl)
                .put("arcsec_per_pixel_x", meta.unitPerPixelX / meta.unitPerArcsec)
                .put("arcsec_per_pixel_y", arcsecPerPixelY)
                .put("unit_per_arcsec", meta.unitPerArcsec)
                .put("unit_per_pixel_x", meta.unitPerPixelX)
                .put("unit_per_pixel_y", meta.unitPerPixelY)
                .put("plane_units_per_rad", meta.wcsPlaneUnitsPerRad)
                .put("crval_internal_x", meta.crval.x)
                .put("crval_internal_y", meta.crval.y)
                .put("crota_rad", crotaRad(meta))
                .put("observer_distance", meta.viewpoint.distance)
                .put("hpc_min_x", hpcBounds.llx)
                .put("hpc_max_x", hpcBounds.urx)
                .put("hpc_min_y", hpcBounds.lly)
                .put("hpc_max_y", hpcBounds.ury)
                .put("radial_bound", ImageBounds.radial(meta))
                .put("sun_shift_x", sunShift.x)
                .put("sun_shift_y", sunShift.y)
                .put("projection", meta.wcsProjection.name())
                .put("zpn_upper_eta", meta.getWcsHeader().zpnUpperEta)
                .put("pv2", pv2);
    }

    private static void initSpice() throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 && args.length != 3)
            throw new IllegalArgumentException("usage: JHVMetadataDump <fits> [--hdu <index>]");

        Integer requestedHdu = null;
        if (args.length == 3) {
            if (!"--hdu".equals(args[1]))
                throw new IllegalArgumentException("usage: JHVMetadataDump <fits> [--hdu <index>]");
            requestedHdu = Integer.parseInt(args[2]);
        }

        if (System.getProperty("user.timezone") == null)
            System.setProperty("user.timezone", "UTC");
        FitsFactory.setUseHierarch(true);
        FitsFactory.setLongStringsEnabled(true);
        initSpice();

        File file = new File(args[0]);
        try (Fits fits = new Fits(file)) {
            ImageHDU hdu = findImageHdu(fits, requestedHdu);
            Header header = hdu.getHeader();
            FitsMetaData meta = new FitsMetaData(new FITSMetaDataContainer(header), file.toURI());
            System.out.println(dumpMetadata(meta, header).toString());
        }
    }
}
