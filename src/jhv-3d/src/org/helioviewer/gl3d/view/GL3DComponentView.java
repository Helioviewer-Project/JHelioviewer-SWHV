package org.helioviewer.gl3d.view;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.gl3d.movie.MovieExport;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.DisplayListener;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.GL3DComponentFakeInterface;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.ExportMovieDialog;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.renderer.GLCommonRenderGraphics;
import org.helioviewer.viewmodel.renderer.screen.GLScreenRenderGraphics;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.viewmodel.view.AbstractComponentView;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.opengl.GLSharedDrawable;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.viewmodel.view.opengl.GLView;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderView;
import org.helioviewer.viewmodel.view.opengl.shader.GLMinimalFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLMinimalVertexShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderHelper;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderView;
import org.helioviewer.viewmodel.viewport.Viewport;

import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

import com.jogamp.opengl.util.TileRenderer;
import com.jogamp.opengl.util.awt.AWTGLPixelBuffer;
import com.jogamp.opengl.util.awt.ImageUtil;

/**
 * The top-most View in the 3D View Chain. Let's the viewchain render to its
 * {@link GLCanvas}.
 *
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DComponentView extends AbstractComponentView implements GLEventListener, ComponentView, DisplayListener, GL3DComponentFakeInterface {
    private static DrawImplementation draw;

    // general
    private final GLCanvas canvas;
    private final AWTGLPixelBuffer.SingleAWTGLPixelBufferProvider pixelBufferProvider = new AWTGLPixelBuffer.SingleAWTGLPixelBufferProvider(true);

    private Color backgroundColor = Color.BLACK;
    private boolean backGroundColorHasChanged = false;

    private boolean rebuildShadersRequest = false;

    private final GLShaderHelper shaderHelper = new GLShaderHelper();

    // screenshot & movie
    private TileRenderer tileRenderer;
    private BufferedImage screenshot;
    private int previousScreenshot = -1;

    private ExportMovieDialog exportMovieDialog;
    private MovieExport export;
    private boolean exportMode = false;
    private boolean screenshotMode = false;
    private File outputFile;

    public GL3DComponentView() {
        if (Displayer.getSingletonInstance().getState() == Displayer.STATE3D)
            draw = new Draw3DImplementation();
        else
            draw = new Draw2DImplementation();

        canvas = GLSharedDrawable.getSingletonInstance().getCanvas();
        canvas.addGLEventListener(this);

        Displayer.getSingletonInstance().register(this);
        Displayer.getSingletonInstance().addListener(this);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        if (screenshot != null) {
            screenshot.flush();
            screenshot = null;
        }
        tileRenderer = null;

        Displayer.getSingletonInstance().removeListener(this);

        drawable.getGL().getGL2().glFinish();
        drawable.removeGLEventListener(this);
    }

    @Override
    public void deactivate() {
    }

    @Override
    public void activate() {
    }

    @Override
    public GLCanvas getComponent() {
        return canvas;
    }

    @Override
    public void startExport(ExportMovieDialog exportMovieDialog) {
        this.exportMovieDialog = exportMovieDialog;
        ImageViewerGui.getSingletonInstance().getLeftContentPane().setEnabled(false);
        View v = LayersModel.getSingletonInstance().getActiveView();
        if (v != null) {
            JHVJPXView movieView = v.getAdapter(JHVJPXView.class);
            if (movieView != null) {
                movieView.pauseMovie();
                movieView.setCurrentFrame(0, new ChangeEvent());
            }

            export = new MovieExport(canvas.getWidth(), canvas.getHeight());
            export.createProcess();
            exportMode = true;

            if (movieView != null) {
                movieView.playMovie();
            } else {
                Displayer.getSingletonInstance().render();
            }
        } else {
            exportMovieDialog.fail();
            exportMovieDialog = null;
        }
    }

    public void stopExport() {
        View v = LayersModel.getSingletonInstance().getActiveView();
        JHVJPXView movieView = v.getAdapter(JHVJPXView.class);

        exportMode = false;
        previousScreenshot = -1;
        export.finishProcess();

        JTextArea text = new JTextArea("Exported movie at: " + export.getFileName());
        text.setBackground(null);
        JOptionPane.showMessageDialog(ImageViewerGui.getSingletonInstance().getMainImagePanel(), text);

        ImageViewerGui.getSingletonInstance().getLeftContentPane().setEnabled(true);
        if (movieView != null) {
            movieView.pauseMovie();
        }
        exportMovieDialog.reset3D();
        exportMovieDialog = null;
    }

    public void startScreenshot() {
        this.screenshotMode = true;
        Displayer.getSingletonInstance().render();
    }

    public void stopScreenshot() {
        this.screenshotMode = false;
    }

    @Override
    public boolean saveScreenshot(String imageFormat, File outputFile) throws IOException {
        this.outputFile = outputFile;
        this.startScreenshot();
        return true;
    }

    private static interface DrawImplementation {

        public void init(GL2 gl);

        public void reshapeView(GL2 gl, int width, int height);

        public void displayBody(GL2 gl, GLView view);

    }

    private static class Draw3DImplementation implements DrawImplementation {

        @Override
        public void init(GL2 gl) {
            // gl.glEnable(GL2.GL_LINE_SMOOTH);
            gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
            // gl.glShadeModel(GL2.GL_FLAT);
            gl.glShadeModel(GL2.GL_SMOOTH);

            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

            gl.glEnable(GL2.GL_BLEND);
            gl.glEnable(GL2.GL_POINT_SMOOTH);
            gl.glEnable(GL2.GL_COLOR_MATERIAL);

            gl.glEnable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_NORMALIZE);
            // gl.glEnable(GL2.GL_CULL_FACE);
            gl.glCullFace(GL2.GL_BACK);
            gl.glFrontFace(GL2.GL_CCW);
            gl.glEnable(GL2.GL_DEPTH_TEST);
            // gl.glDepthFunc(GL2.GL_LESS);
            gl.glDepthFunc(GL2.GL_LEQUAL);

            gl.glEnable(GL2.GL_LIGHT0);
        }

        @Override
        public void reshapeView(GL2 gl, int width, int height) {
        }

        @Override
        public void displayBody(GL2 gl, GLView view) {
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
            gl.glColor4f(1, 1, 1, 1);
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_DEPTH_TEST);

            view.renderGL(gl, true);
        }

    }

    private static class Draw2DImplementation implements DrawImplementation {

        @Override
        public void init(GL2 gl) {
            gl.glShadeModel(GL2.GL_FLAT);
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glEnable(GL2.GL_TEXTURE_1D);
            gl.glEnable(GL2.GL_TEXTURE_2D);
            gl.glEnable(GL2.GL_BLEND);
            gl.glEnable(GL2.GL_POINT_SMOOTH);
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        }

        @Override
        public void reshapeView(GL2 gl, int width, int height) {
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();

            gl.glOrtho(0, width, 0, height, -1, 1);

            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadIdentity();

            gl.glViewport(0, 0, width, height);
        }

        @Override
        public void displayBody(GL2 gl, GLView view) {
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

            ViewportImageSize viewportImageSize = ViewHelper.calculateViewportImageSize(view);
            if (viewportImageSize != null) {
                Region region = view.getAdapter(RegionView.class).getRegion();
                gl.glScalef(viewportImageSize.getWidth() / (float) region.getWidth(), viewportImageSize.getHeight() / (float) region.getHeight(), 1.0f);
                gl.glTranslatef((float) -region.getCornerX(), (float) -region.getCornerY(), 0.0f);

                view.renderGL(gl, true);
            }
        }
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        GLTextureHelper.initHelper(gl);
        GL3DState.create(gl);

        draw.init(gl);

        this.rebuildShadersRequest = true;
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        draw.reshapeView(drawable.getGL().getGL2(), width, height);
    }

    @Override
    public void display(GLAutoDrawable drawable) {

        JHVJP2View mv = null;
        if (exportMode || screenshotMode) {
            View v = LayersModel.getSingletonInstance().getActiveView();
            if (v != null) {
                mv = v.getAdapter(JHVJP2View.class);
                if (tileRenderer == null) {
                    tileRenderer = new TileRenderer();
                } else if (!tileRenderer.isSetup()) {
                    tileRenderer.reset();
                }
            } else {
                this.stopExport();
                Log.warn("Premature stopping the video export: no active layer found");
            }
        }

        GL2 gl = drawable.getGL().getGL2();
        int width = drawable.getSurfaceWidth();
        int height = drawable.getSurfaceHeight();

        GL3DState.getUpdated(gl, width, height);

        AWTGLPixelBuffer pixelBuffer = null;
        if ((screenshotMode || exportMode) && mv != null) {
            tileRenderer.setTileSize(width, height, 0);
            tileRenderer.setImageSize(width, height);
            pixelBuffer = pixelBufferProvider.allocate(gl, AWTGLPixelBuffer.awtPixelAttributesIntRGB3, width, height, 1, true, 0);
            tileRenderer.setImageBuffer(pixelBuffer);
            int tileNum = 0;
            while (!tileRenderer.eot()) {
                ++tileNum;
                if (tileNum > 1) {
                    break;
                }
                tileRenderer.beginTile(gl);
            }
        }

        if (backGroundColorHasChanged) {
            gl.glClearColor(backgroundColor.getRed() / 255.0f, backgroundColor.getGreen() / 255.0f, backgroundColor.getBlue() / 255.0f, backgroundColor.getAlpha() / 255.0f);
            backGroundColorHasChanged = false;
        }

        // Rebuild all shaders, if necessary
        if (rebuildShadersRequest) {
            rebuildShaders(gl);
        }

        View view = this.getView();
        if (view instanceof GLView) {
            gl.glPushMatrix();
            draw.displayBody(gl, (GLView) view);
            gl.glPopMatrix();
        }

        if (!this.postRenderers.isEmpty()) {
            gl.glPushMatrix();

            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();

            gl.glOrtho(0, width, 0, height, -1, 10000);

            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glTranslatef(0.0f, height, 0.0f);
            gl.glScalef(1.0f, -1.0f, 1.0f);
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glColor4f(1, 1, 1, 0);
            gl.glDisable(GL2.GL_DEPTH_TEST);
            gl.glEnable(GL2.GL_TEXTURE_2D);

            GLScreenRenderGraphics glRenderer = new GLScreenRenderGraphics(gl);
            synchronized (postRenderers) {
                for (ScreenRenderer r : postRenderers) {
                    r.render(glRenderer);
                }
            }
            gl.glPopMatrix();
        }

        if (exportMode && mv != null) {
            int currentScreenshot = 1;
            int maxframeno = 1;
            if (mv instanceof JHVJPXView) {
                currentScreenshot = ((JHVJPXView) mv).getCurrentFrameNumber();
                maxframeno = ((JHVJPXView) mv).getMaximumFrameNumber();
            }
            tileRenderer.endTile(gl);
            screenshot = pixelBuffer.image;
            ImageUtil.flipImageVertically(screenshot);
            if (currentScreenshot != previousScreenshot) {
                export.writeImage(screenshot);
            }
            exportMovieDialog.setLabelText("Exporting frame " + (currentScreenshot + 1) + " / " + (maxframeno + 1));

            if ((!(mv instanceof JHVJPXView)) || (mv instanceof JHVJPXView && currentScreenshot < previousScreenshot)) {
                this.stopExport();
            }
            previousScreenshot = currentScreenshot;
        }

        if (screenshotMode && mv != null) {
            tileRenderer.endTile(gl);
            screenshot = pixelBuffer.image;
            ImageUtil.flipImageVertically(screenshot);
            try {
                ImageIO.write(screenshot, "png", outputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.stopScreenshot();
        }
        // GL3DState.get().checkGLErrors();
    }


    @Override
    public void setBackgroundColor(Color background) {
        backgroundColor = background;
        backGroundColorHasChanged = true;
    }

    @Override
    public void setOffset(Vector2dInt offset) {
    }

/*
    @Override
    public void updateMainImagePanelSize(Vector2dInt size) {
        super.updateMainImagePanelSize(size);

        if (this.viewportView != null) {
            Viewport viewport = StaticViewport.createAdaptedViewport(Math.max(1, size.getX()), Math.max(1, size.getY()));
            this.viewportView.setViewport(viewport, null);
        }
    }
*/

    @Override
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
    }

    @Override
    public void display() {
        try {
            this.canvas.repaint();
        } catch (Exception e) {
            Log.warn("Display of GL3DComponentView canvas failed", e);
        }
    }

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (aEvent != null) {
            LayerChangedReason layerChanged;

            if ((aEvent.reasonOccurred(LayerChangedReason.class) &&
                 (layerChanged = aEvent.getLastChangedReasonByType(LayerChangedReason.class)) != null &&
                  layerChanged.getLayerChangeType() == LayerChangeType.LAYER_ADDED) ||
                aEvent.reasonOccurred(ViewChainChangedReason.class)) {
                rebuildShadersRequest = true;
            }
        }

        TimestampChangedReason timestampReason = aEvent.getLastChangedReasonByType(TimestampChangedReason.class);
        SubImageDataChangedReason sidReason = aEvent.getLastChangedReasonByType(SubImageDataChangedReason.class);

        if (sidReason != null ||
            ((timestampReason != null) && (timestampReason.getView() instanceof TimedMovieView) &&
              LinkedMovieManager.getActiveInstance().isMaster((TimedMovieView) timestampReason.getView()))) {
            Displayer.getSingletonInstance().display();
        }

        notifyViewListeners(aEvent);
    }

    private void rebuildShaders(GL2 gl) {
        rebuildShadersRequest = false;
        shaderHelper.delAllShaderIDs(gl);

        GLFragmentShaderView fragmentView = view.getAdapter(GLFragmentShaderView.class);
        if (fragmentView != null) {
            // create new shader builder
            GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl, GL2.GL_FRAGMENT_PROGRAM_ARB);

            // fill with standard values
            GLMinimalFragmentShaderProgram minimalProgram = new GLMinimalFragmentShaderProgram();
            minimalProgram.build(newShaderBuilder);

            // fill with other filters and compile
            fragmentView.buildFragmentShader(newShaderBuilder).compile();
        }

        GLVertexShaderView vertexView = view.getAdapter(GLVertexShaderView.class);
        if (vertexView != null) {
            // create new shader builder
            GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl, GL2.GL_VERTEX_PROGRAM_ARB);

            // fill with standard values
            GLMinimalVertexShaderProgram minimalProgram = new GLMinimalVertexShaderProgram();
            minimalProgram.build(newShaderBuilder);

            // fill with other filters and compile
            vertexView.buildVertexShader(newShaderBuilder).compile();
        }
    }

}
