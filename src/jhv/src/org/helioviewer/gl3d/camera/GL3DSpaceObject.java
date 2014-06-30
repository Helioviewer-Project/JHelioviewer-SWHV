package org.helioviewer.gl3d.camera;

public class GL3DSpaceObject {
    private final String urlName;
    private final String labelName;
    private static GL3DSpaceObject objectList[];

    public static GL3DSpaceObject[] getObjectList() {
        if (objectList == null) {
            createObjectList();
        }
        return objectList;
    }

    private static void createObjectList() {
        objectList = new GL3DSpaceObject[13];

        objectList[0] = new GL3DSpaceObject("Mercury", "Mercury");
        objectList[1] = new GL3DSpaceObject("Venus", "Venus");
        objectList[2] = new GL3DSpaceObject("Earth", "Earth");
        objectList[3] = new GL3DSpaceObject("Moon", "Moon");

        objectList[4] = new GL3DSpaceObject("Mars%20Barycenter", "Mars");
        objectList[5] = new GL3DSpaceObject("Saturn%20Barycenter", "Saturn");
        objectList[6] = new GL3DSpaceObject("Uranus%20Barycenter", "Uranus");
        objectList[7] = new GL3DSpaceObject("Neptune%20Barycenter", "Neptune");
        objectList[8] = new GL3DSpaceObject("Jupiter%20Barycenter", "Jupiter");
        objectList[9] = new GL3DSpaceObject("Pluto%20Barycenter", "Pluto");

        objectList[10] = new GL3DSpaceObject("STEREO%20Ahead", "STEREO Ahead");
        objectList[11] = new GL3DSpaceObject("STEREO%20Behind", "STEREO Behind");
        objectList[12] = new GL3DSpaceObject("Solar%20Orbiter", "Solar Orbiter");
    }

    private GL3DSpaceObject(String urlName, String labelName) {
        this.urlName = urlName;
        this.labelName = labelName;
    }

    public String getUrlName() {
        return this.urlName;
    }

    @Override
    public String toString() {
        return this.labelName;
    }
}
