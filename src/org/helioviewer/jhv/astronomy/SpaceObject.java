package org.helioviewer.jhv.astronomy;

import java.awt.Color;
import java.util.Collection;
import java.util.LinkedHashMap;

import javax.annotation.Nullable;
import javax.swing.border.Border;

import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;

@SuppressWarnings("serial")
public class SpaceObject {

    private final String urlName;
    private final String name;
    private final double radius;
    private final float[] color;
    private final Border border;

    public static final SpaceObject Sol = new SpaceObject("SUN", "Sun", 1, new float[] { Color.YELLOW.getRed() / 255f, Color.YELLOW.getGreen() / 255f, Color.YELLOW.getBlue() / 255f, 1 }, JHVTableCellRenderer.cellEmphasisBorder);

    private static final LinkedHashMap<String, SpaceObject> objectMap = new LinkedHashMap<String, SpaceObject>() {
        {
            put("Mercury", new SpaceObject("Mercury", "Mercury", 2439700 / Sun.RadiusMeter, new float[] { Color.GRAY.getRed() / 255f, Color.GRAY.getGreen() / 255f, Color.GRAY.getBlue() / 255f, 1 }, JHVTableCellRenderer.cellBorder));
            put("Venus", new SpaceObject("Venus", "Venus", 6051800 / Sun.RadiusMeter, new float[] { 181 / 255f, 110 / 255f, 26 / 255f, 1 }, JHVTableCellRenderer.cellBorder));
            put("Earth", new SpaceObject("Earth", "Earth", 6371000 / Sun.RadiusMeter, new float[] { Color.BLUE.getRed() / 255f, Color.BLUE.getGreen() / 255f, Color.BLUE.getBlue() / 255f, 1 }, JHVTableCellRenderer.cellBorder));
            put("Moon", new SpaceObject("Moon", "Moon", 1737400 / Sun.RadiusMeter, new float[] { Color.LIGHT_GRAY.getRed() / 255f, Color.LIGHT_GRAY.getGreen() / 255f, Color.LIGHT_GRAY.getBlue() / 255f, 1 }, JHVTableCellRenderer.cellBorder));
            put("Mars", new SpaceObject("Mars%20Barycenter", "Mars", 3389500 / Sun.RadiusMeter, new float[] { 135 / 255f, 37 / 255f, 18 / 255f, 1 }, JHVTableCellRenderer.cellBorder));
            put("Jupiter", new SpaceObject("Jupiter%20Barycenter", "Jupiter", 69911000 / Sun.RadiusMeter, new float[] { 168 / 255f, 172 / 255f, 180 / 255f, 1 }, JHVTableCellRenderer.cellBorder));
            put("Saturn", new SpaceObject("Saturn%20Barycenter", "Saturn", 58232000 / Sun.RadiusMeter, new float[] { 208 / 255f, 198 / 255f, 173 / 255f, 1}, JHVTableCellRenderer.cellBorder));
            put("Uranus", new SpaceObject("Uranus%20Barycenter", "Uranus", 25362000 / Sun.RadiusMeter, new float[] { 201 / 255f, 239 / 255f, 242 / 225f, 1 }, JHVTableCellRenderer.cellBorder));
            put("Neptune", new SpaceObject("Neptune%20Barycenter", "Neptune", 24622000 / Sun.RadiusMeter, new float[] { 124 / 255f, 157 / 255f, 226 / 255f, 1 }, JHVTableCellRenderer.cellBorder));
            put("Pluto", new SpaceObject("Pluto%20Barycenter", "Pluto", 1195000 / Sun.RadiusMeter, new float[] { 205 / 255f, 169 / 255f, 140 / 255f, 1 }, JHVTableCellRenderer.cellEmphasisBorder));

            put("Comet 67P", new SpaceObject("CHURYUMOV-GERASIMENKO", "Comet 67P", 2200 / Sun.RadiusMeter, new float[] { Color.WHITE.getRed() / 255f, Color.WHITE.getGreen() / 255f, Color.WHITE.getBlue() / 255f, 1 }, JHVTableCellRenderer.cellEmphasisBorder));

            put("SOHO", new SpaceObject("SOHO", "SOHO", 2 / Sun.RadiusMeter, new float[] { Color.WHITE.getRed() / 255f, Color.WHITE.getGreen() / 255f, Color.WHITE.getBlue() / 255f, 1 }, JHVTableCellRenderer.cellBorder));
            put("STEREO Ahead", new SpaceObject("STEREO%20Ahead", "STEREO Ahead", 2 / Sun.RadiusMeter, new float[] { Color.WHITE.getRed() / 255f, Color.WHITE.getGreen() / 255f, Color.WHITE.getBlue() / 255f, 1 }, JHVTableCellRenderer.cellBorder));
            put("STEREO Behind", new SpaceObject("STEREO%20Behind", "STEREO Behind", 2 / Sun.RadiusMeter, new float[] { Color.WHITE.getRed() / 255f, Color.WHITE.getGreen() / 255f, Color.WHITE.getBlue() / 255f, 1 }, JHVTableCellRenderer.cellBorder));
            put("PROBA-2", new SpaceObject("PROBA2", "PROBA-2", 2 / Sun.RadiusMeter, new float[] { Color.WHITE.getRed() / 255f, Color.WHITE.getGreen() / 255f, Color.WHITE.getBlue() / 255f, 1 }, JHVTableCellRenderer.cellBorder));
            put("SDO", new SpaceObject("SDO", "SDO", 2 / Sun.RadiusMeter, new float[] { Color.WHITE.getRed() / 255f, Color.WHITE.getGreen() / 255f, Color.WHITE.getBlue() / 255f, 1 }, JHVTableCellRenderer.cellEmphasisBorder));

            put("Solar Orbiter", new SpaceObject("Solar%20Orbiter", "Solar Orbiter", 2 / Sun.RadiusMeter, new float[] { Color.WHITE.getRed() / 255f, Color.WHITE.getGreen() / 255f, Color.WHITE.getBlue() / 255f, 1 }, JHVTableCellRenderer.cellBorder));
            put("Parker Solar Probe", new SpaceObject("SPP", "Parker Solar Probe", 2 / Sun.RadiusMeter, new float[] { Color.WHITE.getRed() / 255f, Color.WHITE.getGreen() / 255f, Color.WHITE.getBlue() / 255f, 1 }, JHVTableCellRenderer.cellBorder));
            put("PROBA-3", new SpaceObject("PROBA3", "PROBA-3", 2 / Sun.RadiusMeter, new float[] { Color.WHITE.getRed() / 255f, Color.WHITE.getGreen() / 255f, Color.WHITE.getBlue() / 255f, 1 }, JHVTableCellRenderer.cellBorder));
        }
    };

    public static Collection<SpaceObject> getObjects() {
        return objectMap.values();
    }

    @Nullable
    public static SpaceObject get(String obj) {
        return objectMap.get(obj);
    }

    private SpaceObject(String _urlName, String _name, double _radius, float[] _color, Border _border) {
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

    public float[] getColor() {
        return color;
    }

    public Border getBorder() {
        return border;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SpaceObject))
            return false;
        SpaceObject r = (SpaceObject) o;
        return urlName.equals(r.urlName);
    }

    @Override
    public int hashCode() {
        return urlName.hashCode();
    }

}
