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
