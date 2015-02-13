package org.helioviewer.viewmodel.view.opengl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.gl3d.movie.MovieExport;
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
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.renderer.GLCommonRenderGraphics;
import org.helioviewer.viewmodel.renderer.screen.GLScreenRenderGraphics;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.viewmodel.view.AbstractComponentView;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderView;
import org.helioviewer.viewmodel.view.opengl.shader.GLMinimalFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLMinimalVertexShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderHelper;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderView;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

import com.jogamp.opengl.util.TileRenderer;
import com.jogamp.opengl.util.awt.AWTGLPixelBuffer;
import com.jogamp.opengl.util.awt.ImageUtil;

/**
 * Implementation of ComponentView for rendering in OpenGL mode.
 *
 * <p>
 * This class starts the tree walk through all the GLViews to draw the final
 * scene. Therefore the class owns a GLCanvas. Note that GLCanvas is a
 * heavyweight component.
 *
 * <p>
 * For further information about the use of OpenGL within this application, see
 * {@link GLView}.
 *
 * <p>
 * For further information about the role of the ComponentView within the view
 * chain, see {@link org.helioviewer.viewmodel.view.ComponentView}
 *
 * @author Markus Langenberg
 */
public class GLComponentView extends AbstractComponentView implements GLEventListener, ComponentView, DisplayListener, GL3DComponentFakeInterface {
    // general
    private final GLCanvas canvas;
    private final AWTGLPixelBuffer.SingleAWTGLPixelBufferProvider pixelBufferProvider = new AWTGLPixelBuffer.SingleAWTGLPixelBufferProvider(true);

    // render options
    private Color backgroundColor = Color.BLACK;
    private final Color outsideViewportColor = Color.BLACK;

    private RegionView regionView;

    private Vector2dInt viewportSize;
    private float xOffset = 0.0f;
    private float yOffset = 0.0f;
    private final AbstractList<ScreenRenderer> postRenderers = new LinkedList<ScreenRenderer>();

    // Helper
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

