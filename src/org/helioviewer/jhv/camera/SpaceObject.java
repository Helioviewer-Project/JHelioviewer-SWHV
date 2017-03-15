package org.helioviewer.jhv.camera;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JSeparator;

class SpaceObject {

    private final String urlName;
    private final String labelName;
    private final double sizeInMeters;

    private static ArrayList<Object> objectList;
    static Object earth;

    public static List<Object> getObjectList() {
        if (objectList == null) {
            createObjectList();
        }
        return objectList;
    }

    private static void createObjectList() {
        objectList = new ArrayList<>();

        objectList.add(new SpaceObject("Mercury", "Mercury", 2439700));
        objectList.add(new SpaceObject("Venus", "Venus", 6051800));

        earth = new SpaceObject("Earth", "Earth", 6371000);
        objectList.add(earth);

        objectList.add(new SpaceObject("Moon", "Moon", 1737400));

        objectList.add(new SpaceObject("Mars%20Barycenter", "Mars", 3389500));
        objectList.add(new SpaceObject("Jupiter%20Barycenter", "Jupiter", 69911000));
        objectList.add(new SpaceObject("Saturn%20Barycenter", "Saturn", 58232000));
        objectList.add(new SpaceObject("Uranus%20Barycenter", "Uranus", 25362000));
        objectList.add(new SpaceObject("Neptune%20Barycenter", "Neptune", 24622000));
        objectList.add(new SpaceObject("Pluto%20Barycenter", "Pluto", 1195000));

        objectList.add(new JSeparator());

        objectList.add(new SpaceObject("CHURYUMOV-GERASIMENKO", "67P/Churyumov-Gerasimenko", 2200));

        objectList.add(new JSeparator());

        objectList.add(new SpaceObject("SOHO", "SOHO", 2));
        objectList.add(new SpaceObject("STEREO%20Ahead", "STEREO Ahead", 2));
        objectList.add(new SpaceObject("STEREO%20Behind", "STEREO Behind", 2));
        objectList.add(new SpaceObject("PROBA2", "PROBA2", 2));
        objectList.add(new SpaceObject("SDO", "SDO", 2));

        objectList.add(new JSeparator());

        objectList.add(new SpaceObject("Solar%20Orbiter", "Solar Orbiter", 2));
        objectList.add(new SpaceObject("SPP", "Solar Probe Plus", 2));
        objectList.add(new SpaceObject("PROBA3", "PROBA3", 2));
    }

    private SpaceObject(String _urlName, String _labelName, double _sizeInMeters) {
        urlName = _urlName;
        labelName = _labelName;
        sizeInMeters = _sizeInMeters;
    }

    public String getUrlName() {
        return urlName;
    }

    @Override
    public String toString() {
        return labelName;
    }

    public double getSize() {
        return sizeInMeters;
    }

}
