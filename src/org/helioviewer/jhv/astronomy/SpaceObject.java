package org.helioviewer.jhv.astronomy;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Colors;

import com.google.common.collect.ImmutableMap;

public class SpaceObject {

    private final boolean internal;
    private final String spiceName;
    private final String uiName;
    private final double radius;
    private final byte[] color;
    private final byte[] whiteBackgroundColor;

    public static final SpaceObject SUN = new SpaceObject(true, "SUN", "Sun", 1, Colors.Yellow);
    public static final SpaceObject SOLO = new SpaceObject(true, "SOLO", "Solar Orbiter", 2 / Sun.RadiusMeter, Colors.bytes(226, 0, 79));
    public static final SpaceObject STA = new SpaceObject(true, "STEREO AHEAD", "STEREO Ahead", 2 / Sun.RadiusMeter, Colors.bytes(32, 32, 128));

    private static final ImmutableMap<String, SpaceObject> objectMap = new ImmutableMap.Builder<String, SpaceObject>()
            .put("Sun", SUN)
            .put("Mercury", new SpaceObject(true, "MERCURY", "Mercury", 2439700 / Sun.RadiusMeter, Colors.Gray))
            .put("Venus", new SpaceObject(true, "VENUS", "Venus", 6051800 / Sun.RadiusMeter, Colors.bytes(181, 110, 26)))
            .put("Earth", new SpaceObject(true, "EARTH", "Earth", 6371000 / Sun.RadiusMeter, Colors.Blue))
            //
            .put("Parker Solar Probe", new SpaceObject(false, "PSP", "Parker Solar Probe", 2 / Sun.RadiusMeter, Colors.bytes(235, 199, 31)))
            .put("Solar Orbiter", SOLO)
            .put("BepiColombo", new SpaceObject(false, "BEPICOLOMBO MPO", "BepiColombo", 2 / Sun.RadiusMeter, Colors.White))
            .put("STEREO Ahead", STA)
            .put("STEREO Behind", new SpaceObject(false, "STEREO BEHIND", "STEREO Behind", 2 / Sun.RadiusMeter, Colors.White))
            .put("SOHO", new SpaceObject(false, "SOHO", "SOHO", 2 / Sun.RadiusMeter, Colors.White))
            .put("SDO", new SpaceObject(false, "SDO", "SDO", 2 / Sun.RadiusMeter, Colors.White))
            .put("PROBA-2", new SpaceObject(false, "PROBA2", "PROBA-2", 2 / Sun.RadiusMeter, Colors.White))
            //
            .put("Moon", new SpaceObject(true, "MOON", "Moon", 1737400 / Sun.RadiusMeter, Colors.LightGray))
            .put("Mars", new SpaceObject(true, "MARS BARYCENTER", "Mars", 3389500 / Sun.RadiusMeter, Colors.bytes(135, 37, 18)))
            .put("Jupiter", new SpaceObject(true, "JUPITER BARYCENTER", "Jupiter", 69911000 / Sun.RadiusMeter, Colors.bytes(168, 172, 180)))
            .put("Saturn", new SpaceObject(true, "SATURN BARYCENTER", "Saturn", 58232000 / Sun.RadiusMeter, Colors.bytes(208, 198, 173)))
            .put("Uranus", new SpaceObject(true, "URANUS BARYCENTER", "Uranus", 25362000 / Sun.RadiusMeter, Colors.bytes(201, 239, 242)))
            .put("Neptune", new SpaceObject(true, "NEPTUNE BARYCENTER", "Neptune", 24622000 / Sun.RadiusMeter, Colors.bytes(124, 157, 226)))
            .put("Pluto", new SpaceObject(true, "PLUTO BARYCENTER", "Pluto", 1195000 / Sun.RadiusMeter, Colors.bytes(205, 169, 140)))
            //.put("Comet 67P", new SpaceObject(false, "CHURYUMOV-GERASIMENKO", "Comet 67P", 2200 / Sun.RadiusMeter, Colors.White))
            //.put("PROBA-3", new SpaceObject(false, "PROBA3", "PROBA-3", 2 / Sun.RadiusMeter, Colors.White))
            .build();

    public static List<SpaceObject> getTargets(SpaceObject observer) {
        List<SpaceObject> list = new ArrayList<>(objectMap.values());
        list.remove(observer);
        return list;
    }

    @Nullable
    public static SpaceObject get(String obj) {
        return objectMap.get(obj);
    }

    private SpaceObject(boolean _internal, String _spiceName, String _uiName, double _radius, byte[] _color) {
        internal = _internal;
        spiceName = _spiceName;
        uiName = _uiName;
        radius = _radius;
        color = _color;
        whiteBackgroundColor = readableOnWhite(_color);
    }

    private static final double WHITE_TRAJECTORY_MAX_LIGHTNESS = 0.70;

    private static byte[] readableOnWhite(byte[] color) {
        Oklab oklab = srgbToOklab(color);
        if (oklab.lightness() <= WHITE_TRAJECTORY_MAX_LIGHTNESS)
            return color;
        return oklabToSrgb(new Oklab(WHITE_TRAJECTORY_MAX_LIGHTNESS, oklab.a(), oklab.b()));
    }

    // https://bottosson.github.io/posts/oklab/
    private record Oklab(double lightness, double a, double b) {}

    private static Oklab srgbToOklab(byte[] color) {
        double red = srgbToLinear(color[0] & 0xff);
        double green = srgbToLinear(color[1] & 0xff);
        double blue = srgbToLinear(color[2] & 0xff);

        double l = Math.cbrt(0.4122214708 * red + 0.5363325363 * green + 0.0514459929 * blue);
        double m = Math.cbrt(0.2119034982 * red + 0.6806995451 * green + 0.1073969566 * blue);
        double s = Math.cbrt(0.0883024619 * red + 0.2817188376 * green + 0.6299787005 * blue);
        return new Oklab(
                0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
                1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
                0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s);
    }

    private static byte[] oklabToSrgb(Oklab oklab) {
        double l = oklab.lightness() + 0.3963377774 * oklab.a() + 0.2158037573 * oklab.b();
        double m = oklab.lightness() - 0.1055613458 * oklab.a() - 0.0638541728 * oklab.b();
        double s = oklab.lightness() - 0.0894841775 * oklab.a() - 1.2914855480 * oklab.b();

        l = l * l * l;
        m = m * m * m;
        s = s * s * s;

        return new byte[]{
                linearToSrgb(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s),
                linearToSrgb(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
                linearToSrgb(-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s),
                (byte) 255};
    }

    private static double srgbToLinear(int value) {
        double channel = value / 255.;
        return channel <= 0.04045 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    private static byte linearToSrgb(double value) {
        value = Math.clamp(value, 0., 1.);
        double channel = value <= 0.0031308 ? value * 12.92 : 1.055 * Math.pow(value, 1. / 2.4) - 0.055;
        return (byte) Math.round(255 * channel);
    }

    public boolean isInternal() {
        return internal;
    }

    public String getSpiceName() {
        return spiceName;
    }

    public double getRadius() {
        return radius;
    }

    public byte[] getColor() {
        return color;
    }

    public byte[] getTrajectoryColor(boolean whiteBackground) {
        return whiteBackground ? whiteBackgroundColor : color;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof SpaceObject s)
            return spiceName.equals(s.spiceName);
        return false;
    }

    @Override
    public int hashCode() {
        return spiceName.hashCode();
    }

    @Override
    public String toString() {
        return uiName;
    }
}
