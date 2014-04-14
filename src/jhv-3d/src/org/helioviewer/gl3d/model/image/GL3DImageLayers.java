package org.helioviewer.gl3d.model.image;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.media.opengl.GL;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DNode;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DCircle;
import org.helioviewer.gl3d.view.GL3DImageTextureView;


/**
 * The {@link GL3DImageLayers} node offers special capabilities for grouping
 * {@link GL3DImageLayer} nodes, because image nodes require special ordering
 * for the blending of different image layers.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DImageLayers extends GL3DGroup {

    private HashMap<GL3DImageTextureView, GL3DImageLayer> imageLayerMap;

    private boolean coronaVisibility = true;
    
    public GL3DImageLayers() {
        super("Images");
        this.imageLayerMap = new HashMap<GL3DImageTextureView, GL3DImageLayer>();
    }
    
    
    @Override
    public void shapeInit(GL3DState state) {
    	//addNode(GL3DCoronaSort.getInstance());
    	super.shapeInit(state);
    }

    
    
    public void shapeDraw(GL3DState state) {
        if (!this.isDrawBitOn(Bit.Wireframe)) {
            GL3DState.get().checkGLErrors("GL3DImageLayers.beforeEnable");
            state.gl.glEnable(GL.GL_FRAGMENT_PROGRAM_ARB);
            state.gl.glEnable(GL.GL_VERTEX_PROGRAM_ARB);

        }
        //state.gl.glDisable(GL.GL_LIGHTING);

        // state.gl.glEnable(GL.GL_BLEND);
        // drawBlendedImageGroup(state, this, false, false);
        // drawBlendedImageGroup(state, sphereGroup, false, true);
        this.drawImageLayers(state);
        //state.gl.glUniform1f(arg0, arg1)
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
                
        
        state.gl.glDisable(GL.GL_FRAGMENT_PROGRAM_ARB);
        state.gl.glDisable(GL.GL_VERTEX_PROGRAM_ARB);
        
        //state.gl.glDisable(GL.GL_CULL_FACE);
        //state.gl.glEnable(GL.GL_DEPTH_TEST);
        state.gl.glEnable(GL.GL_BLEND);
        state.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        
        

        
        state.gl.glEnable(GL.GL_FRAGMENT_PROGRAM_ARB);
        state.gl.glEnable(GL.GL_VERTEX_PROGRAM_ARB);        
        state.gl.glEnable(GL.GL_DEPTH_TEST);
        state.gl.glEnable(GL.GL_BLEND);
        
        for (GL3DImageLayer layer : layers) {
            if (layer.getImageSphere() != null)
            	layer.getImageSphere().draw(state);
        }
        
        
        state.gl.glEnable(GL.GL_BLEND);
        state.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
        state.gl.glDepthFunc(GL.GL_LEQUAL);
        state.gl.glDepthMask(false);
        
        for (GL3DImageLayer layer : layers) {
            layer.getImageCorona().draw(state);    
        }
        
        state.gl.glDepthMask(true);
        state.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);        
        state.gl.glDisable(GL.GL_BLEND);
        
    }


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
        
        layer.setLayerGroup(this);
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

    
    
    public Collection<GL3DImageLayer> getLayers()
    {
    	GL3DNode node = this.getFirst();
        ArrayList<GL3DImageLayer> layers = new ArrayList<GL3DImageLayer>();
        for(; node!=null; node = node.getNext()) {
            if(node instanceof GL3DImageLayer)
            	layers.add((GL3DImageLayer) node);
        }
    	
        return layers;    	
    }
}
