package org.helioviewer.jhv.view.jp2view;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_thread_env;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.APIResponse;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.view.AbstractView;
import org.helioviewer.jhv.view.jp2view.cache.CacheStatus;
import org.helioviewer.jhv.view.jp2view.cache.CacheStatusLocal;
import org.helioviewer.jhv.view.jp2view.cache.CacheStatusRemote;
import org.helioviewer.jhv.view.jp2view.image.ImageParams;
import org.helioviewer.jhv.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.jhv.view.jp2view.io.jpip.DatabinMap;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPConstants;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPQuery;
import org.helioviewer.jhv.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.view.jp2view.kakadu.JHV_Kdu_cache;
import org.helioviewer.jhv.view.jp2view.kakadu.KakaduMeta;
import org.helioviewer.jhv.view.jp2view.kakadu.KakaduSource;

// This class is responsible for reading and decoding of JPEG2000 images
public class JP2View extends AbstractView {

    private int targetFrame = 0;
    private int trueFrame = -1;

    private int fps;
    private int fpsCount;
    private long fpsTime = System.currentTimeMillis();

    private final APIResponse response;
    private final RenderExecutor executor = new RenderExecutor();
    private final int maxFrame;
    private final CacheStatus cacheStatus;

    private J2KReader reader;
    private JHV_Kdu_cache cacheReader;
    private Kdu_cache cacheRender;

    private JPIPSocket socket;

    public JP2View(URI _uri, APIRequest _req, APIResponse _res) throws Exception {
        super(_uri, _req);
        response = _res;

        try {
            String scheme = uri.getScheme().toLowerCase();
            switch (scheme) {
                case "jpip":
                    cacheReader = new JHV_Kdu_cache();
                    cacheRender = new Kdu_cache();
                    cacheRender.Attach_to(cacheReader);
                    // cache.Set_preferred_memory_limit(60 * 1024 * 1024);
                    initRemote(cacheReader);
                    break;
                case "file":
                    // nothing
                    break;
                default:
                    throw new JHV_KduException(scheme + " scheme not supported!");
            }

            KakaduSource kduReader = new KakaduSource(cacheReader, uri);

            maxFrame = kduReader.getNumberLayers() - 1;
            metaData = new MetaData[maxFrame + 1];

            //kduReader.cacheMetaData(metaData);
            KakaduMeta.cacheMetaData(kduReader.getFamilySrc(), metaData);
            for (int i = 0; i <= maxFrame; i++) {
                if (metaData[i] == null)
                    metaData[i] = new PixelBasedMetaData(256, 256, i); // tbd real size
            }

            int[] lut = kduReader.getLUT();
            if (lut != null)
                builtinLUT = new LUT(getName() + " built-in", lut);

            if (cacheReader != null) { // remote
                cacheStatus = new CacheStatusRemote(kduReader, maxFrame);
                reader = new J2KReader(this);
            } else {
                cacheStatus = new CacheStatusLocal(kduReader, maxFrame);
            }
        } catch (KduException e) {
            e.printStackTrace();
            throw new JHV_KduException("Failed to create Kakadu machinery: " + e.getMessage(), e);
        }
    }

    private static final int mainHeaderKlass = DatabinMap.getKlass(JPIPConstants.MAIN_HEADER_DATA_BIN_CLASS);

    private void initRemote(JHV_Kdu_cache cache) throws JHV_KduException {
        try {
            // Connect to the JPIP server and add the necessary initial data (the main header as well as the metadata) to cache
            socket = new JPIPSocket(uri, cache);

            JPIPResponse res;
            String req = JPIPQuery.create(JPIPConstants.META_REQUEST_LEN, "stream", "0", "metareq", "[*]!!");
            do {
                res = socket.send(req, cache);
            } while (!res.isResponseComplete());

            if (!cache.isDataBinCompleted(mainHeaderKlass, 0, 0)) {
                req = JPIPQuery.create(JPIPConstants.MIN_REQUEST_LEN, "stream", "0");
                do {
                    res = socket.send(req, cache);
                } while (!res.isResponseComplete() && !cache.isDataBinCompleted(mainHeaderKlass, 0, 0));
            }

            // prime first image
            req = JPIPQuery.create(JPIPConstants.MAX_REQUEST_LEN, "context", "jpxl<0-0>", "fsiz", "64,64,closest", "rsiz", "64,64", "roff", "0,0");
            do {
                res = socket.send(req, cache);
            } while (!res.isResponseComplete());
        } catch (IOException e) {
            initCloseSocket();
            throw new JHV_KduException("Error in the server communication: " + e.getMessage(), e);
        }
    }

