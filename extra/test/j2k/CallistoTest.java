package org.helioviewer.jhv.view.j2k;

import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;

public final class CallistoTest {

    // Keep rendering coordinates so cropped pixels can be located in the full decode.
    private static final MetaData metadata = (MetaData) Proxy.newProxyInstance(CallistoTest.class.getClassLoader(),
            new Class<?>[]{MetaData.class}, (proxy, method, values) -> {
                if (method.getName().equals("roiToRegion"))
                    return new Region((int) values[0], (int) values[1], (int) values[2], (int) values[3]);
                throw new AssertionError("Unexpected metadata call: " + method.getName());
            });

    public static void main(String[] arguments) throws Exception {
        System.load(arguments[0]);
        KakaduMessageSystem.startKduMessageSystem();
        J2KSource.Local source = new J2KSource.Local(arguments[1], true);
        try {
            source.open();
            String[] xml = new String[source.maxFrame() + 1];
            source.extractMetaData(xml);
            if (xml.length != 1 || !xml[0].contains("STARTFRQ"))
                throw new AssertionError("Expected a single-frame Callisto fixture");
            ResolutionSet resolution = source.resolutionSet(0);
            ResolutionSet.Level size = resolution.getLevel(0);
            if (resolution.numComps != 1)
                throw new AssertionError("Expected indexed grayscale Callisto data");
            source.close(); // Exercise the same per-decode reopen used by local JP2 views.
            System.out.println("Callisto dimensions=" + size.width() + "x" + size.height());
            for (int level = 0; level <= 5; level++) {
                ResolutionSet.Level reduced = resolution.getLevel(level);
                DecodedImage full = decode(source, resolution, reduced.subImage(), reduced.level());
                MessageDigest hash = MessageDigest.getInstance("SHA-256");
                hash.update(((ByteBuffer) full.imageBuffer().buffer).duplicate());
                for (int x : new int[]{0, size.width() / 3 + 7, size.width() - 103}) {
                    J2KParams.SubImage region = J2KViewCallisto.levelRegion(x, 0, size.width() / 5, size.height(), size, reduced);
                    DecodedImage cropped = decode(source, resolution, region, reduced.level());
                    compare(full, cropped);
                }
                System.out.println("PASS: Callisto level=" + level + " origin, interior and right-edge crops sha256="
                        + HexFormat.of().formatHex(hash.digest()));
            }
        } finally {
            source.destroy();
        }
    }

    private static DecodedImage decode(J2KSource source, ResolutionSet resolution, J2KParams.SubImage region,
                                       int level) throws Exception {
        return new J2KDecoder(source, new J2KParams.Decode(0, region, level), resolution.numComps,
                ImageFilter.Type.None, metadata, 1, 1).call();
    }

    private static void compare(DecodedImage full, DecodedImage cropped) {
        int x = (int) (cropped.region().llx - full.region().llx);
        int y = (int) (cropped.region().lly - full.region().lly);
        int width = cropped.imageBuffer().width;
        int height = cropped.imageBuffer().height;
        int fullWidth = full.imageBuffer().width;
        if (width <= 0 || height <= 0 || x < 0 || y < 0 || x + width > fullWidth
                || y + height > full.imageBuffer().height)
            throw new AssertionError("Crop outside full image: " + cropped.region());
        ByteBuffer reference = (ByteBuffer) full.imageBuffer().buffer;
        ByteBuffer pixels = (ByteBuffer) cropped.imageBuffer().buffer;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (pixels.get(row * width + col) != reference.get((y + row) * fullWidth + x + col))
                    throw new AssertionError("Crop differs at " + col + "," + row + " in " + cropped.region());
            }
        }
    }
}
