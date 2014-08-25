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
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLSharedContextSetter;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.glu.GLU;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.renderer.screen.GLScreenRenderGraphics;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.viewmodel.view.AbstractComponentView;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
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
import com.jogamp.opengl.util.TileRendererBase;
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
public class GLComponentView extends AbstractComponentView implements ViewListener, GLEventListener {

    public static final String SETTING_TILE_WIDTH = "gl.screenshot.tile.width";
    public static final String SETTING_TILE_HEIGHT = "gl.screenshot.tile.height";

    private static final boolean DEBUG = true;

    // general
    private Component canvas;
    private GLAutoDrawable canvasDrawable;
    private RegionView regionView;
    private FPSAnimator animator;
    private boolean useOffscreenRendering = false;

    // render options
    private Color backgroundColor = Color.BLACK;
    private final Color outsideViewportColor = Color.DARK_GRAY;
    private float xOffset = 0.0f;
    private float yOffset = 0.0f;
    private final AbstractList<ScreenRenderer> postRenderers = new LinkedList<ScreenRenderer>();

    // Helper
    private boolean rebuildShadersRequest = false;
    private final GLTextureHelper textureHelper = new GLTextureHelper();
    private final GLShaderHelper shaderHelper = new GLShaderHelper();

    // screenshot
    private boolean saveScreenshotRequest = false;
    private boolean saveBufferedImage = false;
    private String saveScreenshotFormat;
    private File saveScreenshotFile;

    private BufferedImage screenshot;

    private TileRenderer tileRenderer;
    private final AWTGLPixelBuffer.SingleAWTGLPixelBufferProvider pixelBufferProvider = new AWTGLPixelBuffer.SingleAWTGLPixelBufferProvider(true);

    private int[] frameBufferObject;
    private int[] renderBufferDepth;
    private int[] renderBufferColor;

    private static int defaultTileWidth;
    private static int defaultTileHeight;
    private int tileWidth;
    private int tileHeight;

    /**
     * Default constructor.
     * 
     * Also initializes all OpenGL Helper classes.
     */
    public GLComponentView() {
        setUpCanvas(false);
    }

    public void useOffscreenRendering() {
        animator.stop();
        canvasDrawable.destroy();
        setUpCanvas(true);
    }

