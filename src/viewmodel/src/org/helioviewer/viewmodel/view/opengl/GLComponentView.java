package org.helioviewer.viewmodel.view.opengl;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.gl3d.movie.MovieExport;
import org.helioviewer.jhv.display.DisplayListener;
import org.helioviewer.jhv.display.Displayer;
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

import com.jogamp.opengl.util.FPSAnimator;
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
public class GLComponentView extends AbstractComponentView implements ViewListener, GLEventListener, DisplayListener {
    public static final String SETTING_TILE_WIDTH = "gl.screenshot.tile.width";
    public static final String SETTING_TILE_HEIGHT = "gl.screenshot.tile.height";

    private static final boolean DEBUG = true;

    // general
    private final GLCanvas canvas;
    private final GLAutoDrawable canvasDrawable;
    private RegionView regionView;
    private FPSAnimator animator;

    private int previousScreenshot = -1;

    private MovieExport export;
    private ExportMovieDialog exportMovieDialog;
    private File outputFile;

    // render options
    private Color backgroundColor = Color.BLACK;
    private final Color outsideViewportColor = Color.BLACK;
    private float xOffset = 0.0f;
    private float yOffset = 0.0f;
    private final AbstractList<ScreenRenderer> postRenderers = new LinkedList<ScreenRenderer>();

    // Helper
    private boolean rebuildShadersRequest = false;
    private final GLShaderHelper shaderHelper = new GLShaderHelper();

    // screenshot
    private boolean saveScreenshotRequest = false;
    private boolean saveBufferedImage = false;
    private String saveScreenshotFormat;
    private File saveScreenshotFile;

    private BufferedImage screenshot;

    private TileRenderer tileRenderer;
    private final AWTGLPixelBuffer.SingleAWTGLPixelBufferProvider pixelBufferProvider = new AWTGLPixelBuffer.SingleAWTGLPixelBufferProvider(true);

    private static int defaultTileWidth = 640;
    private static int defaultTileHeight = 640;
    private int tileWidth;
    private int tileHeight;
    private final boolean sharedObjectCreate = false;
    private Vector2dInt viewportSize;
    private boolean exportMode;
    private boolean screenshotMode;

    /**
     * Default constructor.
     *
     * Also initializes all OpenGL Helper classes.
     */
    public GLComponentView() {
        GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());

        GLCanvas glCanvas = new GLCanvas(caps);
        canvas = glCanvas;
        canvasDrawable = glCanvas;

        canvas.setMinimumSize(new Dimension());

