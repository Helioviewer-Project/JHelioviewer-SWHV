package org.helioviewer.jhv.wcs;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;

import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.math.Mat2;
import org.helioviewer.jhv.math.Vec2;

import org.lwjgl.system.LibraryResource;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.util.Cursor;

// Prepared WCSLIB solution. Original and decoded geometries share its native owner.
// Exported arrays are read-only by convention; the last nonzero CRVAL adjustment is cached.
@SuppressWarnings("restricted")
public final class WcsSolution {
    public enum Projection {
        TAN, ARC, AZP, ZPN, CAR, CEA;

        public boolean isSurfaceMap() {
            return this == CAR || this == CEA;
        }
    }

    // ABI v1 byte offsets in jhv_wcs_info (native/wcs/jhv_wcs.h). Keep both layouts in sync.
    private static final int INFO_VERSION = 1;
    private static final int INFO_BYTES = 264;
    private static final int OFFSET_VERSION = 0;
    private static final int OFFSET_PROJECTION = 4;
    private static final int OFFSET_REFERENCE_PIXEL = 8;
    private static final int OFFSET_PIXEL_TO_PLANE = 24;
    private static final int OFFSET_REFERENCE = 56;
    private static final int OFFSET_NATIVE_BASIS = 72;
    private static final int OFFSET_PLANE_OFFSET = 168;
    private static final int OFFSET_PV = 184;
    private static final int OFFSET_NATIVE_POLES = 232;
    private static final double ARCSEC_TO_RAD = Math.PI / (180 * 3600);
    private static final MethodHandle create;
    private static final MethodHandle adjust;
    private static final MethodHandle destroy;
    private static final MethodHandle transform;