    private void setUpCanvas(boolean useOffscreenRendering) {
        this.useOffscreenRendering = useOffscreenRendering;
        GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        if (useOffscreenRendering) {
            // Bad performance but better Swing compatibility. 
            // For some reason GLCanvas in combination with offscreen framebuffers
            // screws up the canvas positioning.
            caps.setOnscreen(false);
            GLJPanel glPanel = new GLJPanel(caps);
            canvas = glPanel;
            canvasDrawable = glPanel;
        } else {
            GLCanvas glCanvas = new GLCanvas(caps);
            canvas = glCanvas;
            canvasDrawable = glCanvas;
        }
        canvas.setMinimumSize(new Dimension());

        if (GLSharedContext.getSharedContext() != null) {
            ((GLSharedContextSetter) canvas).setSharedContext(GLSharedContext.getSharedContext());
        }
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
        final GL2 gl = drawable.getGL().getGL2();
        gl.glDeleteFramebuffers(1, frameBufferObject, 0);
        gl.glDeleteRenderbuffers(1, renderBufferDepth, 0);
        gl.glDeleteRenderbuffers(1, renderBufferColor, 0);
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
    public void saveScreenshot(String imageFormat, File outputFile) throws IOException {
        saveScreenshotRequest = true;
        saveScreenshotFormat = imageFormat;
        saveScreenshotFile = outputFile;
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
     * {@link GLTextureHelper#initHelper(GL)}.
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
        GLSharedContext.setSharedContext(drawable.getContext());
        GLDrawableFactory.getFactory(GLProfile.getDefault()).createExternalGLContext();
        final GL2 gl = drawable.getGL().getGL2();

        frameBufferObject = new int[1];
        gl.glGenFramebuffers(1, frameBufferObject, 0);
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferObject[0]);
        generateNewRenderBuffers(gl);
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);

        textureHelper.delAllTextures(gl);
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

    private void generateNewRenderBuffers(GL gl) {
        tileWidth = defaultTileWidth;
        tileHeight = defaultTileHeight;
        if (renderBufferDepth != null) {
            gl.glDeleteRenderbuffers(1, renderBufferDepth, 0);
        }
        renderBufferDepth = new int[1];
        gl.glGenRenderbuffers(1, renderBufferDepth, 0);
        gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufferDepth[0]);
        gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT, tileWidth, tileHeight);
        gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER, renderBufferDepth[0]);
        gl.glFramebufferRenderbuffer(GL2.GL_READ_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER, renderBufferDepth[0]);

        if (renderBufferColor != null) {
            gl.glDeleteRenderbuffers(1, renderBufferColor, 0);
        }
        renderBufferColor = new int[1];
        gl.glGenRenderbuffers(1, renderBufferColor, 0);
        gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufferColor[0]);
        gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_RGBA8, tileWidth, tileHeight);
        gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL2.GL_RENDERBUFFER, renderBufferColor[0]);
        gl.glFramebufferRenderbuffer(GL2.GL_READ_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL2.GL_RENDERBUFFER, renderBufferColor[0]);
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

            // clear viewport
            gl.glColor4f(backgroundColor.getRed() / 255.0f, backgroundColor.getGreen() / 255.0f, backgroundColor.getBlue() / 255.0f, backgroundColor.getAlpha() / 255.0f);
            gl.glRectd(region.getCornerX(), region.getCornerY(), region.getUpperRightCorner().getX(), region.getUpperRightCorner().getY());

            if (view instanceof GLView) {
                ((GLView) view).renderGL(gl, true);
            } else {
                textureHelper.renderImageDataToScreen(gl, view.getAdapter(RegionView.class).getRegion(), view.getAdapter(SubimageDataView.class).getSubimageData(), view.getAdapter(JHVJP2View.class));
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
    public synchronized void display(GLAutoDrawable drawable) {

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

        // Rebuild all shaders, if necessary
        if (rebuildShadersRequest) {
            rebuildShaders(gl);
        }
        // Save Screenshot, if requested
        if (saveScreenshotRequest || saveBufferedImage) {
            if (useOffscreenRendering) {
                gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferObject[0]);
            }
            if (tileWidth != defaultTileWidth || tileHeight != defaultTileHeight) {
                generateNewRenderBuffers(gl);
            }

            Log.trace(">> GLComponentView.display() > Default read buffer: " + gl.getGL2ES3().getDefaultReadBuffer());
            Log.trace(">> GLComponentView.display() > Start taking screenshot");
            Viewport v = getAdapter(ViewportView.class).getViewport();
            Log.trace(">> GLComponentView.display() > Initialize tile renderer - Tile size: " + tileWidth + "x" + tileHeight + " - Image size: " + v.getWidth() + "x" + v.getHeight());
            if (tileRenderer == null) {
                tileRenderer = new TileRenderer();
            } else if (!tileRenderer.isSetup()) {
                tileRenderer.reset();
            }
            tileRenderer.setTileSize(tileWidth, tileHeight, 0);
            tileRenderer.setImageSize(v.getWidth(), v.getHeight());
            AWTGLPixelBuffer pixelBuffer = pixelBufferProvider.allocate(gl, AWTGLPixelBuffer.awtPixelAttributesIntRGB3, v.getWidth(), v.getHeight(), 1, true, 0);
            tileRenderer.setImageBuffer(pixelBuffer);
            int tileNum = 0;
            while (!tileRenderer.eot()) {
                ++tileNum;
                tileRenderer.beginTile(gl);
                int x = tileRenderer.getParam(TileRendererBase.TR_CURRENT_TILE_X_POS);
                int y = tileRenderer.getParam(TileRendererBase.TR_CURRENT_TILE_Y_POS);
                int w = tileRenderer.getParam(TileRendererBase.TR_CURRENT_TILE_WIDTH);
                int h = tileRenderer.getParam(TileRendererBase.TR_CURRENT_TILE_HEIGHT);
                int[] matrixMode = new int[1];
                gl.glGetIntegerv(GL2.GL_MATRIX_MODE, matrixMode, 0);
                gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glLoadIdentity();
                gl.glOrtho(x, x + w, y, y + h, -1, 1);
                gl.glMatrixMode(matrixMode[0]);
                gl.glClearColor(backgroundColor.getRed() / 255.0f, backgroundColor.getGreen() / 255.0f, backgroundColor.getBlue() / 255.0f, backgroundColor.getAlpha() / 255.0f);
                displayBody(gl, 0, 0);
                tileRenderer.endTile(gl);
            }
            ;
            Log.trace(">> GLComponentView.display() > Rendered " + tileNum + " tiles.");
            reshape(drawable, 0, 0, v.getWidth(), v.getHeight());

            if (useOffscreenRendering) {
                gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
            }

            screenshot = pixelBuffer.image;
            ImageUtil.flipImageVertically(screenshot);
            if (saveScreenshotRequest) {
                // save image
                try {
                    Log.trace(">> GLComponentView.display() > Write image");
                    ImageIO.write(screenshot, saveScreenshotFormat, saveScreenshotFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                saveScreenshotRequest = false;
            }
            saveBufferedImage = false;

        } else {
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
        }

        // // fps counter;
        // frame++;
        // long time = System.currentTimeMillis();
        //
        // if (time - timebase > 1000) {
        // float factor = 1000.0f/(time-timebase);
        // float fps = frame*factor;
        // float fps2 = frameUpdated*factor;
        // timebase = time;
        // frame = 0;
        // frameUpdated = 0;
        // System.out.println(fps2 + ", " + fps);
        // }

        // check for errors
        int errorCode = gl.glGetError();
        if (errorCode != GL2.GL_NO_ERROR) {
            GLU glu = new GLU();
            Log.error("OpenGL Error (" + errorCode + ") : " + glu.gluErrorString(errorCode));
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

    @Override
    public void deactivate() {
        // TODO Auto-generated method stub

    }

    @Override
    public void activate() {
        // TODO Auto-generated method stub

    }

}