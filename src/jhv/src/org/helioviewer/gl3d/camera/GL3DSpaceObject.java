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
        objectList = new GL3DSpaceObject[4];
        objectList[0] = new GL3DSpaceObject("Earth", "Earth");
        objectList[1] = new GL3DSpaceObject("Mercury", "Mercury");
        objectList[2] = new GL3DSpaceObject("Venus", "Venus");
        objectList[3] = new GL3DSpaceObject("Solar%20Orbiter", "Solar Orbiter");
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
