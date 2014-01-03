package org.helioviewer.gl3d.model.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.media.opengl.GL;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DNode;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.view.GL3DImageTextureView;

/**
 * The {@link GL3DImageLayers} node offers special capabilities for grouping
 * {@link GL3DImageLayer} nodes, because image nodes require special ordering
 * for the blending of different image layers.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DImageLayers extends GL3DGroup {

    private HashMap<GL3DImageTextureView, GL3DImageLayer> imageLayerMap;

    private boolean coronaVisibility = true;

    public GL3DImageLayers() {
        super("Images");
        this.imageLayerMap = new HashMap<GL3DImageTextureView, GL3DImageLayer>();
    }

    public void shapeDraw(GL3DState state) {
        if (!this.isDrawBitOn(Bit.Wireframe)) {
            GL3DState.get().checkGLErrors("GL3DImageLayers.beforeEnable");
            state.gl.glEnable(GL.GL_FRAGMENT_PROGRAM_ARB);
            state.gl.glEnable(GL.GL_VERTEX_PROGRAM_ARB);

        }
        state.gl.glDisable(GL.GL_LIGHTING);

        // state.gl.glEnable(GL.GL_BLEND);
        // drawBlendedImageGroup(state, this, false, false);
        // drawBlendedImageGroup(state, sphereGroup, false, true);
        this.drawImageLayers(state);

        state.gl.glDisable(GL.GL_FRAGMENT_PROGRAM_ARB);
        state.gl.glDisable(GL.GL_VERTEX_PROGRAM_ARB);
        GL3DState.get().checkGLErrors("GL3DImageLayers.afterDisable");

        state.gl.glDisable(GL.GL_BLEND);
        state.gl.glEnable(GL.GL_LIGHTING);
    }

    private void drawImageLayers(GL3DState state) {
        GL3DNode node = this.getFirst();

        // Create sorted list of image layers
        ArrayList<GL3DImageLayer> layers = new ArrayList<GL3DImageLayer>();
        while (node != null) {
            if (!node.isDrawBitOn(Bit.Hidden) && node instanceof GL3DImageLayer)
                layers.add((GL3DImageLayer) node);
            node = node.getNext();
        }

        Collections.sort(layers, new Comparator<GL3DImageLayer>() {
            public int compare(GL3DImageLayer o1, GL3DImageLayer o2) {
                if (o1.getLastViewAngle() == o2.getLastViewAngle())
                    return 0;
                return o1.getLastViewAngle() < o2.getLastViewAngle() ? 1 : -1;
            }
        });

        // Draw the coronas first
        if(this.coronaVisibility){
	        state.gl.glEnable(GL.GL_BLEND);
	        state.gl.glDisable(GL.GL_DEPTH_TEST);
	        state.gl.glDisable(GL.GL_CULL_FACE);
	
	        for (GL3DImageLayer layer : layers) {
	            if (layer.getImageSphere() != null)
	                layer.getImageSphere().getDrawBits().on(Bit.Hidden);
	
	            layer.draw(state);
	
	            if (layer.getImageSphere() != null)
	                layer.getImageSphere().getDrawBits().off(Bit.Hidden);
	        }
        }
        /*
         * while(node!=null) { if(!node.isDrawBitOn(Bit.Hidden)) { if(node
         * instanceof GL3DImageLayer) { //
         * System.out.println("Drawing GL3DImageLayer Corona");
         * 
         * GL3DImageLayer layer = ((GL3DImageLayer)node);
         * if(layer.getImageSphere()!=null)
         * layer.getImageSphere().getDrawBits().on(Bit.Hidden);
         * 
         * layer.draw(state);
         * 
         * if(layer.getImageSphere()!=null)
         * layer.getImageSphere().getDrawBits().off(Bit.Hidden); } } node =
         * node.getNext(); }
         */

        state.gl.glEnable(GL.GL_CULL_FACE);
        state.gl.glEnable(GL.GL_DEPTH_TEST);
        // state.gl.glDisable(GL.GL_BLEND);
        state.gl.glEnable(GL.GL_BLEND);

        for (GL3DImageLayer layer : layers) {
            if (layer.getImageCorona() != null)
                layer.getImageCorona().getDrawBits().on(Bit.Hidden);

            layer.draw(state);

            if (layer.getImageCorona() != null)
                layer.getImageCorona().getDrawBits().off(Bit.Hidden);
        }

        /*
         * node = this.getFirst(); while(node!=null) {
         * if(!node.isDrawBitOn(Bit.Hidden)) { if(node instanceof
         * GL3DImageLayer) { //
         * System.out.println("Drawing GL3DImageLayer Sphere");
         * 
         * GL3DImageLayer layer = ((GL3DImageLayer)node);
         * if(layer.getImageCorona()!=null)
         * layer.getImageCorona().getDrawBits().on(Bit.Hidden);
         * 
         * layer.draw(state);
         * 
         * if(layer.getImageCorona()!=null)
         * layer.getImageCorona().getDrawBits().off(Bit.Hidden); } } node =
         * node.getNext(); }
         */

        state.gl.glDisable(GL.GL_BLEND);
        state.gl.glEnable(GL.GL_DEPTH_TEST);
    }

    /*
     * private void drawBlendedImageGroup(GL3DState state, GL3DGroup group,
     * boolean depthTest, boolean cullFace) { GL3DNode node = group.getFirst();
     * boolean first = true; // state.gl.glEnable(GL.GL_DEPTH_TEST); //
     * state.gl.glEnable(GL.GL_CULL_FACE); while(node!=null) { //When a layer is
     * disabled the Hidden bit is enabled. If the first layer was disabled, the
     * logic should //skip to the next node... if(node.isDrawBitOn(Bit.Hidden))
     * { node = node.getNext(); continue; } else { node.draw(state); node =
     * node.getNext(); }
     * 
     * if(first) { // if(!depthTest) { // state.gl.glDisable(GL.GL_DEPTH_TEST);
     * // state.gl.glEnable(GL.GL_BLEND); // state.gl.glDepthMask(true); // }
     * first = false; } } state.gl.glDisable(GL.GL_BLEND);
     * state.gl.glEnable(GL.GL_DEPTH_TEST); }
     */

    public void shapeUpdate(GL3DState state) {
        super.shapeUpdate(state);
        updateImageLayerPriorities(state);
    }

    private void updateImageLayerPriorities(GL3DState state) {
        GL3DCamera activeCamera = state.getActiveCamera();
        GL3DMat4d VM = activeCamera.getVM();
        GL3DVec3d LA = new GL3DVec3d();
        GL3DVec3d LR = new GL3DVec3d();
        GL3DVec3d LU = new GL3DVec3d();
        GL3DVec3d EYE = new GL3DVec3d();

        VM.readLookAt(EYE, LA, LU, LR);

        LA.negate();

        // for(GL3DImageLayer imageLayer : this.imageLayerMap.values()) {
        // GL3DVec3d normal = GL3DHelper.toVec(imageLayer.getOrientation());
        // double angle = Math.acos(normal.dot(LA));
        // Log.debug("GL3DImageLayers: Angle to "+imageLayer.getName()+" is "+
        // Math.toDegrees(angle)+"�");
        // }
    }

    public void setCoronaVisibility(boolean visible) {
        GL3DNode node = this.first;
        while (node != null) {
            if (node instanceof GL3DImageLayer) {
                ((GL3DImageLayer) node).setCoronaVisibility(visible);
            }

            node = node.getNext();
        }
        this.coronaVisibility = visible;
    }

    public boolean getCoronaVisibility() {
        return this.coronaVisibility;
    }

    public void insertLayer(GL3DImageLayer layer) {
        this.imageLayerMap.put(layer.getImageTextureView(), layer);
        this.addNode(layer);
    }

    public void removeLayer(GL3DState state, GL3DImageTextureView view) {
        GL3DImageLayer layer = getImageLayerForView(view);
        layer.delete(state);
        Log.debug("GL3DImageLayers: Removed Layer " + layer.getName());
        this.imageLayerMap.remove(view);
    }

    public void moveImages(GL3DImageTextureView view, int index) {
        GL3DImageLayer layer = getImageLayerForView(view);
        layer.getParent().moveNode(layer, index);
    }

    public GL3DImageLayer getImageLayerForView(GL3DImageTextureView view) {
        return this.imageLayerMap.get(view);
    }

}
