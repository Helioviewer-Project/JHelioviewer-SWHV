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
import org.helioviewer.jhv.opengl.GLHelper;

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment.Type;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;

public class MovieExporter {

    private static int w;
    private static int h;
    private final FBObject fbo = new FBObject();
    private TextureAttachment fboTex;

    private static String moviePath;
    private static String imagePath;
    private static boolean inited = false;
    private static boolean stopped = false;
    private static IMediaWriter movieWriter;

    private static BufferedImage lastScreenshot;

    private static GL3DViewport vp;
    private static int frameNumber = 0;

    private static final int frameRate = 30;

    private void initMovieWriter(String moviePath, int w, int h) {
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

    private void initFBO(GL2 gl, int fbow, int fboh) {
        fbo.init(gl, fbow, fboh, 0);
        fboTex = fbo.attachTexture2D(gl, 0, true);

        fbo.attachRenderbuffer(gl, Type.DEPTH, FBObject.CHOSEN_BITS);
        fbo.unbind(gl);
    }

    private void disposeFBO(GL2 gl) {
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

        return screenshot;
    }

    private void exportFrame(BufferedImage screenshot) {
        try {
            ImageUtil.flipImageVertically(screenshot);
            movieWriter.encodeVideo(0, screenshot, (int) (1000 / frameRate * frameNumber), TimeUnit.MILLISECONDS);
            frameNumber++;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exportMovieStart(GL2 gl) {
        vp = new GL3DViewport(0, 0, w, h, Displayer.getViewport().getCamera(), false);
        initFBO(gl, w, h);
        initMovieWriter(moviePath, w, h);
        inited = true;
    }

    private void exportMovieFinish(GL2 gl) {
        ImageViewerGui.getMainComponent().detachExport();
        disposeFBO(gl);

        try {
            if (frameNumber < 2) {
                disposeMovieWriter(false);
                ImageIO.write(lastScreenshot, "png", new File(imagePath));
            } else
                disposeMovieWriter(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        lastScreenshot = null;
        frameNumber = 0;
        inited = false;
    }

    public void handleMovieExport(GL2 gl) {
        if (!inited) {
            exportMovieStart(gl);
        }
        lastScreenshot = renderFrame(gl);
        exportFrame(lastScreenshot);
        if (stopped) {
            exportMovieFinish(gl);
        }
    }

    public static void start(int _w, int _h) {
        String prefix = JHVDirectory.EXPORTS.getPath() + "JHV_" + "__" + TimeUtils.filenameDateFormat.format(new Date());
        moviePath = prefix + ".mp4";
        imagePath = prefix + ".png";
        w = _w;
        h = _h;
        ImageViewerGui.getMainComponent().attachExport(instance);
    }

    public static void stop() {
        stopped = true;
    }

    private static final MovieExporter instance = new MovieExporter();

    private MovieExporter() {}

}
