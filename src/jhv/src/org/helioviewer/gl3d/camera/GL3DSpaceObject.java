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
        objectList = new GL3DSpaceObject[9];
        objectList[0] = new GL3DSpaceObject("Mercury", "Mercury");
        objectList[1] = new GL3DSpaceObject("Venus", "Venus");
        objectList[2] = new GL3DSpaceObject("Earth", "Earth");
        objectList[3] = new GL3DSpaceObject("Mars", "Mars");
        objectList[4] = new GL3DSpaceObject("Saturn", "Saturn");
        objectList[5] = new GL3DSpaceObject("Uranus", "Uranus");
        objectList[6] = new GL3DSpaceObject("Jupiter", "Jupiter");
        objectList[7] = new GL3DSpaceObject("Pluto", "Pluto");
        objectList[8] = new GL3DSpaceObject("Solar%20Orbiter", "Solar Orbiter");
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
