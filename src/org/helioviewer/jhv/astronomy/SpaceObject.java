package org.helioviewer.jhv.astronomy;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.border.Border;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;

import com.google.common.collect.ImmutableMap;

public class SpaceObject {

    private final String urlName;
    private final String name;
    private final double radius;
    private final byte[] color;
    private final Border border;

    public static final SpaceObject Sol = new SpaceObject("SUN", "Sun", 1, Colors.Yellow, JHVTableCellRenderer.cellEmphasisBorder);

    private static final ImmutableMap<String, SpaceObject> objectMap = new ImmutableMap.Builder<String, SpaceObject>().
            put("Sun", Sol).
            put("Mercury", new SpaceObject("Mercury", "Mercury", 2439700 / Sun.RadiusMeter, Colors.Gray, JHVTableCellRenderer.cellBorder)).
            put("Venus", new SpaceObject("Venus", "Venus", 6051800 / Sun.RadiusMeter, Colors.bytes(181, 110, 26), JHVTableCellRenderer.cellBorder)).
            put("Earth", new SpaceObject("Earth", "Earth", 6371000 / Sun.RadiusMeter, Colors.Blue, JHVTableCellRenderer.cellBorder)).
            put("Moon", new SpaceObject("Moon", "Moon", 1737400 / Sun.RadiusMeter, Colors.LightGray, JHVTableCellRenderer.cellBorder)).
            put("Mars", new SpaceObject("Mars%20Barycenter", "Mars", 3389500 / Sun.RadiusMeter, Colors.bytes(135, 37, 18), JHVTableCellRenderer.cellBorder)).
            put("Jupiter", new SpaceObject("Jupiter%20Barycenter", "Jupiter", 69911000 / Sun.RadiusMeter, Colors.bytes(168, 172, 180), JHVTableCellRenderer.cellBorder)).
            put("Saturn", new SpaceObject("Saturn%20Barycenter", "Saturn", 58232000 / Sun.RadiusMeter, Colors.bytes(208, 198, 173), JHVTableCellRenderer.cellBorder)).
            put("Uranus", new SpaceObject("Uranus%20Barycenter", "Uranus", 25362000 / Sun.RadiusMeter, Colors.bytes(201, 239, 242), JHVTableCellRenderer.cellBorder)).
            put("Neptune", new SpaceObject("Neptune%20Barycenter", "Neptune", 24622000 / Sun.RadiusMeter, Colors.bytes(124, 157, 226), JHVTableCellRenderer.cellBorder)).
            put("Pluto", new SpaceObject("Pluto%20Barycenter", "Pluto", 1195000 / Sun.RadiusMeter, Colors.bytes(205, 169, 140), JHVTableCellRenderer.cellEmphasisBorder)).

            put("Comet 67P", new SpaceObject("CHURYUMOV-GERASIMENKO", "Comet 67P", 2200 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellEmphasisBorder)).

            put("SOHO", new SpaceObject("SOHO", "SOHO", 2 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellBorder)).
            put("STEREO Ahead", new SpaceObject("STEREO%20Ahead", "STEREO Ahead", 2 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellBorder)).
            put("STEREO Behind", new SpaceObject("STEREO%20Behind", "STEREO Behind", 2 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellBorder)).
            put("PROBA-2", new SpaceObject("PROBA2", "PROBA-2", 2 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellBorder)).
            put("SDO", new SpaceObject("SDO", "SDO", 2 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellEmphasisBorder)).

            put("Solar Orbiter", new SpaceObject("Solar%20Orbiter", "Solar Orbiter", 2 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellBorder)).
            put("Solar Orbiter -5y", new SpaceObject("-999", "Solar Orbiter -5y", 2 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellBorder)).
            put("Parker Solar Probe", new SpaceObject("PSP", "Parker Solar Probe", 2 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellBorder)).
            // put("PROBA-3", new SpaceObject("PROBA3", "PROBA-3", 2 / Sun.RadiusMeter, Colors.White, JHVTableCellRenderer.cellBorder)).
            build();

    public static List<SpaceObject> getTargets(SpaceObject observer) {
        ArrayList<SpaceObject> list = new ArrayList<>(objectMap.values());
        list.remove(observer);
        return list;
    }

    @Nullable
    public static SpaceObject get(String obj) {
        return objectMap.get(obj);
    }

    private SpaceObject(String _urlName, String _name, double _radius, byte[] _color, Border _border) {
        urlName = _urlName;
        name = _name;
        radius = _radius;
        color = _color;
        border = _border;
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
        return name;
    }

}
