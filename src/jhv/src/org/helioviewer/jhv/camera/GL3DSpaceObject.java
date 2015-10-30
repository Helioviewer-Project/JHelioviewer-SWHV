package org.helioviewer.jhv.camera;

public class GL3DSpaceObject {

    private final String urlName;
    private final String labelName;
    private final double sizeInMeters;
    private static GL3DSpaceObject objectList[];
    public static GL3DSpaceObject earth;
    public static final int LINESEPPLANETS = 10;
    public static final int LINESEPSATS = 17;

    public static GL3DSpaceObject[] getObjectList() {
        if (objectList == null) {
            createObjectList();
        }
        return objectList;
    }

    private static void createObjectList() {
        objectList = new GL3DSpaceObject[18];

        objectList[0] = new GL3DSpaceObject("Mercury", "Mercury", 2439700);
        objectList[1] = new GL3DSpaceObject("Venus", "Venus", 6051800);

        objectList[2] = new GL3DSpaceObject("Earth", "Earth", 6371000);
        earth = objectList[2];
        objectList[3] = new GL3DSpaceObject("Moon", "Moon", 1737400);

        objectList[4] = new GL3DSpaceObject("Mars%20Barycenter", "Mars", 3389500);
        objectList[5] = new GL3DSpaceObject("Jupiter%20Barycenter", "Jupiter", 69911000);
        objectList[6] = new GL3DSpaceObject("Saturn%20Barycenter", "Saturn", 58232000);
        objectList[7] = new GL3DSpaceObject("Uranus%20Barycenter", "Uranus", 25362000);
        objectList[8] = new GL3DSpaceObject("Neptune%20Barycenter", "Neptune", 24622000);
        objectList[9] = new GL3DSpaceObject("Pluto%20Barycenter", "Pluto", 1195000);

        objectList[10] = new GL3DSpaceObject("CHURYUMOV-GERASIMENKO", "67P/Churyumov-Gerasimenko", 2200);

        objectList[11] = new GL3DSpaceObject("SOHO", "SOHO", 2);
        objectList[12] = new GL3DSpaceObject("STEREO%20Ahead", "STEREO Ahead", 2);
        objectList[13] = new GL3DSpaceObject("STEREO%20Behind", "STEREO Behind", 2);
        objectList[14] = new GL3DSpaceObject("PROBA2", "PROBA2", 2);
        objectList[15] = new GL3DSpaceObject("SDO", "SDO", 2);

        objectList[16] = new GL3DSpaceObject("Solar%20Orbiter", "Solar Orbiter", 2);
        objectList[17] = new GL3DSpaceObject("PROBA3", "PROBA3", 2);
    }

    private GL3DSpaceObject(String urlName, String labelName, double sizeInMeters) {
        this.urlName = urlName;
        this.labelName = labelName;
        this.sizeInMeters = sizeInMeters;
    }

    public String getUrlName() {
        return this.urlName;
    }

    @Override
    public String toString() {
        return this.labelName;
    }

    public double getSize() {
        return this.sizeInMeters;
    }

}
