package org.helioviewer.jhv.timelines.radio;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.io.NetFileCache;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.source.J2KSource;
import org.helioviewer.jhv.source.ResolutionSet;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.TimeAxis;

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
            radioCache(Path.of(arguments[2]), source, size);
        } finally {
            source.close();
        }
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    @SuppressWarnings("unchecked")
    private static void radioCache(Path file, J2KSource source, ResolutionSet.Level size) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        RadioData[] holder = {null};
        EventQueue.invokeAndWait(() -> holder[0] = new RadioData(null));
        RadioData owner = holder[0];
        Map<Long, RadioJ2KData> days = (Map<Long, RadioJ2KData>) field(owner, "cache");
        long date = TimeUtils.parse(new XMLMetaDataContainer(source.xml(0)).getRequiredString("DATE-OBS"));
        APIRequest request = new APIRequest("ROB", APIRequest.CallistoID, date, date, APIRequest.CADENCE_ALL);
        DataUri uri = NetFileCache.get(file.toUri());
        RadioJ2KData[] data = new RadioJ2KData[7];
        RadioJ2KData.Crop whole = new RadioJ2KData.Crop(0, 0, size.width(), size.height());
        RadioJ2KData.Crop edge = new RadioJ2KData.Crop(0, 32, size.width() - 32, size.height());
        RadioJ2KData.Crop newer = new RadioJ2KData.Crop(0, 64, size.width() - 64, size.height());
        byte[] full = source.decode(0, 0, 0, 0, whole.width(), whole.height());
        byte[] edgePixels = source.decode(0, 0, edge.x(), 0, edge.width(), edge.height());
        byte[] newerPixels = source.decode(0, 0, newer.x(), 0, newer.width(), newer.height());
        RadioJ2KData.Crop initial = RadioJ2KData.levelCrop(size.width() * 15 / 32, size.width() * 3 / 32, size.width(), source.level(0, 0));
        byte[] initialPixels = source.decode(0, 0, initial.x(), 0, initial.width(), initial.height());
        try {
            for (int i = 0; i < data.length; i++) {
                RadioJ2KData day = data[i] = new RadioJ2KData(owner, request, uri);
                long key = date + i * TimeUtils.DAY_IN_MILLIS;
                EventQueue.invokeAndWait(() -> days.put(key, day));
            }
            EventQueue.invokeAndWait(() -> {
                data[0].requestData(new TimeAxis(date + 2 * TimeUtils.DAY_IN_MILLIS, date + 3 * TimeUtils.DAY_IN_MILLIS));
                if (data[0].isLoading() || data[0].hasData())
                    throw new AssertionError("Offscreen day started decoding or reported loading");
                // Reuse immutable fixture arrays across days to exercise the byte budget without allocating 438 MiB.
                for (RadioJ2KData day : data) {
                    owner.putDecoded(day, whole, full);
                    owner.putDecoded(day, edge, edgePixels);
                }
                for (int repeat = 0; repeat < 100; repeat++)
                    owner.getDecoded(data[0], whole);
                for (RadioJ2KData day : data) {
                    if (day != data[0])
                        owner.getDecoded(day, whole);
                    owner.getDecoded(day, edge);
                }
                owner.putDecoded(data[6], newer, newerPixels);
                if (owner.getDecoded(data[0], whole) != null || owner.getDecoded(data[6], newer) != newerPixels
                        || owner.getDecoded(data[1], whole) != full || days.size() != 7)
                    throw new AssertionError("Radio LRU did not share its byte budget across retained days");
                owner.removeDecoded(data[0]);
                owner.putDecoded(data[0], initial, initialPixels);
                long noon = date + TimeUtils.DAY_IN_MILLIS / 2;
                data[0].requestData(new TimeAxis(noon, noon + TimeUtils.DAY_IN_MILLIS / 32));
                try {
                    BufferedImage before = (BufferedImage) field(data[0], "bufferedImage");
                    if (before == null || ((DataBufferByte) before.getRaster().getDataBuffer()).getData() != initialPixels)
                        throw new AssertionError("First radio request did not reuse the padded crop's scalar pixels");
                    Object executor = field(data[0], "executor");
                    int generation = (int) field(executor, "generation");
                    data[0].requestData(new TimeAxis(noon + TimeUtils.MINUTE_IN_MILLIS, noon + TimeUtils.DAY_IN_MILLIS / 32));
                    if ((int) field(executor, "generation") != generation
                            || field(data[0], "bufferedImage") != before || data[0].isLoading())
                        throw new AssertionError("Panning inside the padded crop requested another decode");
                    owner.removeDecoded(data[0]);
                    Method setLUT = RadioData.class.getDeclaredMethod("setLUT", LUT.class);
                    setLUT.setAccessible(true);
                    setLUT.invoke(owner, LUT.gray());
                    BufferedImage after = (BufferedImage) field(data[0], "bufferedImage");
                    int sample = initialPixels[0] & 0xff;
                    if (after.getRaster() != before.getRaster() || after.getRGB(0, 0) != (0xff000000 | sample << 16 | sample << 8 | sample))
                        throw new AssertionError("Eviction or LUT change lost the displayed scalar raster");
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
                days.remove(date);
                data[0].removeData();
                owner.putDecoded(data[0], whole, full);
                if (owner.getDecoded(data[0], whole) != null
                        || owner.getDecoded(data[1], whole) != full)
                    throw new AssertionError("Removed day accepted late pixels or purged another day's crops");
            });
        } finally {
            EventQueue.invokeAndWait(owner::remove);
        }
        if (!days.isEmpty() || owner.getDecoded(data[1], whole) != null)
            throw new AssertionError("Radio removal retained days or crops");
        System.out.println("PASS: padded first crop, offscreen days, shared radio LRU, day isolation, late-result refusal and scalar/LUT retention after eviction");
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
