package org.helioviewer.jhv.camera;

public class SpaceObject {

    private final String urlName;
    private final String labelName;
    private final double sizeInMeters;
    private static SpaceObject objectList[];
    public static SpaceObject earth;
    public static final int LINESEPPLANETS = 10;
    public static final int LINESEPSATS = 17;

    public static SpaceObject[] getObjectList() {
        if (objectList == null) {
            createObjectList();
        }
        return objectList;
    }

    private static void createObjectList() {
        objectList = new SpaceObject[18];

        objectList[0] = new SpaceObject("Mercury", "Mercury", 2439700);
        objectList[1] = new SpaceObject("Venus", "Venus", 6051800);

        objectList[2] = new SpaceObject("Earth", "Earth", 6371000);
        earth = objectList[2];
        objectList[3] = new SpaceObject("Moon", "Moon", 1737400);

        objectList[4] = new SpaceObject("Mars%20Barycenter", "Mars", 3389500);
        objectList[5] = new SpaceObject("Jupiter%20Barycenter", "Jupiter", 69911000);
        objectList[6] = new SpaceObject("Saturn%20Barycenter", "Saturn", 58232000);
        objectList[7] = new SpaceObject("Uranus%20Barycenter", "Uranus", 25362000);
        objectList[8] = new SpaceObject("Neptune%20Barycenter", "Neptune", 24622000);
        objectList[9] = new SpaceObject("Pluto%20Barycenter", "Pluto", 1195000);

        objectList[10] = new SpaceObject("CHURYUMOV-GERASIMENKO", "67P/Churyumov-Gerasimenko", 2200);

        objectList[11] = new SpaceObject("SOHO", "SOHO", 2);
        objectList[12] = new SpaceObject("STEREO%20Ahead", "STEREO Ahead", 2);
        objectList[13] = new SpaceObject("STEREO%20Behind", "STEREO Behind", 2);
        objectList[14] = new SpaceObject("PROBA2", "PROBA2", 2);
        objectList[15] = new SpaceObject("SDO", "SDO", 2);

        objectList[16] = new SpaceObject("Solar%20Orbiter", "Solar Orbiter", 2);
        objectList[17] = new SpaceObject("PROBA3", "PROBA3", 2);
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
