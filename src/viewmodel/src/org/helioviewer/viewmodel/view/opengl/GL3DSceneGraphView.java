package org.helioviewer.viewmodel.view.opengl;

import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.media.opengl.GL2;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.GL3DKeyController;
import org.helioviewer.gl3d.GL3DKeyController.GL3DKeyListener;
import org.helioviewer.gl3d.math.GL3DVec3d;
import org.helioviewer.gl3d.math.GL3DVec4f;
import org.helioviewer.gl3d.model.GL3DArtificialObjects;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DModel;
import org.helioviewer.gl3d.scenegraph.GL3DNode;
import org.helioviewer.gl3d.scenegraph.GL3DShape;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DArrow;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.GL3DLayeredView;
import org.helioviewer.viewmodel.view.View;

/**
 * This is the most important view in the 3D viewchain. It assembles all 3D
 * Models in a hierarchical scene graph. Also it automatically adds new nodes (
 * {@link GL3DImageMesh}) to the scene when a new layer is added to the
 * {@link GL3DLayeredView}. Furthermore it takes care of setting the currently
 * active image region by performing a ray casting using the
 * {@link GL3DRayTracer} to find the maximally spanning image region within the
 * displayed scene.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DSceneGraphView extends AbstractGL3DView implements GL3DView {
    private final GL3DGroup root;

    // private GL3DImageGroup imageMeshes;
    private GLOverlayView overlayView = null;
    private GL3DGroup artificialObjects;

    private final ArrayList<GL3DNode> nodesToDelete = new ArrayList<GL3DNode>();

    public GL3DSceneGraphView() {
        this.root = createRoot();
        // this.root = createTestRoot();

        printScenegraph();

        GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
            @Override
            public void keyHit(KeyEvent e) {
                root.getDrawBits().toggle(Bit.BoundingBox);
                Displayer.display();
                Log.debug("Toggling BoundingBox");
            }
        }, KeyEvent.VK_B);

        GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
            @Override
            public void keyHit(KeyEvent e) {
                toggleCoronaVisibility();
                Displayer.display();
                Log.debug("Toggling Corona Visibility");
            }
        }, KeyEvent.VK_X);
    }

    @Override
    public void render3D(GL3DState state) {
        GL2 gl = state.gl;

        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBlendEquation(GL2.GL_FUNC_ADD);

        deleteNodes(state);
        if (this.getView() != null) {
            state.pushMV();
            this.renderChild(gl);
            state.popMV();
        }

        if (state.getActiveCamera() == null) {
            Log.warn("GL3DSceneGraph: Camera not ready, aborting renderpass");
            return;
        }
        // gl.glBlendFunc(GL2.GL_ONE, GL2.GL_DST_ALPHA);
        gl.glDisable(GL2.GL_BLEND);
        gl.glEnable(GL2.GL_DEPTH_TEST);

        state.pushMV();
        state.loadIdentity();
        this.root.update(state);
        state.popMV();

        state.pushMV();
        state.getActiveCamera().applyPerspective(state);
        state.getActiveCamera().applyCamera(state);

        if (overlayView != null)
            overlayView.preRender3D(gl);

        this.root.draw(state);

        if (overlayView != null)
            overlayView.postRender3D(gl);

        // Draw the camera or its interaction feedbacks
        state.getActiveCamera().drawCamera(state);

        // Resume Previous Projection
        state.getActiveCamera().resumePerspective(state);

        state.popMV();

        gl.glEnable(GL2.GL_BLEND);
    }

    private void deleteNodes(GL3DState state) {
        for (GL3DNode node : this.nodesToDelete) {
            node.delete(state);
        }
        this.nodesToDelete.clear();
    }

    @Override
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
    }

    public int getNumberOfModels() {
        if (this.root == null) {
            return 0;
        }
        return this.root.getNumberOfChilds(GL3DModel.class);
    }

    public GL3DModel getModelAt(int index) {
        if (this.root == null) {
            return null;
        }
        return this.root.getModelAt(index);
    }

    public GL3DGroup getRoot() {
        return this.root;
    }

    @Override
    public void deactivate(GL3DState state) {
        super.deactivate(state);
        this.getRoot().delete(state);
    }

    private GL3DGroup createRoot() {
        GL3DGroup root = new GL3DGroup("Scene Root");

        artificialObjects = new GL3DArtificialObjects();

        root.addNode(artificialObjects);

        GL3DGroup indicatorArrows = new GL3DModel("Arrows", "Arrows indicating the viewspace axes");
        artificialObjects.addNode(indicatorArrows);

        GL3DShape north = new GL3DArrow("Northpole", Constants.SunRadius / 128, Constants.SunRadius, Constants.SunRadius / 4, 128, new GL3DVec4f(1.0f, 0.2f, 0.1f, 1.0f));
        north.modelView().rotate(-Math.PI / 2, GL3DVec3d.XAxis);
        indicatorArrows.addNode(north);

        GL3DShape south = new GL3DArrow("Southpole", Constants.SunRadius / 128, Constants.SunRadius, Constants.SunRadius / 4, 128, new GL3DVec4f(0.1f, 0.2f, 1.0f, 1.0f));
        south.modelView().rotate(Math.PI / 2, GL3DVec3d.XAxis);
        indicatorArrows.addNode(south);

        return root;
    }

    public void toggleCoronaVisibility() {
    }

    public void printScenegraph() {
        // System.out.println("PRINTING SCENEGRAPH =======================>");
        // printNode(root, 0);
    }

    public void setGLOverlayView(GLOverlayView overlayView) {
        this.overlayView = overlayView;
    }

    private void printNode(GL3DNode node, int level) {
        for (int i = 0; i < level; ++i)
            System.out.print("   ");

        if (node == null) {
            return;
        }

        //System.out.println(node.getClass().getName() + " (" + node.getName() + ")");

        /*
         * GL3DNode sibling = node; while((sibling = sibling.getNext()) != null)
         * { for(int i=0; i<level; ++i) System.out.print("   ");
         * 
         * System.out.println("Sibling: " + sibling.getClass().getName() + " ("
         * + node.getName() + ")"); }
         */

        if (node instanceof GL3DGroup) {
            GL3DGroup grp = (GL3DGroup) node;
            for (int i = 0; i < grp.numChildNodes(); ++i) {
                printNode(grp.getChild(i), level + 1);
            }
        }
    }

    @Override
    protected void renderChild(GL2 gl) {
        ((GLView) view).renderGL(gl, true);
    }

}
