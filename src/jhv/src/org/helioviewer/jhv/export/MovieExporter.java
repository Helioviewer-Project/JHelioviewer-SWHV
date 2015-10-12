package org.helioviewer.jhv.export;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.helioviewer.base.time.TimeUtils;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.camera.GL3DViewport;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.MoviePanel.RecordMode;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.threads.JHVThread;

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment.Type;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;

public class MovieExporter {

    private int w;
    private int h;
    private final FBObject fbo = new FBObject();
    private TextureAttachment fboTex;

    private String moviePath;
    private String imagePath;
    private boolean inited = false;
    private boolean stopped = false;
    private IMediaWriter movieWriter;

    private static BufferedImage lastScreenshot;

    private static final int frameRate = 30;
    private GL3DViewport vp;
    private static int frameNumber = 0;
    private final ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(1024);
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue, new JHVThread.NamedThreadFactory("MovieExporter"), new ThreadPoolExecutor.DiscardPolicy()/* rejectedExecutionHandler */);
    private RecordMode loop;

    private void initMovieWriter(String moviePath, int w, int h) {
        movieWriter = ToolFactory.makeWriter(moviePath);
        movieWriter.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, w, h);
    }

    public void disposeMovieWriter(boolean keep) {
        if (inited) {
            blockingQueue.poll();
            if (keep) {
                executor.submit(new CloseWriter(keep, movieWriter, moviePath));
            } else {
                while (blockingQueue.poll() != null) {
                }
                Future<?> f = executor.submit(new CloseWriter(keep, movieWriter, moviePath));
                try {
                    f.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
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
            executor.submit(new FrameConsumer(screenshot, frameNumber++, movieWriter));
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
    }

    private void reset() {
        stopped = false;
        lastScreenshot = null;
        frameNumber = 0;
        inited = false;
        moviePath = null;
    }

    public void handleMovieExport(GL2 gl) {
        if (stopped) {
            exportMovieFinish(gl);
            reset();
            return;
        }
        if (!inited) {
            exportMovieStart(gl);
        }
        lastScreenshot = renderFrame(gl);
        exportFrame(lastScreenshot);
        if (loop == RecordMode.SHOT) {
            exportMovieFinish(gl);
            reset();
        }
    }

    public void start(int _w, int _h, RecordMode _loop) {
        loop = _loop;
        String prefix = JHVDirectory.EXPORTS.getPath() + "JHV_" + "__" + TimeUtils.filenameDateFormat.format(new Date());
        moviePath = prefix + ".mp4";
        imagePath = prefix + ".png";
        w = _w;
        h = _h;
        ImageViewerGui.getMainComponent().attachExport(instance);
    }

    public void stop() {
        stopped = true;
        Displayer.display();
    }

    private static final MovieExporter instance = new MovieExporter();

    public static MovieExporter getInstance() {
        return instance;
    }

    private MovieExporter() {
    }

    private static class FrameConsumer implements Runnable {

        private final BufferedImage el;
        private final int framenumber;
        private final IMediaWriter im;

        public FrameConsumer(BufferedImage el, int framenumber, IMediaWriter im) {
            this.el = el;
            this.framenumber = framenumber;
            this.im = im;
        }

        @Override
        public void run() {
            ImageUtil.flipImageVertically(el);
            im.encodeVideo(0, el, 1000 / frameRate * framenumber, TimeUnit.MILLISECONDS);
        }

    }

    private class CloseWriter implements Runnable {

        private final boolean done;
        private final IMediaWriter im;
        private final String moviePath;

        public CloseWriter(boolean done, IMediaWriter im, String moviePath) {
            this.done = done;
            this.im = im;
            this.moviePath = moviePath;
        }

        @Override
        public void run() {
            if (im != null) {
                im.close();
            }
            if (!done && moviePath != null) {
                File f = new File(moviePath);
                f.delete();
            }
        }
    }

}
