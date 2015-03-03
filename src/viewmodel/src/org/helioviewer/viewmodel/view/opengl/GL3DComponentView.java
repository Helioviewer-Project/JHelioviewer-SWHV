package org.helioviewer.viewmodel.view.opengl;

import java.awt.Color;
import java.awt.Component;
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

import org.helioviewer.jhv.gui.states.StateController;
import org.helioviewer.jhv.gui.states.ViewStateEnum;
import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.movie.MovieExport;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.DisplayListener;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.ViewListenerDistributor;
import org.helioviewer.jhv.gui.dialogs.ExportMovieDialog;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.renderer.screen.GLScreenRenderGraphics;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.viewmodel.view.AbstractComponentView;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.RegionView;

import com.jogamp.opengl.util.TileRenderer;
import com.jogamp.opengl.util.awt.AWTGLPixelBuffer;
import com.jogamp.opengl.util.awt.ImageUtil;

/**
 * The top-most View in the 3D View Chain. Lets the viewchain render to its
 * {@link GLCanvas}.
 *
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DComponentView extends AbstractComponentView implements GLEventListener, ComponentView, DisplayListener {
    private static DrawInterface draw;

    // general
    private GLCanvas canvas;
    private final AWTGLPixelBuffer.SingleAWTGLPixelBufferProvider pixelBufferProvider = new AWTGLPixelBuffer.SingleAWTGLPixelBufferProvider(true);

    private Color backgroundColor = Color.BLACK;
    private boolean backGroundColorChanged = false;

    private boolean rebuildShaders = false;

    // screenshot & movie
    private TileRenderer tileRenderer;
    private BufferedImage screenshot;
    private int previousScreenshot = -1;

    private ExportMovieDialog exportMovieDialog;
    private MovieExport export;
    private boolean exportMode = false;
    private boolean screenshotMode = false;
    private File outputFile;

    @Override
    public void activate() {
        if ((view instanceof GLView) == false)
            throw new NullPointerException("View is not an instance of GLView");

        rebuildShaders = true;
        if (StateController.getInstance().getCurrentState().getType() == ViewStateEnum.View3D)
            draw = new Draw3DInterface();
        else
            draw = new Draw2DInterface();

        canvas.addGLEventListener(this);
        Displayer.getSingletonInstance().addListener(this);
    }

    @Override
    public void deactivate() {
        if (screenshot != null) {
            screenshot.flush();
            screenshot = null;
        }
        tileRenderer = null;

        Displayer.getSingletonInstance().removeListener(this);
        canvas.removeGLEventListener(this);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
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
        screenshotMode = true;
        Displayer.getSingletonInstance().render();
    }

    public void stopScreenshot() {
        screenshotMode = false;
    }

    @Override
    public boolean saveScreenshot(String imageFormat, File outputFile) throws IOException {
        this.outputFile = outputFile;
        startScreenshot();
        return true;
    }

    private static interface DrawInterface {

        public void init(GL2 gl);

        public void reshapeView(GL2 gl, int width, int height);

        public void displayBody(GL2 gl, View v, int width, int height);

    }

    private static class Draw3DInterface implements DrawInterface {

        @Override
        public final void init(GL2 gl) {
            gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
            gl.glShadeModel(GL2.GL_SMOOTH);

            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

            gl.glEnable(GL2.GL_BLEND);
            gl.glEnable(GL2.GL_POINT_SMOOTH);
            gl.glEnable(GL2.GL_COLOR_MATERIAL);

            gl.glEnable(GL2.GL_NORMALIZE);
            // gl.glEnable(GL2.GL_CULL_FACE);
            gl.glCullFace(GL2.GL_BACK);
            gl.glFrontFace(GL2.GL_CCW);
            // gl.glDepthFunc(GL2.GL_LESS);
            gl.glDepthFunc(GL2.GL_LEQUAL);

            gl.glEnable(GL2.GL_LIGHT0);
        }

        @Override
        public final void reshapeView(GL2 gl, int width, int height) {
        }

        @Override
        public final void displayBody(GL2 gl, View v, int width, int height) {
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_DEPTH_TEST);

            ((GL3DView) v).renderGL(gl, true);
        }

    }

    private static class Draw2DInterface implements DrawInterface {

        @Override
        public final void init(GL2 gl) {
            gl.glDisable(GL2.GL_DEPTH_TEST);
            gl.glShadeModel(GL2.GL_FLAT);
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glEnable(GL2.GL_TEXTURE_1D);
            gl.glEnable(GL2.GL_TEXTURE_2D);
            gl.glEnable(GL2.GL_BLEND);
            gl.glEnable(GL2.GL_POINT_SMOOTH);
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        }

        @Override
        public final void reshapeView(GL2 gl, int width, int height) {
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();

            gl.glOrtho(0, width, 0, height, -1, 1);

            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadIdentity();
        }

        @Override
        public final void displayBody(GL2 gl, View v, int width, int height) {
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

            Region region = v.getAdapter(RegionView.class).getRegion();

            if (region != null) {
                gl.glScalef(width / (float) region.getWidth(), height / (float) region.getHeight(), 1.0f);
                gl.glTranslatef((float) -region.getCornerX(), (float) -region.getCornerY(), 0.0f);

                ((GLView) v).renderGL(gl, true);
            }
        }
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        GL3DState.create(gl);
        GLTextureHelper.initHelper(gl);

        draw.init(gl);
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
                stopExport();
                Log.warn("Premature stop of video export: no active layer found");
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

        if (backGroundColorChanged) {
            gl.glClearColor(backgroundColor.getRed() / 255.0f, backgroundColor.getGreen() / 255.0f,
                            backgroundColor.getBlue() / 255.0f, backgroundColor.getAlpha() / 255.0f);
            backGroundColorChanged = false;
        }

        if (rebuildShaders) {
            GLShaderBuilder.rebuildShaders(gl, (GLView) view);
            rebuildShaders = false;
        }

        gl.glPushMatrix();
        draw.displayBody(gl, view, width, height);
        gl.glPopMatrix();

        if (!this.postRenderers.isEmpty()) {
            gl.glPushMatrix();

            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();

            gl.glOrtho(0, width, 0, height, -1, 1);

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
                stopExport();
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
            stopScreenshot();
        }
        // GL3DState.get().checkGLErrors();
    }


    @Override
    public void setBackgroundColor(Color background) {
        backgroundColor = background;
        backGroundColorChanged = true;
    }

    @Override
    public void display() {
        canvas.repaint();
    }

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        LayerChangedReason lcReason = aEvent.getLastChangedReasonByType(LayerChangedReason.class);
        if ((lcReason != null && lcReason.getLayerChangeType() == LayerChangeType.LAYER_ADDED) ||
             aEvent.reasonOccurred(ViewChainChangedReason.class)) {
            rebuildShaders = true;
        }

        SubImageDataChangedReason sidReason = aEvent.getLastChangedReasonByType(SubImageDataChangedReason.class);
        if (sidReason != null) {
            Displayer.getSingletonInstance().display();
        }

        ViewListenerDistributor.getSingletonInstance().viewChanged(this, aEvent);
    }

    @Override
    public void setComponent(Component component) {
        canvas = (GLCanvas) component;
    }

    @Override
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
    }

}
