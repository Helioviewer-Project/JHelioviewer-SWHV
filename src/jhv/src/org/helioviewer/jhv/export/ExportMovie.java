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
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.MainComponent;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.MoviePanel.RecordMode;
import org.helioviewer.jhv.layers.FrameListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.threads.JHVThread;

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment.Type;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.ImageUtil;

public class ExportMovie implements FrameListener {

    private static MovieExporter exporter;

    private static int w;
    private static int h;

    private final FBObject fbo = new FBObject();
    private TextureAttachment fboTex;

    private static RecordMode mode;
    private static String moviePath;
    private static String imagePath;
    private static boolean inited = false;
    private static boolean stopped = false;

    private static GL3DViewport vp;

    private final ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(1024);
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue, new JHVThread.NamedThreadFactory("ExportMovie"), new ThreadPoolExecutor.DiscardPolicy());

    public void disposeMovieWriter(boolean keep) {
        if (exporter != null) {
            blockingQueue.poll();
            if (keep) {
                executor.submit(new CloseWriter(exporter, moviePath, keep));
            } else {
                while (blockingQueue.poll() != null) {
                }
                Future<?> f = executor.submit(new CloseWriter(exporter, moviePath, keep));
                try {
                    f.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
            exporter = null;
        }
    }

    private void init(GL2 gl, int w, int h) {
        inited = true;
        vp = new GL3DViewport(0, 0, 0, w, h, Displayer.getViewport().getCamera(), false);

        fbo.init(gl, w, h, 0);
        fboTex = fbo.attachTexture2D(gl, 0, true);

        fbo.attachRenderbuffer(gl, Type.DEPTH, FBObject.CHOSEN_BITS);
        fbo.unbind(gl);
    }

    private void dispose(GL2 gl) {
        inited = false;
        vp = null;

        fbo.detachAll(gl);
        fbo.destroy(gl);
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
    }

    private BufferedImage renderFrame(GL2 gl) {
        BufferedImage screenshot;

        int _w = Displayer.getGLWidth();
        int _h = Displayer.getGLHeight();

        GLHelper.unitScale = true;
        Displayer.setGLSize(w, h);
        Displayer.reshapeAll();
        {
            fbo.bind(gl);
            MainComponent.renderScene(gl);
            fbo.unbind(gl);

            fbo.use(gl, fboTex);

            screenshot = new BufferedImage(fbo.getWidth(), fbo.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            byte[] array = ((DataBufferByte) screenshot.getRaster().getDataBuffer()).getData();
            ByteBuffer fb = ByteBuffer.wrap(array);
            gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, fbo.getReadFramebuffer());
            gl.glPixelStorei(GL2.GL_PACK_ALIGNMENT, 1);
            gl.glReadPixels(0, 0, fbo.getWidth(), fbo.getHeight(), GL2.GL_BGR, GL2.GL_UNSIGNED_BYTE, fb);

            fbo.unuse(gl);
        }
        Displayer.setGLSize(_w, _h);
        Displayer.reshapeAll();
        GLHelper.unitScale = false;

        return screenshot;
    }

    private void exportMovieFinish(GL2 gl) {
        ImageViewerGui.getMainComponent().detachExport();
        ComponentUtils.enableComponents(MoviePanel.getRecordPanel(), true);

        try {
            dispose(gl);
            disposeMovieWriter(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleMovieExport(GL2 gl) {
        if (!inited) {
            init(gl, w, h);
        }

        if (stopped) {
            exportMovieFinish(gl);
            return;
        }

        BufferedImage screenshot = renderFrame(gl);
        try {
            if (mode == RecordMode.SHOT) {
                ImageUtil.flipImageVertically(screenshot);
                ImageIO.write(screenshot, "png", new File(imagePath));
                stop();
            } else {
                try {
                    executor.submit(new FrameConsumer(exporter, screenshot));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void start(int _w, int _h, int fps, RecordMode _mode) {
        w = (_w / 2) * 2; // wiser for video formats
        h = (_h / 2) * 2;
        mode = _mode;

        int ct = Displayer.countActiveLayers();
        Displayer.setViewport(new GL3DViewport(0, 0, 0, w / ct, h / ct, Displayer.getViewport().getCamera()));
        stopped = false;
        currentFrame = 0;

        String prefix = JHVDirectory.EXPORTS.getPath() + "JHV_" + TimeUtils.filenameDateFormat.format(new Date());
        moviePath = prefix + ".mp4";
        imagePath = prefix + ".png";

        ComponentUtils.enableComponents(MoviePanel.getRecordPanel(), false);
        ImageViewerGui.getMainComponent().attachExport(instance);

        if (mode == RecordMode.SHOT) {
            Displayer.display();
        } else {
            try {
                exporter = new XuggleExporter();
                exporter.open(moviePath, w, h, fps);
            } catch (Exception e) {
                e.printStackTrace();
            }

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

            if (mode == RecordMode.LOOP)
                Layers.removeFrameListener(instance);
            if (mode != RecordMode.FREE)
                MoviePanel.clickRecordButton();
            Displayer.setViewport(Displayer.getViewports()[0]);
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

    private static final ExportMovie instance = new ExportMovie();

    private ExportMovie() {
    }

    public static ExportMovie getInstance() {
        return instance;
    }

    private static class FrameConsumer implements Runnable {

        private final MovieExporter movieExporter;
        private final BufferedImage el;

        public FrameConsumer(MovieExporter _movieExporter, BufferedImage _el) {
            movieExporter = _movieExporter;
            el = _el;
        }

        @Override
        public void run() {
            try {
                ImageUtil.flipImageVertically(el);
                movieExporter.encode(el);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private class CloseWriter implements Runnable {

        private final MovieExporter movieExporter;
        private final String moviePath;
        private final boolean keep;

        public CloseWriter(MovieExporter _movieExporter, String _moviePath, boolean _keep) {
            movieExporter = _movieExporter;
            moviePath = _moviePath;
            keep = _keep;
        }

        @Override
        public void run() {
            try {
                if (movieExporter != null) {
                    movieExporter.close();
                }
                if (!keep && moviePath != null) {
                    File f = new File(moviePath);
                    f.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
