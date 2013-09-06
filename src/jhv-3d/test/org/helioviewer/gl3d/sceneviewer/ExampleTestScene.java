package org.helioviewer.gl3d.sceneviewer;

import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DNode;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DArrow;

public class ExampleTestScene extends GL3DTestScene {
    public static void main(String[] args) {
        System.out.println("Starting Example TestScene...");
        new ExampleTestScene();
    }

    public GL3DNode getSceneRoot() {
        GL3DGroup root = new GL3DGroup("Root");

        GL3DArrow arrow = new GL3DArrow(0.5, 1, 12, new GL3DVec4f(1f, 1f, 1f, 1f));
        // arrow.modelView().rotate(Math.PI/2, 0, 1, 1);
        root.addNode(arrow);

        return root;
    }
}
