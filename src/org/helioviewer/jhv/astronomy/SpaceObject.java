package org.helioviewer.jhv.astronomy;

import java.awt.Color;
import java.util.Collection;
import java.util.LinkedHashMap;

import javax.swing.border.Border;

import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;

@SuppressWarnings("serial")
public class SpaceObject {

    private final String urlName;
    private final String name;
    private final double radius;
    private final Color color;
    private final Border border;

    private static LinkedHashMap<String, SpaceObject> objectMap = new LinkedHashMap<String, SpaceObject>() {
        {
            put("Mercury", new SpaceObject("Mercury", "Mercury", 2439700 / Sun.RadiusMeter, Color.GRAY, JHVTableCellRenderer.cellBorder));
            put("Venus", new SpaceObject("Venus", "Venus", 6051800 / Sun.RadiusMeter, new Color(181, 110, 26), JHVTableCellRenderer.cellBorder));
            put("Earth", new SpaceObject("Earth", "Earth", 6371000 / Sun.RadiusMeter, Color.BLUE, JHVTableCellRenderer.cellBorder));
            put("Moon", new SpaceObject("Moon", "Moon", 1737400 / Sun.RadiusMeter, Color.LIGHT_GRAY, JHVTableCellRenderer.cellBorder));
            put("Mars", new SpaceObject("Mars%20Barycenter", "Mars", 3389500 / Sun.RadiusMeter, new Color(135, 37, 18), JHVTableCellRenderer.cellBorder));
            put("Jupiter", new SpaceObject("Jupiter%20Barycenter", "Jupiter", 69911000 / Sun.RadiusMeter, new Color(168, 172, 180), JHVTableCellRenderer.cellBorder));
            put("Saturn", new SpaceObject("Saturn%20Barycenter", "Saturn", 58232000 / Sun.RadiusMeter, new Color(208, 198, 173), JHVTableCellRenderer.cellBorder));
            put("Uranus", new SpaceObject("Uranus%20Barycenter", "Uranus", 25362000 / Sun.RadiusMeter, new Color(201, 239, 242), JHVTableCellRenderer.cellBorder));
            put("Neptune", new SpaceObject("Neptune%20Barycenter", "Neptune", 24622000 / Sun.RadiusMeter, new Color(124, 157, 226), JHVTableCellRenderer.cellBorder));
            put("Pluto", new SpaceObject("Pluto%20Barycenter", "Pluto", 1195000 / Sun.RadiusMeter, new Color(205, 169, 140), JHVTableCellRenderer.cellEmphasisBorder));

            put("Comet 67P", new SpaceObject("CHURYUMOV-GERASIMENKO", "Comet 67P", 2200 / Sun.RadiusMeter, Color.WHITE, JHVTableCellRenderer.cellEmphasisBorder));

            put("SOHO", new SpaceObject("SOHO", "SOHO", 2 / Sun.RadiusMeter, Color.WHITE, JHVTableCellRenderer.cellBorder));
            put("STEREO Ahead", new SpaceObject("STEREO%20Ahead", "STEREO Ahead", 2 / Sun.RadiusMeter, Color.WHITE, JHVTableCellRenderer.cellBorder));
            put("STEREO Behind", new SpaceObject("STEREO%20Behind", "STEREO Behind", 2 / Sun.RadiusMeter, Color.WHITE, JHVTableCellRenderer.cellBorder));
            put("PROBA-2", new SpaceObject("PROBA2", "PROBA-2", 2 / Sun.RadiusMeter, Color.WHITE, JHVTableCellRenderer.cellBorder));
            put("SDO", new SpaceObject("SDO", "SDO", 2 / Sun.RadiusMeter, Color.WHITE, JHVTableCellRenderer.cellEmphasisBorder));

            put("Solar Orbiter", new SpaceObject("Solar%20Orbiter", "Solar Orbiter", 2 / Sun.RadiusMeter, Color.WHITE, JHVTableCellRenderer.cellBorder));
            put("Parker Solar Probe", new SpaceObject("SPP", "Parker Solar Probe", 2 / Sun.RadiusMeter, Color.WHITE, JHVTableCellRenderer.cellBorder));
            put("PROBA-3", new SpaceObject("PROBA3", "PROBA-3", 2 / Sun.RadiusMeter, Color.WHITE, JHVTableCellRenderer.cellBorder));
        }
    };

    public static Collection<SpaceObject> getObjectList() {
        return objectMap.values();
    }

    public static SpaceObject Earth = objectMap.get("Earth");

    private SpaceObject(String _urlName, String _name, double _radius, Color _color, Border _border) {
        urlName = _urlName;
        name = _name;
        radius = _radius;
        color = _color;
        border = _border;
    }

    public String getUrlName() {
        return urlName;
    }

    @Override
    public String toString() {
        return name;
    }

    public double getRadius() {
        return radius;
    }

    public Color getColor() {
        return color;
    }

    public Border getBorder() {
        return border;
    }

}