        canvasDrawable.addGLEventListener(this);
        animator = new FPSAnimator(canvasDrawable, 30);
        animator.start();
    }

    public void dispose() {
        if (screenshot != null) {
            screenshot.flush();
            screenshot = null;
        }
        tileRenderer = null;
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        dispose();
    }

    /**
     * Stop the animation of the canvas. Useful if one wants to display frame
     * per frame (such as in movie export).
     */
    public void stopAnimation() {
        if (animator != null && animator.isAnimating())
            animator.stop();
    }

    /**
     * (Re-)start the animation of the canvas.
     */
    public void startAnimation() {
        if (animator == null)
            animator = new FPSAnimator(canvasDrawable, 30);
        if (!animator.isAnimating())
            animator.start();
    }

    /**
     * Save the next rendered frame in a buffered image and return this image.
     *
     * WARNING: The returned image is a reference to the internal buffered image
     * of this class. A subsequent call to this function might change the data
     * of this returned buffered image. If this is not desired, one has to make
     * a copy of the returned image, before calling getBufferedImage() again.
     * This choice was made with the movie export application in mind in order
     * to save main memory.
     *
     * @return BufferedImage of the next rendered frame
     */
    public BufferedImage getBufferedImage() {
        saveBufferedImage = true;
        canvasDrawable.display();
        return screenshot;
    }

    public static void setTileSize(int width, int height) {
        defaultTileWidth = width;
        defaultTileHeight = height;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getComponent() {
        return canvas;
    }

    /**
     * {@inheritDoc}
     *
     * Since the screenshot is saved after the next rendering cycle, the result
     * is not available directly after calling this function. It only places a
     * request to save the screenshot.
     */
    @Override
    public boolean saveScreenshot(String imageFormat, File outputFile) throws IOException {
        saveScreenshotRequest = true;
        saveScreenshotFormat = imageFormat;
        saveScreenshotFile = outputFile;
        return true;
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

    /**
     * {@inheritDoc}
     *
     * In this case, the canvas is repainted.
     */
    @Override
    protected synchronized void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        if (newView != null) {
            regionView = newView.getAdapter(RegionView.class);
        }

        canvas.repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {

        if (aEvent.reasonOccurred(ViewChainChangedReason.class)) {
            regionView = view.getAdapter(RegionView.class);
        }

        // rebuild shaders, if necessary
        if (aEvent.reasonOccurred(ViewChainChangedReason.class) || (aEvent.reasonOccurred(LayerChangedReason.class) && aEvent.getLastChangedReasonByType(LayerChangedReason.class).getLayerChangeType() == LayerChangeType.LAYER_ADDED)) {
            rebuildShadersRequest = true;
        }

        // inform all listener of the latest change reason
        // frameUpdated++;
        notifyViewListeners(aEvent);
        if (this.exportMode) {
            TimestampChangedReason timestampReason = aEvent.getLastChangedReasonByType(TimestampChangedReason.class);
            SubImageDataChangedReason sidReason = aEvent.getLastChangedReasonByType(SubImageDataChangedReason.class);

            if (sidReason != null || ((timestampReason != null) && (timestampReason.getView() instanceof TimedMovieView) && LinkedMovieManager.getActiveInstance().isMaster((TimedMovieView) timestampReason.getView()))) {
                Displayer.getSingletonInstance().display();
            }
        }
    }

    @Override
    public void setBackgroundColor(Color color) {
        backgroundColor = color;
    }

    /**
     * {@inheritDoc}
     */
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
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
     * Note, that this function should not be called by any user defined
     * function. It is part of the GLEventListener and invoked by the OpenGL
     * thread.
     *
     * @param drawable
     *            GLAutoDrawable passed by the OpenGL thread
     */
    @Override
    public void init(GLAutoDrawable drawable) {
        //GLDrawableFactory.getFactory(GLProfile.getDefault()).createExternalGLContext();
        final GL2 gl = drawable.getGL().getGL2();
        //gl.getContext().setGL(GLPipelineFactory.create("javax.media.opengl.Trace", null, gl, new Object[] { System.err }));

        GLTextureHelper.initHelper(gl);

        shaderHelper.delAllShaderIDs(gl);

        gl.glShadeModel(GL2.GL_FLAT);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glEnable(GL2.GL_TEXTURE_1D);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glEnable(GL2.GL_BLEND);
        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        gl.glColor3f(1.0f, 1.0f, 1.0f);
    }

    /**
     * Reshapes the viewport.
     *
     * This function is called, whenever the canvas is resized. It ensures, that
     * the perspective never gets corrupted.
     *
     * <p>
     * Note, that this function should not be called by any user defined
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
        final GL2 gl = drawable.getGL().getGL2();
        this.viewportSize = new Vector2dInt(width, height);

        gl.setSwapInterval(1);

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
            // Draw image

            gl.glPushMatrix();

            Region region = regionView.getRegion();

            gl.glTranslatef(xOffsetFinal, yOffsetFinal, 0.0f);
            gl.glScalef(viewportImageSize.getWidth() / (float) region.getWidth(), viewportImageSize.getHeight() / (float) region.getHeight(), 1.0f);
            gl.glTranslated(-region.getCornerX(), -region.getCornerY(), 0.0);

            if (view instanceof GLView) {
                ((GLView) view).renderGL(gl, true);
            } else {
                GLTextureHelper.renderImageDataToScreen(gl, view.getAdapter(RegionView.class).getRegion(), view.getAdapter(SubimageDataView.class).getSubimageData(), view.getAdapter(JHVJP2View.class));
            }
            gl.glPopMatrix();
        }

        if (viewport != null) {
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
        if (!saveBufferedImage && (canvas.getSize().width <= 0 || canvas.getSize().height <= 0)) {
            return;
        }

        final GL2 gl;
        if (DEBUG) {
            gl = new DebugGL2(drawable.getGL().getGL2());
        } else {
            gl = drawable.getGL().getGL2();
        }

        while (rebuildShadersRequest) {
            rebuildShaders(gl);
        }
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
    public void display() {
        try {
            this.canvas.display();
        } catch (Exception e) {
            Log.warn("Display of GL3DComponentView canvas failed", e);
        }
    }

    /**
     * Force rebuilding the shaders during the next rendering iteration.
     */
    public void requestRebuildShaders() {
        rebuildShadersRequest = true;
    }

    /**
     * Start rebuilding all shaders.
     *
     * This function is called, whenever the shader structure of the whole view
     * chain may have changed, e.g. when new views are added.
     *
     * @param gl
     *            Valid reference to the current gl object
     */
    private void rebuildShaders(GL2 gl) {

        rebuildShadersRequest = false;
        shaderHelper.delAllShaderIDs(gl);

        GLFragmentShaderView fragmentView = view.getAdapter(GLFragmentShaderView.class);
        GLVertexShaderView vertexView = view.getAdapter(GLVertexShaderView.class);
        GLVertexShaderView vertexHelperView = null;
        GLFragmentShaderView fragmentHelperView = null;
        while (vertexView != vertexHelperView && fragmentView != fragmentHelperView) {
            fragmentView = fragmentHelperView;
            vertexView = vertexHelperView;
            fragmentHelperView = view.getAdapter(GLFragmentShaderView.class);
            vertexHelperView = view.getAdapter(GLVertexShaderView.class);
        }
        if (fragmentView != null) {
            // create new shader builder
            GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl, GL2.GL_FRAGMENT_PROGRAM_ARB);

            // fill with standard values
            GLMinimalFragmentShaderProgram minimalProgram = new GLMinimalFragmentShaderProgram();
            minimalProgram.build(newShaderBuilder);

            // fill with other filters and compile
            fragmentView.buildFragmentShader(newShaderBuilder).compile();
        }

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

    @Override
    public void deactivate() {
        // TODO Auto-generated method stub

    }

    @Override
    public void activate() {
        // TODO Auto-generated method stub

    }

    @Override
    public void startExport(ExportMovieDialog exportMovieDialog) {
        animator.stop();
        Displayer.getSingletonInstance().addListener(this);
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
            }
        } else {
            exportMovieDialog.fail();
            exportMovieDialog = null;
        }
    }

    public void stopExport() {
        Displayer.getSingletonInstance().removeListener(this);
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
        animator.start();
    }

    public void stopScreenshot() {
        this.screenshotMode = false;
    }
}
