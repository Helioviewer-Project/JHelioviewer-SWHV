package org.helioviewer.viewmodel.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.jhv.display.DisplayListener;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.controller.CameraMouseController;
import org.helioviewer.jhv.gui.dialogs.ExportMovieDialog;
import org.helioviewer.jhv.gui.interfaces.ImagePanelPlugin;
import org.helioviewer.jhv.io.MovieExport;
import org.helioviewer.jhv.renderable.RenderableGrid;
import org.helioviewer.jhv.renderable.RenderableGridType;
import org.helioviewer.jhv.renderable.RenderableSolarAxes;
import org.helioviewer.jhv.renderable.RenderableSolarAxesType;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.opengl.GLInfo;
import org.helioviewer.viewmodel.view.opengl.GLSLShader;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.jogamp.opengl.util.awt.ImageUtil;

/**
 * A ComponentView is responsible for rendering the actual image to a Component.
 */
public class ComponentView implements GLEventListener, DisplayListener {

    private static GLWindow window;
    private static JPanel canvas;

    // screenshot & movie
    private ExportMovieDialog exportMovieDialog;
    private MovieExport export;
    private boolean exportMode = false;
    private boolean screenshotMode = false;
    private int previousScreenshot = -1;
    private File outputFile;

    public ComponentView() {
        RenderableSolarAxesType solarAxesType = new RenderableSolarAxesType("Solar Axes");
        Displayer.getRenderableContainer().addRenderable(new RenderableSolarAxes(solarAxesType));
        RenderableGridType gridType = new RenderableGridType("Grids");
        Displayer.getRenderableContainer().addRenderable(new RenderableGrid(gridType, false));
        Displayer.getRenderableContainer().addRenderable(Displayer.getRenderableCamera());

        window = GLWindow.create(new GLCapabilities(GLProfile.getDefault()));
        window.setUndecorated(true);
        window.addGLEventListener(this);

        // to allow resizing of SplitPane
        canvas = new JPanel(new BorderLayout(0, 0));
        canvas.add(new NewtCanvasAWT(window), BorderLayout.CENTER);
        canvas.setMinimumSize(new Dimension(0, 0));

        CameraMouseController mouseController = new CameraMouseController(canvas);
        canvas.addMouseListener(mouseController);
        canvas.addMouseMotionListener(mouseController);
        canvas.addMouseWheelListener(mouseController);

        Displayer.addListener(this);
    }