    static {
        if (Platform.getResourceDir() == null)
            Platform.init();
        Path path = LibraryResource.load(WcsSolution.class, "jhv",
                Platform.getResourceDir().substring(1) + System.mapLibraryName("jhvwcs"));
        SymbolLookup library = SymbolLookup.libraryLookup(path, Arena.global());
        create = bind(library, "create", FunctionDescriptor.of(ValueLayout.ADDRESS,
                ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        adjust = bind(library, "adjust", FunctionDescriptor.of(ValueLayout.ADDRESS,
                ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE,
                ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        destroy = bind(library, "destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        transform = bind(library, "transform", FunctionDescriptor.of(ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
    }

    private static MethodHandle bind(SymbolLookup library, String name, FunctionDescriptor descriptor) {
        return Linker.nativeLinker().downcallHandle(library.find("jhv_wcs_" + name).orElseThrow(), descriptor);
    }

    public final Projection projection;
    public final double unitsPerRad;
    public final Vec2 reference; // Celestial angles, in radians.
    final Vec2 referencePixel; // FITS centers, Y up; ImageGeometry converts to buffer coordinates.
    final Mat2 pixelToPlane;
    public final Vec2 offset;
    public final float[] nativeBasis = new float[12];
    public final float[] nativePoles = new float[4];
    public final float[] pv2 = new float[6];
    private final MemorySegment handle;
    private volatile Adjustment adjustment;

    private record Adjustment(double longitude, double latitude, WcsSolution wcs) {}

    public WcsSolution(Header header, double _unitsPerRad) {
        this(header, null, 0, 0, _unitsPerRad);
    }

    // Centered geometries used for images without FITS metadata and local rendering tests.
    public WcsSolution(Projection _projection, float[] _pv2, double _unitsPerRad) {
        this(centeredHeader(_projection, _pv2), _unitsPerRad);
    }

    private WcsSolution(Header header, WcsSolution source, double longitude, double latitude, double _unitsPerRad) {
        unitsPerRad = _unitsPerRad;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(INFO_BYTES, 8);
            MemorySegment error = arena.allocate(512);
            MemorySegment raw;
            if (source == null) {
                StringBuilder cards = new StringBuilder();
                for (Cursor<String, HeaderCard> cursor = header.iterator(); cursor.hasNext(); )
                    cards.append(cursor.next().toString());
                MemorySegment input = arena.allocateFrom(cards.toString());
                raw = (MemorySegment) create.invokeExact(input, cards.length() / 80, info, (long) INFO_BYTES, error, 512L);
            } else {
                raw = (MemorySegment) adjust.invokeExact(source.handle, longitude, latitude, info, (long) INFO_BYTES, error, 512L);
            }
            if (raw.equals(MemorySegment.NULL))
                throw new IllegalArgumentException("WCSLIB: " + error.getString(0));
            handle = raw.reinterpret(0, Arena.ofAuto(), WcsSolution::destroy);
            if (info.get(ValueLayout.JAVA_INT, OFFSET_VERSION) != INFO_VERSION)
                throw new IllegalStateException("WCSLIB adapter ABI mismatch");
            projection = Projection.values()[info.get(ValueLayout.JAVA_INT, OFFSET_PROJECTION)];
            referencePixel = pair(info, OFFSET_REFERENCE_PIXEL, 1);
            pixelToPlane = new Mat2(value(info, OFFSET_PIXEL_TO_PLANE) * unitsPerRad,
                    value(info, OFFSET_PIXEL_TO_PLANE + Double.BYTES) * unitsPerRad,
                    value(info, OFFSET_PIXEL_TO_PLANE + 2 * Double.BYTES) * unitsPerRad,
                    value(info, OFFSET_PIXEL_TO_PLANE + 3 * Double.BYTES) * unitsPerRad);
            reference = pair(info, OFFSET_REFERENCE, 1);
            for (int i = 0; i < nativeBasis.length; i++)
                nativeBasis[i] = (float) value(info, OFFSET_NATIVE_BASIS + i * Double.BYTES);
            offset = pair(info, OFFSET_PLANE_OFFSET, unitsPerRad);
            for (int i = 0; i < pv2.length; i++)
                pv2[i] = (float) value(info, OFFSET_PV + i * Double.BYTES);
            for (int i = 0; i < nativePoles.length; i++)
                nativePoles[i] = (float) value(info, OFFSET_NATIVE_POLES + i * Double.BYTES);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("WCSLIB binding failed", e);
        }
    }

    private static double value(MemorySegment info, int byteOffset) {
        return info.get(ValueLayout.JAVA_DOUBLE, byteOffset);
    }

    private static Vec2 pair(MemorySegment info, int byteOffset, double scale) {
        return new Vec2(value(info, byteOffset) * scale, value(info, byteOffset + Double.BYTES) * scale);
    }

    private static void destroy(MemorySegment nativeHandle) {
        try {
            destroy.invokeExact(nativeHandle);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("WCSLIB cleanup failed", e);
        }
    }

    public double unitsPerArcsec() {
        return unitsPerRad * ARCSEC_TO_RAD;
    }

    // User offsets change CRVAL, not the projection-plane origin. Recompute only when
    // the requested adjustment changes, and let WCSLIB resolve pole defaults again.
    public WcsSolution adjusted(double longitudeArcsec, double latitudeArcsec) {
        if (longitudeArcsec == 0 && latitudeArcsec == 0)
            return this;
        Adjustment cached = adjustment;
        if (cached != null && cached.longitude == longitudeArcsec && cached.latitude == latitudeArcsec)
            return cached.wcs;
        WcsSolution result = new WcsSolution(null, this, longitudeArcsec * ARCSEC_TO_RAD, latitudeArcsec * ARCSEC_TO_RAD, unitsPerRad);
        adjustment = new Adjustment(longitudeArcsec, latitudeArcsec, result);
        return result;
    }

    // Internal plane units -> celestial radians. Invalid points remain NaN pairs.
    public double[] planeToWorld(double[] plane) {
        double[] radians = plane.clone();
        for (int i = 0; i < radians.length; i++)
            radians[i] /= unitsPerRad;
        return transform(radians, false);
    }

    public Vec2 worldToPlane(double longitude, double latitude) {
        double[] point = transform(new double[]{longitude, latitude}, true);
        return Double.isFinite(point[0]) && Double.isFinite(point[1])
                ? new Vec2(point[0] * unitsPerRad, point[1] * unitsPerRad) : null;
    }

    private double[] transform(double[] coordinates, boolean toPlane) {
        if (coordinates.length % 2 != 0)
            throw new IllegalArgumentException("WCS coordinates require pairs");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment points = arena.allocateFrom(ValueLayout.JAVA_DOUBLE, coordinates);
            int _ = (int) transform.invokeExact(handle, toPlane ? 1 : 0, coordinates.length / 2, points);
            return points.toArray(ValueLayout.JAVA_DOUBLE);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("WCSLIB conversion failed", e);
        }
    }

    private static Header centeredHeader(Projection projection, float[] pv) {
        try {
            Header header = new Header();
            header.addValue("NAXIS", 2, null);
            header.addValue("CTYPE1", "HPLN-" + projection, null);
            header.addValue("CTYPE2", "HPLT-" + projection, null);
            header.addValue("CUNIT1", "rad", null);
            header.addValue("CUNIT2", "rad", null);
            for (int i = 0; i < pv.length; i++)
                if (pv[i] != 0)
                    header.addValue("PV2_" + i, pv[i], null);
            return header;
        } catch (HeaderCardException e) {
            throw new IllegalArgumentException("Invalid WCS header", e);
        }
    }
}
