package org.helioviewer.jhv.metadata;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.math.Region;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.wcs.ImageBounds;
import org.helioviewer.jhv.wcs.ImageGeometry;
import org.helioviewer.jhv.wcs.WcsSolution;

import org.json.JSONArray;
import org.json.JSONObject;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;

public final class JHVMetadataDump {

    private static final class FitsMetaDataContainer implements MetaDataContainer {
        private final Header header;

        private FitsMetaDataContainer(Header header) {
            this.header = header;
        }

        @Override
        public Set<String> keys() {
            Set<String> keys = new HashSet<>();
            for (Cursor<String, HeaderCard> cursor = header.iterator(); cursor.hasNext(); )
                keys.add(cursor.next().getKey());
            return keys;
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

    private static JSONObject dumpMetadata(FitsMetaData meta, Header header) {
        int pixelWidth = (int) header.getLongValue("ZNAXIS1", header.getLongValue("NAXIS1"));
        int pixelHeight = (int) header.getLongValue("ZNAXIS2", header.getLongValue("NAXIS2"));

        ImageGeometry geometry = meta.getGeometry();
        WcsSolution wcs = geometry.wcs;
        double unitsPerArcsec = wcs.unitsPerArcsec();
        double arcsecPerPixelY = wcs.projection == WcsSolution.Projection.CEA
                ? geometry.pixelSizeY
                : geometry.pixelSizeY / unitsPerArcsec;
        Region hpcBounds = ImageBounds.hpc(geometry, meta.viewpoint.distance);
        // The validator's rectangle follows rows: its lower Y edge is the negated upper plane edge.
        Region sized = geometry.withSize(pixelWidth, pixelHeight).bounds;
        Region renderRegion = new Region(sized.llx, -sized.ury, sized.width, sized.height);
        Vec2 sunShift = new Vec2((geometry.sunPixel.x - geometry.referencePixel.x) * geometry.pixelSizeX,
                (geometry.sunPixel.y - geometry.referencePixel.y) * geometry.pixelSizeY);

        return new JSONObject()
                .put("arcsec_per_pixel_x", geometry.pixelSizeX / unitsPerArcsec)
                .put("arcsec_per_pixel_y", arcsecPerPixelY)
                .put("unit_per_arcsec", unitsPerArcsec)
                .put("unit_per_pixel_x", geometry.pixelSizeX)
                .put("unit_per_pixel_y", geometry.pixelSizeY)
                .put("plane_units_per_rad", wcs.unitsPerRad)
                .put("crval_internal_x", wcs.reference.x * wcs.unitsPerRad)
                .put("crval_internal_y", (wcs.projection == WcsSolution.Projection.CEA
                        ? Math.sin(wcs.reference.y) / wcs.pv2[1] : wcs.reference.y) * wcs.unitsPerRad)
                .put("native_basis", new JSONArray(wcs.nativeBasis))
                .put("native_offset", new JSONArray(new double[]{wcs.offset.x, wcs.offset.y}))
                .put("image_to_plane", new JSONArray(new double[]{
                        geometry.pixelToPlane.m00 / geometry.pixelSizeX,
                        -geometry.pixelToPlane.m01 / geometry.pixelSizeY,
                        geometry.pixelToPlane.m10 / geometry.pixelSizeX,
                        -geometry.pixelToPlane.m11 / geometry.pixelSizeY}))
                .put("render_rect", new JSONArray(new double[]{
                        renderRegion.llx,
                        renderRegion.lly,
                        1 / renderRegion.width,
                        1 / renderRegion.height}))
                .put("observer_distance", meta.viewpoint.distance)
                .put("hpc_min_x", hpcBounds.llx)
                .put("hpc_max_x", hpcBounds.urx)
                .put("hpc_min_y", hpcBounds.lly)
                .put("hpc_max_y", hpcBounds.ury)
                .put("radial_bound", ImageBounds.radial(geometry, meta.viewpoint.distance))
                .put("sun_shift_x", sunShift.x)
                .put("sun_shift_y", sunShift.y)
                .put("projection", wcs.projection.name())
                .put("zpn_upper_eta", wcs.nativeBasis[11])
                .put("pv2", new JSONArray(wcs.pv2));
    }

    private static void initSpice() throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 3 && args[0].equals("--prepare")) {
            JSONArray inputs = new JSONArray(Files.readString(Path.of(args[1])));
            JSONArray output = new JSONArray();
            for (int i = 0; i < inputs.length(); i++) {
                JSONObject input = inputs.getJSONObject(i);
                String cards = input.getString("header");
                String[] records = new String[cards.length() / 80];
                for (int j = 0; j < records.length; j++)
                    records[j] = cards.substring(j * 80, (j + 1) * 80);
                Header header = new Header(records);
                WcsSolution wcs = WcsInterpreter.read(new FitsMetaDataContainer(header),
                        input.getInt("width"), input.getInt("height"), input.getDouble("unitsPerRad"));
                wcs = wcs.adjusted(input.optDouble("deltaCRVAL1", 0), input.optDouble("deltaCRVAL2", 0));
                output.put(new JSONObject()
                        .put("nativeBasis", new JSONArray(wcs.nativeBasis))
                        .put("nativePoles", new JSONArray(wcs.nativePoles))
                        .put("reference", new JSONArray(new double[]{wcs.reference.x * wcs.unitsPerRad, wcs.reference.y * wcs.unitsPerRad}))
                        .put("nativeOffset", new JSONArray(new double[]{wcs.offset.x, wcs.offset.y}))
                        .put("pv2", new JSONArray(wcs.pv2)));
            }
            Files.writeString(Path.of(args[2]), output.toString());
            return;
        }
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
            FitsMetaData meta = new FitsMetaData(new FitsMetaDataContainer(header), file.toURI());
            System.out.println(dumpMetadata(meta, header).toString());
        }
    }
}
