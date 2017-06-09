package org.helioviewer.jhv.astronomy;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.border.Border;

import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;

public class SpaceObject {

    private final String urlName;
    private final String name;
    private final double radius;
    private final Color color;
    private final Border border;

    private static ArrayList<SpaceObject> objectList;
    public static SpaceObject Earth;

    public static SpaceObject[] getObjectArray() {
        if (objectList == null) {
            createObjectList();
        }
        return objectList.toArray(new SpaceObject[objectList.size()]);
    }

    private static void createObjectList() {
        objectList = new ArrayList<>();

        objectList.add(new SpaceObject("Mercury", "Mercury", 2439700 / Sun.RadiusMeter,
            Color.GRAY, JHVTableCellRenderer.cellBorder));
        objectList.add(new SpaceObject("Venus", "Venus", 6051800 / Sun.RadiusMeter,
            new Color(181, 110, 26), JHVTableCellRenderer.cellBorder));

        Earth = new SpaceObject("Earth", "Earth", 6371000 / Sun.RadiusMeter,
            Color.BLUE, JHVTableCellRenderer.cellBorder);
        objectList.add(Earth);

        objectList.add(new SpaceObject("Moon", "Moon", 1737400 / Sun.RadiusMeter,
            Color.LIGHT_GRAY, JHVTableCellRenderer.cellBorder));

        objectList.add(new SpaceObject("Mars%20Barycenter", "Mars", 3389500 / Sun.RadiusMeter,
            new Color(135, 37, 18), JHVTableCellRenderer.cellBorder));
        objectList.add(new SpaceObject("Jupiter%20Barycenter", "Jupiter", 69911000 / Sun.RadiusMeter,
            new Color(168, 172, 180), JHVTableCellRenderer.cellBorder));
        objectList.add(new SpaceObject("Saturn%20Barycenter", "Saturn", 58232000 / Sun.RadiusMeter,
            new Color(208, 198, 173), JHVTableCellRenderer.cellBorder));
        objectList.add(new SpaceObject("Uranus%20Barycenter", "Uranus", 25362000 / Sun.RadiusMeter,
            new Color(201, 239, 242), JHVTableCellRenderer.cellBorder));
        objectList.add(new SpaceObject("Neptune%20Barycenter", "Neptune", 24622000 / Sun.RadiusMeter,
            new Color(124, 157, 226), JHVTableCellRenderer.cellBorder));
        objectList.add(new SpaceObject("Pluto%20Barycenter", "Pluto", 1195000 / Sun.RadiusMeter,
            new Color(205, 169, 140), JHVTableCellRenderer.cellEmphasisBorder));

        objectList.add(new SpaceObject("CHURYUMOV-GERASIMENKO", "67P/Churyumov-Gerasimenko", 2200 / Sun.RadiusMeter,
            Color.WHITE,JHVTableCellRenderer.cellEmphasisBorder));

        objectList.add(new SpaceObject("SOHO", "SOHO", 2 / Sun.RadiusMeter,
            Color.WHITE, JHVTableCellRenderer.cellBorder));
        objectList.add(new SpaceObject("STEREO%20Ahead", "STEREO Ahead", 2 / Sun.RadiusMeter,
            Color.WHITE, JHVTableCellRenderer.cellBorder));
        objectList.add(new SpaceObject("STEREO%20Behind", "STEREO Behind", 2 / Sun.RadiusMeter,
            Color.WHITE, JHVTableCellRenderer.cellBorder));
        objectList.add(new SpaceObject("PROBA2", "PROBA-2", 2 / Sun.RadiusMeter,
            Color.WHITE, JHVTableCellRenderer.cellBorder));
        objectList.add(new SpaceObject("SDO", "SDO", 2 / Sun.RadiusMeter,
            Color.WHITE, JHVTableCellRenderer.cellEmphasisBorder));

        objectList.add(new SpaceObject("Solar%20Orbiter", "Solar Orbiter", 2 / Sun.RadiusMeter,
            Color.WHITE, JHVTableCellRenderer.cellBorder));
        objectList.add(new SpaceObject("SPP", "Parker Solar Probe", 2 / Sun.RadiusMeter,
            Color.WHITE, JHVTableCellRenderer.cellBorder));
        objectList.add(new SpaceObject("PROBA3", "PROBA-3", 2 / Sun.RadiusMeter,
            Color.WHITE, JHVTableCellRenderer.cellBorder));
    }

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

    Border getBorder() {
        return border;
    }

    @SuppressWarnings("serial")
    public static class CellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean hasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            if (value instanceof SpaceObject)
                label.setBorder(((SpaceObject) value).getBorder());
            return label;
        }
    }

}