    /**
     * Default constructor.
     *
     * Also initializes all OpenGL Helper classes.
     */
    public GLComponentView() {
        GLSharedDrawable shared = GLSharedDrawable.getSingletonInstance();

        //canvas = GLSharedDrawable.getSingletonInstance().getCanvas();
        canvas = new GLCanvas(shared.caps);
        canvas.setSharedAutoDrawable(shared.sharedDrawable);
        canvas.setMinimumSize(new Dimension(0, 0));

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
    public Component getComponent() {
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

    /**
     * Initializes OpenGL2.
     *
     * This function is called when the canvas is visible the first time. It
     * initializes OpenGL by setting some system properties, such as switching
     * on some OpenGL features. Apart from that, the function also calls
     * {@link GLTextureHelper#initHelper(GL2)}.
     *
     * <p>
     * Note that this function should not be called by any user defined
     * function. It is part of the GLEventListener and invoked by the OpenGL
     * thread.
     *
     * @param drawable
     *            GLAutoDrawable passed by the OpenGL thread
     */
    @Override
    public void init(GLAutoDrawable drawable) {
        Log.debug("GLComponentView.Init");

        GL2 gl = drawable.getGL().getGL2();
        //gl.getContext().setGL(GLPipelineFactory.create("javax.media.opengl.Trace", null, gl, new Object[] { System.err }));

        GLTextureHelper.initHelper(gl);

        gl.glShadeModel(GL2.GL_FLAT);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glEnable(GL2.GL_TEXTURE_1D);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glEnable(GL2.GL_BLEND);
        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        this.rebuildShadersRequest = true;
        gl.glColor3f(1.0f, 1.0f, 1.0f);
    }

    /**
     * Reshapes the viewport.
     *
     * This function is called whenever the canvas is resized. It ensures that
     * the perspective never gets corrupted.
     *
     * <p>
     * Note that this function should not be called by any user defined
     * function. It is part of the GLEventListener and invoked by the OpenGL
     * thread.
     *
     * @param drawable
     *            GLAutoDrawable passed by the OpenGL thread
     * @param x
     *            New x-offset on the screen
     * @param y
     *            New y-offset on the screen
     * @param width
     *            New width of the canvas
     * @param height
     *            New height of the canvas
     */
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        this.viewportSize = new Vector2dInt(width, height);

        // gl.setSwapInterval(1);

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        gl.glOrtho(0, width, 0, height, -1, 1);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    /**
     * This function does the actual rendering of the scene on the screen.
     *
     * This is the most important function of this class, it is responsible for
     * rendering the entire scene to the screen. Therefore, it starts the tree
     * walk. After that, all post renderes are called.
     *
     * @param gl
     *            current GL object
     * @param xOffsetFinal
     *            x-offset in pixels
     * @param yOffsetFinal
     *            y-offset in pixels
     */
    protected void displayBody(GL2 gl, float xOffsetFinal, float yOffsetFinal) {
        // Set up screen

        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
        gl.glLoadIdentity();

        Viewport viewport = view.getAdapter(ViewportView.class).getViewport();
        ViewportImageSize viewportImageSize = ViewHelper.calculateViewportImageSize(view);

        if (viewportImageSize != null) {
            gl.glPushMatrix();

            Region region = regionView.getRegion();
            gl.glTranslatef(xOffsetFinal, yOffsetFinal, 0.0f);
            gl.glScalef(viewportImageSize.getWidth() / (float) region.getWidth(), viewportImageSize.getHeight() / (float) region.getHeight(), 1.0f);
            gl.glTranslatef((float) -region.getCornerX(), (float) -region.getCornerY(), 0.0f);

            if (view instanceof GLView) {
                ((GLView) view).renderGL(gl, true);
            } else {
                GLTextureHelper.renderImageDataToScreen(gl, view.getAdapter(RegionView.class).getRegion(), view.getAdapter(SubimageDataView.class).getSubimageData(), view.getAdapter(JHVJP2View.class));
            }

            gl.glPopMatrix();
        }

        if (viewport != null) {
            if (!this.postRenderers.isEmpty()) {
                // Draw post renderer
                gl.glTranslatef(0.0f, viewport.getHeight(), 0.0f);
                gl.glScalef(1.0f, -1.0f, 1.0f);

                GLScreenRenderGraphics glRenderer = new GLScreenRenderGraphics(gl);
                synchronized (postRenderers) {
                    for (ScreenRenderer r : postRenderers) {
                        r.render(glRenderer);
                    }
                }
            }
        }
    }

    /**
     * Displays the scene on the screen.
     *
     * This is the most important function of this class, it is responsible for
     * rendering the entire scene to the screen. Therefore, it starts the tree
     * walk. After that, all post renderes are called.
     *
     * <p>
     * Note, that this function should not be called by any user defined
     * function. It is part of the GLEventListener and invoked by the OpenGL
     * thread.
     *
     * @param drawable
     *            GLAutoDrawable passed by the OpenGL thread
     * @see ComponentView#addPostRenderer(ScreenRenderer)
     */
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

        if (view == null) {
            return;
        }

        GL2 gl = drawable.getGL().getGL2();

        int width = this.viewportSize.getX();
        int height = this.viewportSize.getY();
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

        while (rebuildShadersRequest) {
            rebuildShaders(gl);
        }

        float xOffsetFinal = xOffset;
        float yOffsetFinal = yOffset;
        gl.glClearColor(outsideViewportColor.getRed() / 255.0f, outsideViewportColor.getGreen() / 255.0f, outsideViewportColor.getBlue() / 255.0f, outsideViewportColor.getAlpha() / 255.0f);

        ViewportImageSize viewportImageSize = ViewHelper.calculateViewportImageSize(view);
        if (viewportImageSize != null && canvas != null) {
            if (viewportImageSize.getWidth() < canvas.getWidth()) {
                xOffsetFinal += (canvas.getWidth() - viewportImageSize.getWidth()) / 2;
            }
            if (viewportImageSize.getHeight() < canvas.getHeight()) {
                yOffsetFinal += canvas.getHeight() - viewportImageSize.getHeight() - yOffsetFinal - (canvas.getHeight() - viewportImageSize.getHeight()) / 2;
            }
        }

        displayBody(gl, xOffsetFinal, yOffsetFinal);

        // check for errors
        int errorCode = gl.glGetError();
        if (errorCode != GL2.GL_NO_ERROR) {
            GLU glu = new GLU();
            Log.error("OpenGL Error (" + errorCode + ") : " + glu.gluErrorString(errorCode));
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
    }

    @Override
    public void setBackgroundColor(Color color) {
        backgroundColor = color;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOffset(Vector2dInt offset) {
        xOffset = offset.getX();
        yOffset = offset.getY();
    }

    /**
     * {@inheritDoc}
     *
     * In this case, the canvas is repainted.
     */
    @Override
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        this.regionView = view.getAdapter(RegionView.class);
    }

    @Override
    public void display() {
        try {
            this.canvas.display();
        } catch (Exception e) {
            Log.warn("Display of GLComponentView canvas failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {

        if (aEvent != null) {
            LayerChangedReason layerChanged;

            if ((aEvent.reasonOccurred(LayerChangedReason.class) &&
                 (layerChanged = aEvent.getLastChangedReasonByType(LayerChangedReason.class)) != null &&
                  layerChanged.getLayerChangeType() == LayerChangeType.LAYER_ADDED) ||
                aEvent.reasonOccurred(ViewChainChangedReason.class)) {
                rebuildShadersRequest = true;
                this.regionView = view.getAdapter(RegionView.class);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void addPostRenderer(ScreenRenderer postRenderer) {
        if (postRenderer != null) {
            synchronized (postRenderers) {
                postRenderers.add(postRenderer);
                if (postRenderer instanceof ViewListener) {
                    addViewListener((ViewListener) postRenderer);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePostRenderer(ScreenRenderer postRenderer) {
        if (postRenderer != null) {
            synchronized (postRenderers) {
                do {
                    postRenderers.remove(postRenderer);
                    if (postRenderer instanceof ViewListener) {
                        removeViewListener((ViewListener) postRenderer);
                    }
                } while (postRenderers.contains(postRenderer));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractList<ScreenRenderer> getAllPostRenderer() {
        return postRenderers;
    }

}
