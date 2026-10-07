package org.helioviewer.jhv.timelines.radio;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.helioviewer.jhv.view.j2k.J2KSource;
import org.helioviewer.jhv.view.j2k.ResolutionSet;

// Arguments: Kakadu library, bridge library, Callisto JP2 file.
public final class CallistoTest {

    public static void main(String[] arguments) throws Exception {
        System.load(arguments[0]);
        System.load(arguments[1]);
        J2KSource source = new J2KSource(Path.of(arguments[2]));
        try {
            String xml = source.xml(0);
            if (xml == null || !xml.contains("STARTFRQ"))
                throw new AssertionError("Expected a Callisto fixture");
            ResolutionSet.Level size = source.level(0, 0);
            System.out.println("Callisto dimensions=" + size.width() + "x" + size.height());
            for (int level = 0; level <= 5; level++) {
                ResolutionSet.Level reduced = source.level(0, level);
                byte[] full = source.decode(0, reduced.level(), 0, 0, reduced.width(), reduced.height());
                if (full.length != reduced.width() * reduced.height())
                    throw new AssertionError("Expected indexed grayscale Callisto data");
                for (int x : new int[]{0, size.width() / 3 + 7, size.width() - 103}) {
                    RadioJ2KData.Crop crop = RadioJ2KData.levelCrop(x, size.width() / 5, size.width(), reduced);
                    compare(full, reduced, source.decode(0, crop.level(), crop.x(), 0, crop.width(), crop.height()), crop);
                }
                System.out.println("PASS: Callisto level=" + level + " origin, interior and right-edge crops sha256="
                        + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(full)));
            }
        } finally {
            source.close();
        }
    }

    private static void compare(byte[] full, ResolutionSet.Level reduced, byte[] cropped, RadioJ2KData.Crop crop) {
        int x = crop.x(), width = crop.width(), height = crop.height(), fullWidth = reduced.width();
        if (width <= 0 || x < 0 || x + width > fullWidth || height != reduced.height() || cropped.length != width * height)
            throw new AssertionError("Crop outside full image: " + crop);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (cropped[row * width + col] != full[row * fullWidth + x + col])
                    throw new AssertionError("Crop differs at " + col + "," + row + " in " + crop);
            }
        }
    }
}
