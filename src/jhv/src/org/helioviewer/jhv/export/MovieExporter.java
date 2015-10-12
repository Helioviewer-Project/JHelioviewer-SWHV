package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.helioviewer.base.time.TimeUtils;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.camera.GL3DViewport;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.MoviePanel.RecordMode;
import org.helioviewer.jhv.layers.FrameListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLHelper;

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment.Type;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;

public class MovieExporter implements FrameListener {

    private static int w;
    private static int h;
    private final FBObject fbo = new FBObject();
    private TextureAttachment fboTex;

    private static RecordMode mode;
    private static String moviePath;
    private static String imagePath;
    private static boolean inited = false;
    private static boolean stopped = false;
    private static IMediaWriter movieWriter;

    private static GL3DViewport vp;
    private static int frameNumber = 0;

    private static final int frameRate = 30;

    private static void initMovieWriter(String moviePath, int w, int h) {
        movieWriter = ToolFactory.makeWriter(moviePath);
        movieWriter.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, w, h);
    }

    public static void disposeMovieWriter(boolean keep) {
        if (movieWriter != null) {
            movieWriter.close();
            movieWriter = null;
        }
        if (!keep && moviePath != null) {
            File f = new File(moviePath);
            f.delete();
        }
        moviePath = null;
    }

    private void init(GL2 gl, int w, int h) {
        inited = true;
        vp = new GL3DViewport(0, 0, w, h, Displayer.getViewport().getCamera(), false);

        fbo.init(gl, w, h, 0);
        fboTex = fbo.attachTexture2D(gl, 0, true);

        fbo.attachRenderbuffer(gl, Type.DEPTH, FBObject.CHOSEN_BITS);
        fbo.unbind(gl);
    }

    private void dispose(GL2 gl) {
        inited = false;
        vp = null;

        fbo.unbind(gl);
        fbo.detachAll(gl);
        fbo.destroy(gl);
    }

    private BufferedImage renderFrame(GL2 gl) {
        GLHelper.unitScale = true;
        fbo.bind(gl);

        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        vp.getCamera().updateCameraWidthAspect(vp.getWidth() / (double) vp.getHeight());
        gl.glViewport(vp.getOffsetX(), vp.getOffsetY(), vp.getWidth(), vp.getHeight());
        vp.getCamera().applyPerspective(gl);
        ImageViewerGui.getRenderableContainer().render(gl, vp);

        fbo.unbind(gl);
        GLHelper.unitScale = false;

        fbo.use(gl, fboTex);

        BufferedImage screenshot = new BufferedImage(fbo.getWidth(), fbo.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        byte[] array = ((DataBufferByte) screenshot.getRaster().getDataBuffer()).getData();
        ByteBuffer fb = ByteBuffer.wrap(array);
        gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, fbo.getReadFramebuffer());
        gl.glPixelStorei(GL2.GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels(0, 0, fbo.getWidth(), fbo.getHeight(), GL2.GL_BGR, GL2.GL_UNSIGNED_BYTE, fb);

        fbo.unuse(gl);

        ImageUtil.flipImageVertically(screenshot);

        return screenshot;
    }

    private void exportMovieFinish(GL2 gl) {
        dispose(gl);

        try {
            disposeMovieWriter(frameNumber > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleMovieExport(GL2 gl) {
        if (!inited) {
            init(gl, w, h);
        }

        BufferedImage screenshot = renderFrame(gl);
        try {
            if (mode == RecordMode.SHOT) {
                ImageIO.write(screenshot, "png", new File(imagePath));
                stop();
            } else {
                movieWriter.encodeVideo(0, screenshot, (int) (1000 / frameRate * frameNumber), TimeUnit.MILLISECONDS);
                frameNumber++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (stopped) {
            exportMovieFinish(gl);
        }
    }

    public static void start(int _w, int _h, RecordMode _mode) {
        stopped = false;
        frameNumber = 0;
        currentFrame = 0;

        String prefix = JHVDirectory.EXPORTS.getPath() + "JHV_" + "__" + TimeUtils.filenameDateFormat.format(new Date());
        moviePath = prefix + ".mp4";
        imagePath = prefix + ".png";
        w = _w;
        h = _h;
        mode = _mode;

        ImageViewerGui.getMainComponent().attachExport(instance);

        if (mode == RecordMode.SHOT) {
            Displayer.display();
        } else {
            initMovieWriter(moviePath, w, h);
            if (mode == RecordMode.LOOP) {
                Layers.addFrameListener(instance);
                Layers.setFrame(0);
                Layers.playMovie();
             }
        }
    }

    public static void stop() {
        if (!stopped) {
            stopped = true;

            ImageViewerGui.getMainComponent().detachExport();
            if (mode == RecordMode.LOOP) {
                Layers.removeFrameListener(instance);
            }
            MoviePanel.clickRecordButton();
        }
    }

    private static int currentFrame = 0;

    // loop mode only
    @Override
    public void frameChanged(int frame) {
        if (frame < currentFrame)
            stop();
        else
            currentFrame = frame;
    }

    private static final MovieExporter instance = new MovieExporter();

    private MovieExporter() {}

}
