package org.helioviewer.jhv.astronomy;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.border.Border;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;

import com.google.common.collect.ImmutableMap;

public class SpaceObject {

    private final boolean internal;
    private final String spiceName;
    private final String urlName;
    private final String uiName;
    private final double radius;
    private final byte[] color;
    private final Border border;

    public static final SpaceObject Sol = new SpaceObject(true, "SUN", "Sun", 1, Colors.Yellow, JHVTableCellRenderer.cellEmphasisBorder);

    private static final ImmutableMap<String, SpaceObject> objectMap = new ImmutableMap.Builder<String, SpaceObject>()
            .put("Sun", Sol)
            .put("Mercury", new SpaceObject(true, "MERCURY", "Mercury", 2439700 / Sun.RadiusMeter, Colors.Gray, JHVTableCellRenderer.cellBorder))
            .put("Venus", new SpaceObject(true, "VENUS", "Venus", 6051800 / Sun.RadiusMeter, Colors.bytes(181, 110, 26), JHVTableCellRenderer.cellBorder))
            .put("Earth", new SpaceObject(true, "EARTH", "Earth", 6371000 / Sun.RadiusMeter, Colors.Blue, JHVTableCellRenderer.cellEmphasisBorder))
            //
            .put("Parker Solar Probe", new SpaceObject(false, "PSP", "Parker Solar Probe", 2 / Sun.RadiusMeter, Colors.bytes(235, 199, 31), JHVTableCellRenderer.cellBorder))
            .put("Solar Orbiter", new SpaceObject(false, "SOLO", "Solar Orbiter", 2 / Sun.RadiusMeter, Colors.bytes(226, 0, 79), JHVTableCellRenderer.cellBorder))
            .put("Solar Orbiter -5y", new SpaceObject(false, "-999", "Solar Orbiter -5y", 2 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellBorder))
            .put("STEREO Ahead", new SpaceObject(false, "STEREO AHEAD", "STEREO Ahead", 2 / Sun.RadiusMeter, Colors.bytes(32, 32, 128), JHVTableCellRenderer.cellBorder))
            .put("STEREO Behind", new SpaceObject(false, "STEREO BEHIND", "STEREO Behind", 2 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellBorder))
            .put("SOHO", new SpaceObject(false, "SOHO", "SOHO", 2 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellBorder))
            .put("SDO", new SpaceObject(false, "SDO", "SDO", 2 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellBorder))
            .put("PROBA-2", new SpaceObject(false, "PROBA2", "PROBA-2", 2 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellEmphasisBorder))
            //
            .put("Moon", new SpaceObject(true, "MOON", "Moon", 1737400 / Sun.RadiusMeter, Colors.LightGray, JHVTableCellRenderer.cellBorder))
            .put("Mars", new SpaceObject(true, "MARS BARYCENTER", "Mars", 3389500 / Sun.RadiusMeter, Colors.bytes(135, 37, 18), JHVTableCellRenderer.cellBorder))
            .put("Jupiter", new SpaceObject(true, "JUPITER BARYCENTER", "Jupiter", 69911000 / Sun.RadiusMeter, Colors.bytes(168, 172, 180), JHVTableCellRenderer.cellBorder))
            .put("Saturn", new SpaceObject(true, "SATURN BARYCENTER", "Saturn", 58232000 / Sun.RadiusMeter, Colors.bytes(208, 198, 173), JHVTableCellRenderer.cellBorder))
            .put("Uranus", new SpaceObject(true, "URANUS BARYCENTER", "Uranus", 25362000 / Sun.RadiusMeter, Colors.bytes(201, 239, 242), JHVTableCellRenderer.cellBorder))
            .put("Neptune", new SpaceObject(true, "NEPTUNE BARYCENTER", "Neptune", 24622000 / Sun.RadiusMeter, Colors.bytes(124, 157, 226), JHVTableCellRenderer.cellBorder))
            .put("Pluto", new SpaceObject(true, "PLUTO BARYCENTER", "Pluto", 1195000 / Sun.RadiusMeter, Colors.bytes(205, 169, 140), JHVTableCellRenderer.cellBorder))
            //.put("Comet 67P", new SpaceObject(false, "CHURYUMOV-GERASIMENKO", "Comet 67P", 2200 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellBorder))
            //.put("PROBA-3", new SpaceObject(false, "PROBA3", "PROBA-3", 2 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellBorder))
            .build();

    public static List<SpaceObject> getTargets(SpaceObject observer) {
        ArrayList<SpaceObject> list = new ArrayList<>(objectMap.values());
        list.remove(observer);
        return list;
    }

    @Nullable
    public static SpaceObject get(String obj) {
        return objectMap.get(obj);
    }

    private SpaceObject(boolean _internal, String _spiceName, String _uiName, double _radius, byte[] _color, Border _border) {
        internal = _internal;
        spiceName = _spiceName;
        urlName = URLEncoder.encode(spiceName, StandardCharsets.UTF_8);
        uiName = _uiName;
        radius = _radius;
        color = _color;
        border = _border;
    }

    public boolean isInternal() {
        return internal;
    }

    public String getSpiceName() {
        return spiceName;
    }

    public String getUrlName() {
        return urlName;
    }

    public double getRadius() {
        return radius;
    }

    public byte[] getColor() {
        return color;
    }

    public Border getBorder() {
        return border;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SpaceObject))
            return false;
        SpaceObject r = (SpaceObject) o;
        return urlName.equals(r.urlName);
    }

    @Override
    public int hashCode() {
        return urlName.hashCode();
    }

    @Override
    public String toString() {
        return uiName;
    }

}
