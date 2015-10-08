package org.helioviewer.jhv.io;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.helioviewer.base.time.TimeUtils;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.opengl.GL3DViewport;

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment.Type;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;

public class MovieExporter {

    private final int w;
    private final int h;
    private final FBObject fbo = new FBObject();
    private int numSamples = 0;
    private TextureAttachment fboTex;

    private final String moviePath;
    private boolean inited;
    private boolean stopped = false;
    private IMediaWriter movieWriter;

    private GL3DViewport vp;
    private int framenumber = 0;

    private MovieExporter(String moviePath, int w, int h) {
        this.w = w;
        this.h = h;
        this.moviePath = moviePath;
    }

    private void initMovieWriter(String moviePath, int w, int h) {
        movieWriter = ToolFactory.makeWriter(moviePath);
        movieWriter.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, w, h);
    }

    private void disposeMovieWriter() {
        movieWriter.close();
        movieWriter = null;
    }

    private void initFBO(final GL2 gl, int fbow, int fboh) {
        fbo.init(gl, fbow, fboh, numSamples);

        numSamples = fbo.getNumSamples();
        if (numSamples > 0) {
            fbo.attachColorbuffer(gl, 0, true);
            fbo.resetSamplingSink(gl);

            fboTex = fbo.getSamplingSink().getTextureAttachment();
        } else {
            fboTex = fbo.attachTexture2D(gl, 0, true);
        }
        numSamples = fbo.getNumSamples();
        fbo.attachRenderbuffer(gl, Type.DEPTH, FBObject.CHOSEN_BITS);
        fbo.unbind(gl);
    }

    private void disposeFBO(final GL2 gl) {
        this.fbo.destroy(gl);
    }

    private void renderFrame(GL2 gl) {
        fbo.bind(gl);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        vp.getCamera().updateCameraWidthAspect(vp.getWidth() / (double) vp.getHeight());
        gl.glViewport(vp.getOffsetX(), vp.getOffsetY(), vp.getWidth(), vp.getHeight());
        vp.getCamera().applyPerspective(gl);
        ImageViewerGui.getRenderableContainer().render(gl, vp);
        fbo.unbind(gl);
    }

    private void exportFrame(GL2 gl, double framerate, int framenumber) {
        fbo.use(gl, fboTex);
        BufferedImage screenshot = new BufferedImage(fbo.getWidth(), fbo.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        byte[] array = ((DataBufferByte) screenshot.getRaster().getDataBuffer()).getData();
        ByteBuffer fb = ByteBuffer.wrap(array);
        gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, fbo.getReadFramebuffer());
        gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels(0, 0, fbo.getWidth(), fbo.getHeight(), GL2.GL_BGR, GL2.GL_UNSIGNED_BYTE, fb);
        ImageUtil.flipImageVertically(screenshot);
        movieWriter.encodeVideo(0, screenshot, (int) (1000 / framerate * framenumber), TimeUnit.MILLISECONDS);
    }

    private void exportMovieStart(GL2 gl) {
        vp = new GL3DViewport(0, 0, w, h, Displayer.getViewport().getCamera(), true);
        initFBO(gl, w, h);
        initMovieWriter(moviePath, w, h);
        inited = true;
    }

    private void exportMovieFrame(GL2 gl) {
        renderFrame(gl);
        exportFrame(gl, 20, framenumber++);
        if (stopped) {
            exportMovieFinish(gl);
        }
    }

    public void stop() {
        stopped = true;
    }

    private void exportMovieFinish(GL2 gl) {
        ImageViewerGui.getMainComponent().detachExport();
        disposeFBO(gl);
        disposeMovieWriter();
    }

    public void handleMovieExport(GL2 gl) {
        if (!inited) {
            exportMovieStart(gl);
            exportMovieFrame(gl);
        } else {
            exportMovieFrame(gl);
        }
    }

    public static MovieExporter exportMovie(int w, int h) {
        String moviePath = JHVDirectory.EXPORTS.getPath() + "JHV_" + "__" + TimeUtils.filenameDateFormat.format(new Date()) + ".mp4";
        MovieExporter me = new MovieExporter(moviePath, w, h);
        ImageViewerGui.getMainComponent().attachExport(me);
        return me;
    }

}