    public final Component getComponent() {
        return canvas;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();

        GLInfo.update(gl);
        GLInfo.updatePixelScale(window);

        GLSLShader.initShader(gl);

        gl.glEnable(GL2.GL_TEXTURE_1D);
        gl.glEnable(GL2.GL_TEXTURE_2D);

        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);

        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBlendEquation(GL2.GL_FUNC_ADD);

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LEQUAL);

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        Displayer.setViewportSize(canvas.getWidth(), canvas.getHeight());
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();

        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        gl.glPushMatrix();
        {
            GL3DCamera camera = Displayer.getActiveCamera();
            camera.applyPerspective(gl);
            camera.applyCamera(gl);
            Displayer.getRenderableContainer().render(gl);
            camera.drawCamera(gl);
            camera.resumePerspective(gl);
        }
        gl.glPopMatrix();

        if (exportMode || screenshotMode) {
            exportFrame();
        }
    }

    @Override
    public void display() {
        //canvas.repaint();
        window.display();
    }

    private void exportFrame() {
        AbstractView mv = Displayer.getLayersModel().getActiveView();
        if (mv == null) {
            stopExport();
            return;
        }

        AWTGLReadBufferUtil rbu = new AWTGLReadBufferUtil(window.getGLProfile(), false);
        GL2 gl = (GL2) window.getGL();
        int width = canvas.getWidth();

        BufferedImage screenshot;

        if (exportMode) {
            int currentScreenshot = 1;
            int maxframeno = 1;
            if (mv instanceof JHVJPXView) {
                currentScreenshot = ((JHVJPXView) mv).getCurrentFrameNumber();
                maxframeno = ((JHVJPXView) mv).getMaximumFrameNumber();
            }

            screenshot = ImageUtil.createThumbnail(rbu.readPixelsToBufferedImage(gl, true), width);
            if (currentScreenshot != previousScreenshot) {
                export.writeImage(screenshot);
            }
            exportMovieDialog.setLabelText("Exporting frame " + (currentScreenshot + 1) + " / " + (maxframeno + 1));

            if ((!(mv instanceof JHVJPXView)) || (mv instanceof JHVJPXView && currentScreenshot < previousScreenshot)) {
                stopExport();
            }
            previousScreenshot = currentScreenshot;
        }

        if (screenshotMode) {
            screenshot = ImageUtil.createThumbnail(rbu.readPixelsToBufferedImage(gl, true), width);
            try {
                ImageIO.write(screenshot, "png", outputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            screenshotMode = false;
        }
    }

    public void startExport(ExportMovieDialog exportMovieDialog) {
        this.exportMovieDialog = exportMovieDialog;
        ImageViewerGui.getLeftContentPane().setEnabled(false);

        AbstractView mv = Displayer.getLayersModel().getActiveView();
        if (mv instanceof JHVJPXView) {
            export = new MovieExport(canvas.getWidth(), canvas.getHeight());
            export.createProcess();
            exportMode = true;

            JHVJPXView jpxView = (JHVJPXView) mv;
            jpxView.pauseMovie();
            jpxView.setCurrentFrame(0);
            jpxView.playMovie();
        } else {
            exportMovieDialog.fail();
            exportMovieDialog = null;
        }
    }

    private void stopExport() {
        AbstractView mv = Displayer.getLayersModel().getActiveView();

        exportMode = false;
        previousScreenshot = -1;
        export.finishProcess();

        JTextArea text = new JTextArea("Exported movie at: " + export.getFileName());
        text.setBackground(null);
        JOptionPane.showMessageDialog(canvas, text);

        ImageViewerGui.getLeftContentPane().setEnabled(true);

        ((JHVJPXView) mv).pauseMovie();
        exportMovieDialog.reset();
        exportMovieDialog = null;
    }

    /**
     * Saves the current screen content to the given file in the given format.
     *
     * @param imageFormat
     *            Desired output format
     * @param outputFile
     *            Desired output destination
     * @return
     * @throws IOException
     *             is thrown, if the given output file is not valid
     */
    public boolean saveScreenshot(String imageFormat, File outputFile) {
        this.outputFile = outputFile;
        screenshotMode = true;
        return true;
    }

    private final LinkedList<ImagePanelPlugin> plugins = new LinkedList<ImagePanelPlugin>();

    /**
     * Adds a new plug-in to the component. Plug-ins in this case are controller
     * which e.g. has to react on inputs made to this component.
     *
     * @param newPlugin
     *            new plug-in which has to to be added to this component
     */
    public void addPlugin(ImagePanelPlugin newPlugin) {
        if (newPlugin == null || plugins.contains(newPlugin)) {
            return;
        }

        newPlugin.setImagePanel(canvas);
        newPlugin.setView(this);
        plugins.add(newPlugin);

        if (newPlugin instanceof MouseListener)
            canvas.addMouseListener((MouseListener) newPlugin);
        if (newPlugin instanceof MouseMotionListener)
            canvas.addMouseMotionListener((MouseMotionListener) newPlugin);
        if (newPlugin instanceof MouseWheelListener)
            canvas.addMouseWheelListener((MouseWheelListener) newPlugin);
    }

    public void removePlugin(ImagePanelPlugin oldPlugin) {
        if (oldPlugin == null || !plugins.contains(oldPlugin)) {
            return;
        }

        oldPlugin.setView(null);
        oldPlugin.setImagePanel(null);
        plugins.remove(oldPlugin);

        if (oldPlugin instanceof MouseListener)
            canvas.removeMouseListener((MouseListener) oldPlugin);
        if (oldPlugin instanceof MouseMotionListener)
            canvas.removeMouseMotionListener((MouseMotionListener) oldPlugin);
        if (oldPlugin instanceof MouseWheelListener)
            canvas.removeMouseWheelListener((MouseWheelListener) oldPlugin);
    }

}