    private void initCloseSocket() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.error("JP2Image.initRemote() > Error closing socket.", e);
            }
            socket = null;
        }
    }

    // if instance was built before cancelling
    @Override
    protected void finalize() throws Throwable {
        try {
            abolish();
        } finally {
            super.finalize();
        }
    }

    private volatile boolean isAbolished = false;

    @Override
    public void abolish() {
        if (isAbolished)
            return;
        isAbolished = true;

        new Thread(() -> {
            executor.abolish();
            if (reader != null) {
                reader.abolish();
                reader = null;
            }
            kduDestroy();
        }).start();
    }

    private void kduDestroy() {
        try {
            if (cacheRender != null) {
                cacheRender.Close();
                cacheRender.Native_destroy();
            }
            if (cacheReader != null) {
                cacheReader.Close();
                cacheReader.Native_destroy();
            }
        } catch (KduException e) {
            e.printStackTrace();
        } finally {
            cacheRender = null;
            cacheReader = null;
        }
    }

    @Override
    public int getCurrentFramerate() {
        long currentTime = System.currentTimeMillis();
        long delta = currentTime - fpsTime;

        if (delta > 1000) {
            fps = (int) (1000 * fpsCount / (double) delta + .5);
            fpsCount = 0;
            fpsTime = currentTime;
        }
        return fps;
    }

    @Override
    public boolean isMultiFrame() {
        return maxFrame > 0;
    }

    @Override
    public int getMaximumFrameNumber() {
        return maxFrame;
    }

    @Override
    public int getCurrentFrameNumber() {
        return targetFrame;
    }

    // to be accessed only from Layers
    @Nullable
    @Override
    public JHVDate getNextTime(AnimationMode mode, int deltaT) {
        int next = targetFrame + 1;
        switch (mode) {
        case Stop:
            if (next > maxFrame) {
                return null;
            }
            break;
        case Swing:
            if (targetFrame == maxFrame) {
                Movie.setAnimationMode(AnimationMode.SwingDown);
                return metaData[targetFrame - 1].getViewpoint().time;
            }
            break;
        case SwingDown:
            if (targetFrame == 0) {
                Movie.setAnimationMode(AnimationMode.Swing);
                return metaData[1].getViewpoint().time;
            }
            return metaData[targetFrame - 1].getViewpoint().time;
        default: // Loop
            if (next > maxFrame) {
                return metaData[0].getViewpoint().time;
            }
        }
        return metaData[next].getViewpoint().time;
    }

    @Override
    public void setFrame(JHVDate time) {
        int frame = getFrameNumber(time);
        if (frame != targetFrame) {
            if (frame > cacheStatus.getPartialUntil())
                return;
            targetFrame = frame;
        }
    }

    private int getFrameNumber(JHVDate time) {
        int frame = -1;
        long lastDiff, currentDiff = -Long.MAX_VALUE;
        do {
            lastDiff = currentDiff;
            currentDiff = metaData[++frame].getViewpoint().time.milli - time.milli;
        } while (currentDiff < 0 && frame < maxFrame);
        return -lastDiff < currentDiff ? frame - 1 : frame;
    }

    @Override
    public JHVDate getFrameTime(int frame) {
        if (frame < 0) {
            frame = 0;
        } else if (frame > maxFrame) {
            frame = maxFrame;
        }
        return metaData[frame].getViewpoint().time;
    }

    @Override
    public JHVDate getFirstTime() {
        return metaData[0].getViewpoint().time;
    }

    @Override
    public JHVDate getLastTime() {
        return metaData[maxFrame].getViewpoint().time;
    }

    @Override
    public JHVDate getFrameTime(JHVDate time) {
        return metaData[getFrameNumber(time)].getViewpoint().time;
    }

    @Override
    public MetaData getMetaData(JHVDate time) {
        return metaData[getFrameNumber(time)];
    }

    private volatile boolean isDownloading;

    void setDownloading(boolean val) {
        isDownloading = val;
    }

    @Override
    public boolean isDownloading() {
        return isDownloading;
    }

    @Override
    public void render(Camera camera, Viewport vp, double factor) {
        executor.execute(this, camera, vp, targetFrame, factor);
    }

    void signalRenderFromReader(ImageParams params) {
        if (isAbolished)
            return;
        EventQueue.invokeLater(() -> {
            if (params.frame == targetFrame)
                executor.execute(this, params, false);
        });
    }

    void setDataFromRender(ImageParams params, ImageData data) {
        if (isAbolished)
            return;

        int frame = params.frame;
        if (frame != trueFrame) {
            trueFrame = frame;
            ++fpsCount;
        }

        MetaData m = metaData[frame];
        data.setMetaData(m);
        data.setViewpoint(params.viewpoint);
        data.setRegion(m.roiToRegion(params.subImage, params.resolution.factorX, params.resolution.factorY));

        EventQueue.invokeLater(() -> {
            if (dataHandler != null)
                dataHandler.handleData(data);
        });
    }

    KakaduSource getRenderSource(Kdu_thread_env threadEnv) throws KduException, IOException {
        Thread.currentThread().setName("Render " + getName());
        KakaduSource source = new KakaduSource(cacheRender, uri);
        source.getCompositor().Set_thread_env(threadEnv, null);
        return source;
    }

    protected void signalReader(ImageParams params) {
        if (reader != null)
            reader.signalReader(params);
    }

    // Recalculates the image parameters used within the jp2-package
    ImageParams calculateParams(Camera camera, Viewport vp, int frame, double factor) {
        ResolutionLevel res;
        SubImage subImage;

        if (Movie.isRecording()) { // all bets are off
            res = cacheStatus.getResolutionSet(frame).getResolutionLevel(0);
            subImage = new SubImage(0, 0, res.width, res.height, res.width, res.height);
            factor = 1;
        } else {
            MetaData m = metaData[frame];
            Region mr = m.getPhysicalRegion();
            double ratio = 2 * camera.getWidth() / vp.height;
            int totalHeight = (int) (mr.height / ratio + .5);

            res = cacheStatus.getResolutionSet(frame).getNextResolutionLevel(totalHeight, totalHeight);
            subImage = new SubImage(0, 0, res.width, res.height, res.width, res.height);

            int maxDim = Math.max(res.width, res.height);
            if (maxDim > JHVGlobals.hiDpiCutoff && Movie.isPlaying()) {
                factor = Math.min(factor, 0.5);
            }
        }

        int level = res.level;
        AtomicBoolean status = cacheStatus.getFrameStatus(frame, level);
        boolean frameLevelComplete = status != null && status.get();
        boolean priority = !frameLevelComplete && !Movie.isPlaying();

        ImageParams params = new ImageParams(camera.getViewpoint(), subImage, res, frame, factor, priority);
        if (priority || (!frameLevelComplete && level < currentLevel)) {
            signalReader(params);
        }
        currentLevel = level;

        return params;
    }

    private int currentLevel = 10000;

    @Override
    public AtomicBoolean getFrameCacheStatus(int frame) {
        return cacheStatus.getFrameStatus(frame, currentLevel);
    }

    @Override
    public boolean isComplete() {
        return cacheStatus.isComplete(currentLevel);
    }

    public ResolutionLevel getResolutionLevel(int frame, int level) {
        return cacheStatus.getResolutionSet(frame).getResolutionLevel(level);
    }

    /**
     * Returns the socket, if in remote mode.
     *
     * The socket is returned only one time. After calling this function for the
     * first time, it will always return null.
     *
     * @return Socket connected to the server
     */
    JPIPSocket getSocket() {
        if (socket == null)
            return null;

        JPIPSocket output = socket;
        socket = null;
        return output;
    }

    int getNumComponents(int frame) {
        return cacheStatus.getResolutionSet(frame).numComps;
    }

    JHV_Kdu_cache getReaderCache() {
        return cacheReader;
    }

    CacheStatus getCacheStatus() {
        return cacheStatus;
    }

    // very slow
    @Override
    public String getXMLMetaData() throws Exception {
        KakaduSource kduTmp = new KakaduSource(cacheReader, uri);
        return KakaduMeta.getXml(kduTmp.getFamilySrc(), trueFrame + 1);
    }

}
