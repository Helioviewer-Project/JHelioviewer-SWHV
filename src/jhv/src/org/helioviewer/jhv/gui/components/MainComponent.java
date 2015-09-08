package org.helioviewer.jhv.gui.components;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.helioviewer.base.time.TimeUtils;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.ExportMovieDialog;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.opengl.GLSLShader;
import org.helioviewer.jhv.renderable.viewport.GL3DViewport;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.video.ConverterFactory;

@SuppressWarnings("serial")
public class MainComponent extends GLCanvas implements GLEventListener {

    // screenshot & movie
    private final AWTGLReadBufferUtil rbu;

    private ExportMovieDialog exportMovieDialog;

    private String moviePath;
    private IMediaWriter movieWriter;
    private final double framerate = 30;

    private boolean exportMode = false;
    private boolean screenshotMode = false;
    private int previousScreenshot = -1;
    private File outputFile;

    public MainComponent() {
        super(new GLCapabilities(GLProfile.getDefault()));

        GLAutoDrawable sharedDrawable = GLDrawableFactory.getFactory(getGLProfile()).createDummyAutoDrawable(null, true, getRequestedGLCapabilities(), null);
        sharedDrawable.display();

        // GUI events can lead to context destruction and invalidation of GL objects and state
        setSharedAutoDrawable(sharedDrawable);
        setMinimumSize(new Dimension());
        setAutoSwapBufferMode(false);

        addGLEventListener(this);
        Displayer.setDisplayComponent(this);

        rbu = new AWTGLReadBufferUtil(getGLProfile(), false);
    }

    @Override
    public void init(GLAutoDrawable drawable) throws GLException {
        GL2 gl = drawable.getGL().getGL2(); // try to force an exception

        GLInfo.update(gl);
        GLInfo.updatePixelScale(this);

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

        GLSLShader.init(gl);
        ImageViewerGui.getRenderableContainer().init(gl);
        //initFrameBuffer(gl);
    }

    private final int[] framebufferName = new int[1];
    private final int[] renderedTexture = new int[1];
    private final int[] depthrenderbuffer = new int[1];
    private final int buffs[] = { GL.GL_COLOR_ATTACHMENT0 };

    private void initFrameBuffer(GL2 gl) {
        gl.glGenFramebuffers(1, framebufferName, 0);
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, framebufferName[0]);
        gl.glGenTextures(1, renderedTexture, 0);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, renderedTexture[0]);

        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGB, 1024, 768, 0, GL2.GL_RGB, GL2.GL_UNSIGNED_BYTE, null);

        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);

        gl.glGenRenderbuffers(1, depthrenderbuffer, 0);
        gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, depthrenderbuffer[0]);
        gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT, 1024, 768);
        gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER, depthrenderbuffer[0]);
        gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL2.GL_TEXTURE_2D, renderedTexture[0], 0);
        gl.glDrawBuffers(1, buffs, 0);
        if (gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER) != GL2.GL_FRAMEBUFFER_COMPLETE) {
            System.out.println("CANNOT CONFIGURE FRAMEBUFFERS");
            System.exit(2);
        }

    }

    private void disposeFrameBuffer(GL2 gl) {
        gl.glDeleteRenderbuffers(1, depthrenderbuffer, 0);
        gl.glDeleteTextures(1, renderedTexture, 0);
        gl.glDeleteFramebuffers(1, framebufferName, 0);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();
        //disposeFrameBuffer(gl);
        ImageViewerGui.getRenderableContainer().dispose(gl);
        GLSLShader.dispose(gl);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        int w = getWidth();
        int h = getHeight();
        Displayer.getActiveCamera().updateCameraWidthAspect(w / (double) h);
        Displayer.getActiveViewport().setViewportSize(w, h);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();
        GLInfo.updatePixelScale(this);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        for (GL3DViewport vp : Displayer.getViewports()) {
            if (vp.isVisible()) {
                vp.getCamera().activate(Displayer.getActiveCamera());
                vp.getCamera().updateCameraWidthAspect(vp.getWidth() / (double) vp.getHeight());
                gl.glViewport(vp.getOffsetX(), vp.getOffsetY(), vp.getWidth() * GLInfo.pixelScale[0], vp.getHeight() * GLInfo.pixelScale[1]);
                vp.getCamera().applyPerspective(gl);
                ImageViewerGui.getRenderableContainer().render(gl, vp);
            }
        }

        drawable.swapBuffers();

        if (exportMode || screenshotMode) {
            exportFrame(gl);
        }
    }

    private void exportFrame(GL2 gl) {
        View view = Layers.getActiveView();
        if (view == null) {
            stopExport();
            return;
        }

        BufferedImage screenshot;
        int width = getWidth();

        int currentScreenshot = view.getImageLayer().getImageData().getFrameNumber();
        if (exportMode && currentScreenshot == previousScreenshot + 1) {
            int maxframeno = view.getMaximumFrameNumber();

            screenshot = ImageUtil.createThumbnail(rbu.readPixelsToBufferedImage(gl, true), width);
            if (currentScreenshot != previousScreenshot) {
                BufferedImage xugScreenshot = ConverterFactory.convertToType(screenshot, BufferedImage.TYPE_3BYTE_BGR);
                movieWriter.encodeVideo(0, xugScreenshot, (int) (1000 / framerate * currentScreenshot), TimeUnit.MILLISECONDS);
            }
            exportMovieDialog.setLabelText("Exporting frame " + (currentScreenshot + 1) + " / " + (maxframeno + 1));
            previousScreenshot = currentScreenshot;

            if (currentScreenshot == maxframeno) {
                stopExport();
            }
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

        View mv = Layers.getActiveView();
        if (mv instanceof JHVJP2View) {
            ImageViewerGui.getLeftContentPane().setEnabled(false);

            moviePath = JHVDirectory.EXPORTS.getPath() + "JHV_" + mv.getName().replace(" ", "_") + "__" + TimeUtils.filenameDateFormat.format(new Date()) + ".mp4";

            movieWriter = ToolFactory.makeWriter(moviePath);
            movieWriter.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, getWidth(), getHeight());

            Layers.pauseMovie();
            Layers.setFrame(0);
            Layers.playMovie();
            exportMode = true;
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
