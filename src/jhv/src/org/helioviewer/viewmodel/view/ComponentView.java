package org.helioviewer.viewmodel.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.DisplayListener;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.ExportMovieDialog;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.opengl.GLInfo;
import org.helioviewer.viewmodel.view.opengl.GLSLShader;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.jogamp.opengl.util.awt.ImageUtil;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.video.ConverterFactory;

/**
 * The ComponentView is responsible for rendering the actual image to a Component.
 */
public class ComponentView implements GLEventListener, DisplayListener {

    private static GLCanvas canvas;

    // screenshot & movie
    private ExportMovieDialog exportMovieDialog;

    private String moviePath;
    private IMediaWriter movieWriter;
    private double framerate;

    private boolean exportMode = false;
    private boolean screenshotMode = false;
    private int previousScreenshot = -1;
    private File outputFile;

    public ComponentView() {
        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);

        GLAutoDrawable sharedDrawable = GLDrawableFactory.getFactory(glp).createDummyAutoDrawable(null, true, caps, null);
        sharedDrawable.display();

        canvas = new GLCanvas(caps);
        // GUI events can lead to context destruction and invalidation of GL objects and state
        canvas.setSharedAutoDrawable(sharedDrawable);

        canvas.setMinimumSize(new Dimension(1, 1));
        canvas.addGLEventListener(this);

        Displayer.addListener(this);
    }

    public final Component getComponent() {
        return canvas;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();

        GLInfo.update(gl);
        GLInfo.updatePixelScale(canvas);

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

        GL3DCamera camera = Displayer.getActiveCamera();
        camera.applyPerspective(gl);
        camera.applyCamera(gl);
        ImageViewerGui.getRenderableContainer().render(gl);
        camera.drawCamera(gl);
        camera.resumePerspective(gl);

        if (exportMode || screenshotMode) {
            exportFrame();
        }
    }

    @Override
    public void display() {
        canvas.display();
    }

    private void exportFrame() {
        AbstractView mv = Displayer.getLayersModel().getActiveView();
        if (mv == null) {
            stopExport();
            return;
        }

        AWTGLReadBufferUtil rbu = new AWTGLReadBufferUtil(canvas.getGLProfile(), false);
        GL2 gl = (GL2) canvas.getGL();
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
                BufferedImage xugScreenshot = ConverterFactory.convertToType(screenshot, BufferedImage.TYPE_3BYTE_BGR);
                movieWriter.encodeVideo(0, xugScreenshot, (int) (1000 / framerate * currentScreenshot), TimeUnit.MILLISECONDS);
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
            exportMode = true;

            JHVJPXView jpxView = (JHVJPXView) mv;
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss");
            String dateString = format.format(new Date(System.currentTimeMillis()));
            moviePath = JHVDirectory.EXPORTS.getPath() + "JHV_" + mv.getName().replace(" ", "_") + "__" + dateString + ".mp4";

            framerate = jpxView.getDesiredRelativeSpeed();
            if (framerate <= 0 || framerate > 60)
                framerate = 20;

            movieWriter = ToolFactory.makeWriter(moviePath);
            movieWriter.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, canvas.getWidth(), canvas.getHeight());

            jpxView.pauseMovie();
            jpxView.setCurrentFrame(0);
            jpxView.playMovie();
        } else {
            exportMovieDialog.fail();
            exportMovieDialog = null;
        }
    }

    private void stopExport() {
        exportMode = false;
        previousScreenshot = -1;
        movieWriter.close();
        movieWriter = null;

        JTextArea text = new JTextArea("Exported movie at: " + moviePath);
        moviePath = null;
        text.setBackground(null);
        JOptionPane.showMessageDialog(ImageViewerGui.getMainFrame(), text);

        ImageViewerGui.getLeftContentPane().setEnabled(true);

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

}
