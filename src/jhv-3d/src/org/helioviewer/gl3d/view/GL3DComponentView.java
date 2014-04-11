package org.helioviewer.gl3d.view;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.DisplayListener;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.GL3DComponentFakeInterface;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.renderer.screen.GLScreenRenderGraphics;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.viewmodel.view.AbstractComponentView;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.view.opengl.GLSharedContext;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.viewmodel.view.opengl.GLView;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderView;
import org.helioviewer.viewmodel.view.opengl.shader.GLMinimalFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLMinimalVertexShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderHelper;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderView;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.Viewport;

import com.sun.opengl.util.FPSAnimator;

/**
 * The top-most View in the 3D View Chain. Let's the viewchain render to its
 * {@link GLCanvas}.
 * 
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DComponentView extends AbstractComponentView implements GLEventListener, ComponentView, DisplayListener, GL3DComponentFakeInterface {
    private GLCanvas canvas;
    private FPSAnimator animator;

    private Color backgroundColor = Color.BLACK;
    private boolean backGroundColorHasChanged = false;

    private boolean rebuildShadersRequest = false;

    private GLTextureHelper textureHelper = new GLTextureHelper();
    private GLShaderHelper shaderHelper = new GLShaderHelper();

    // private GL3DOrthoView orthoView;
    private ViewportView viewportView;

    private Vector2dInt viewportSize;

    public GL3DComponentView() {
        this.setCanvas(new GLCanvas(null, null, GLSharedContext.getSharedContext(), null));
        this.getCanvas().setMinimumSize(new java.awt.Dimension(100,100));
        Displayer.getSingletonInstance().register(this);        
        Displayer.getSingletonInstance().addListener(this);
        this.getCanvas().addGLEventListener(this);
    }

    public void deactivate() {

    }

    public void activate() {
    }

    public GLCanvas getComponent() {
        return this.getCanvas();
    }

    public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {
        Log.debug("GL3DComponentView.DisplayChanged");
    }

    public void init(GLAutoDrawable glAD) {
        Log.debug("GL3DComponentView.Init");
        GLSharedContext.setSharedContext(glAD.getContext());

        GL gl = glAD.getGL();
        GL3DState.create(gl);
        
        // GLTextureCoordinate.init(gl);
        textureHelper.delAllTextures(gl);
        GLTextureHelper.initHelper(gl);

        shaderHelper.delAllShaderIDs(gl);
        //gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        // gl.glShadeModel(GL.GL_FLAT);
        gl.glShadeModel(GL.GL_SMOOTH);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_BLEND);
        // gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE,
        // GL.GL_REPLACE);
        gl.glEnable(GL.GL_BLEND);
        gl.glEnable(GL.GL_POINT_SMOOTH);
        gl.glEnable(GL.GL_COLOR_MATERIAL);

        gl.glEnable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_NORMALIZE);
        // gl.glEnable(GL.GL_CULL_FACE);
        gl.glCullFace(GL.GL_BACK);
        gl.glFrontFace(GL.GL_CCW);
        gl.glEnable(GL.GL_DEPTH_TEST);
        //gl.glDepthFunc(GL.GL_LESS);
        gl.glDepthFunc(GL.GL_LEQUAL);

        gl.glEnable(GL.GL_LIGHT0);

        viewportSize = new Vector2dInt(0, 0);
        this.rebuildShadersRequest = true;
        // gl.glColor3f(1.0f, 1.0f, 0.0f);
    }

    public void reshape(GLAutoDrawable glAD, int x, int y, int width, int height) {
        viewportSize = new Vector2dInt(width, height);
        GL gl = glAD.getGL();

        gl.setSwapInterval(1);

        updateViewport();
    }

    public synchronized void display(GLAutoDrawable glAD) {
    	
    		
        GL gl = glAD.getGL();

        int width = this.viewportSize.getX();
        int height = this.viewportSize.getY();
        GL3DState.getUpdated(gl, width, height);

        if (backGroundColorHasChanged) {
            gl.glClearColor(backgroundColor.getRed() / 255.0f, backgroundColor.getGreen() / 255.0f, backgroundColor.getBlue() / 255.0f, backgroundColor.getAlpha() / 255.0f);

            backGroundColorHasChanged = false;
        }

        // Rebuild all shaders, if necessary
        if (rebuildShadersRequest) {
            rebuildShaders(gl);
        }

        // Viewport viewport =
        // view.getAdapter(ViewportView.class).getViewport();
        // int width = viewport.getWidth();
        // int height = viewport.getHeight();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glColor4f(1, 1, 1, 1);
        gl.glEnable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_DEPTH_TEST);

        // gl.glLoadIdentity();

        gl.glPushMatrix();

        if (this.getView() instanceof GLView) {
            ((GLView) this.getView()).renderGL(gl, true);
        }
        gl.glPopMatrix();

        gl.glPushMatrix();
        if (!this.postRenderers.isEmpty()) {

            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glLoadIdentity();

            gl.glOrtho(0, width, 0, height, -1, 10000);

            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glTranslatef(0.0f, height, 0.0f);
            gl.glScalef(1.0f, -1.0f, 1.0f);
            gl.glDisable(GL.GL_LIGHTING);
            gl.glColor4f(1, 1, 1, 0);
            gl.glDisable(GL.GL_DEPTH_TEST);
            gl.glEnable(GL.GL_TEXTURE_2D);

            GLScreenRenderGraphics glRenderer = new GLScreenRenderGraphics(gl);
            synchronized (postRenderers) {
                for (ScreenRenderer r : postRenderers) {
                    r.render(glRenderer);
                }
            }
            gl.glDisable(GL.GL_TEXTURE_2D);
            
        }
        gl.glPopMatrix();
        GL3DState.get().checkGLErrors();
    }

    public void saveScreenshot(String imageFormat, File outputFile) throws IOException {
        throw new UnsupportedOperationException("Cannot Save screenshots in 3D mode yet!");
    }

    public void setBackgroundColor(Color background) {
        backgroundColor = background;
        backGroundColorHasChanged = true;
    }

    public void setOffset(Vector2dInt offset) {
        // if(this.orthoView!=null) {
        // orthoView.setOffset(offset);
        // }
    }

    public void updateMainImagePanelSize(Vector2dInt size) {
        super.updateMainImagePanelSize(size);
        this.viewportSize = size;

        // if(this.orthoView!=null) {
        // this.orthoView.updateMainImagePanelSize(size);
        // }
        if (this.viewportView != null) {
            Viewport viewport = StaticViewport.createAdaptedViewport(Math.max(1, size.getX()), Math.max(1, size.getY()));
            this.viewportView.setViewport(viewport, null);
        }
    }

    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        // this.orthoView = getAdapter(GL3DOrthoView.class);
        this.viewportView = getAdapter(ViewportView.class);
    }

    private void updateViewport() {
        // this.orthoView.updateMainImagePanelSize(mainImagePanelSize);
    }

    public void display(){
    	try{
    	    this.canvas.display();
    	}
    	catch( Exception e){
            Log.warn("Display of GL3DComponentView canvas failed", e);    		
    	}
    }
    
    public void viewChanged(View sender, ChangeEvent aEvent) {

        // rebuild shaders, if necessary
        if (aEvent.reasonOccurred(ViewChainChangedReason.class) || (aEvent.reasonOccurred(LayerChangedReason.class) && aEvent.getLastChangedReasonByType(LayerChangedReason.class).getLayerChangeType() == LayerChangeType.LAYER_ADDED)) {
            rebuildShadersRequest = true;
            // this.orthoView = getAdapter(GL3DOrthoView.class);
            this.viewportView = getAdapter(ViewportView.class);
            this.updateViewport();
        }
        
        TimestampChangedReason timestampReason = aEvent.getLastChangedReasonByType(TimestampChangedReason.class);
        if ((timestampReason != null) && (timestampReason.getView() instanceof TimedMovieView) && LinkedMovieManager.getActiveInstance().isMaster((TimedMovieView) timestampReason.getView())) {
        	try{
        	    //this.display();
        	    Displayer.getSingletonInstance().display();
        	}
        	catch( Exception e){
        		
        	}
        }        

        notifyViewListeners(aEvent);
    }

    private void rebuildShaders(GL gl) {
        rebuildShadersRequest = false;
        shaderHelper.delAllShaderIDs(gl);

        GLFragmentShaderView fragmentView = view.getAdapter(GLFragmentShaderView.class);
        if (fragmentView != null) {
            // create new shader builder
            GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl, GL.GL_FRAGMENT_PROGRAM_ARB);

            // fill with standard values
            GLMinimalFragmentShaderProgram minimalProgram = new GLMinimalFragmentShaderProgram();
            minimalProgram.build(newShaderBuilder);

            // fill with other filters and compile
            fragmentView.buildFragmentShader(newShaderBuilder).compile();
        }

        GLVertexShaderView vertexView = view.getAdapter(GLVertexShaderView.class);
        if (vertexView != null) {
            // create new shader builder
            GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl, GL.GL_VERTEX_PROGRAM_ARB);

            // fill with standard values
            GLMinimalVertexShaderProgram minimalProgram = new GLMinimalVertexShaderProgram();
            minimalProgram.build(newShaderBuilder);

            // fill with other filters and compile
            vertexView.buildVertexShader(newShaderBuilder).compile();
        }
    }

	public GLCanvas getCanvas() {
		return canvas;
	}

	public void setCanvas(GLCanvas canvas) {
		this.canvas = canvas;
	}
     
}
