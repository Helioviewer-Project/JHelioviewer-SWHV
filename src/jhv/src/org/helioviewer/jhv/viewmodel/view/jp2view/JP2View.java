package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_thread_env;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.SubImage;
import org.helioviewer.jhv.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.viewmodel.view.AbstractView;
import org.helioviewer.jhv.viewmodel.view.ViewROI;
import org.helioviewer.jhv.viewmodel.view.jp2view.cache.CacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.cache.CacheStatusLocal;
import org.helioviewer.jhv.viewmodel.view.jp2view.cache.CacheStatusRemote;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPConstants;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPDatabinClass;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPQuery;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_Kdu_cache;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduEngine;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduHelper;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduMeta;

// This class is responsible for reading and decoding of JPEG2000 images
public class JP2View extends AbstractView {

    private static final String[] SUPPORTED_EXTENSIONS = { ".jp2", ".jpx" };

    private int targetFrame = 0;
    private int trueFrame = -1;

    private int fpsCount = 0;
    private long fpsTime = System.currentTimeMillis();
    private float fps;

    private final RenderExecutor executor = new RenderExecutor();
    private final URI uri;
    private final int maxFrame;
    private final int[] builtinLUT;
    private final CacheStatus cacheStatus;

    private J2KReader reader;
    private JHV_Kdu_cache cacheReader;
    private Kdu_cache cacheRender;

    private JPIPSocket socket;

    final MetaData[] metaData;

    public JP2View(URI _uri) throws Exception {
        uri = _uri;

        String name = uri.getPath().toLowerCase();
        boolean supported = false;
        for (String ext : SUPPORTED_EXTENSIONS)
            if (name.endsWith(ext))
                supported = true;
        if (!supported)
            throw new JHV_KduException("File extension not supported");

        try {
            String scheme = uri.getScheme().toLowerCase();
            switch (scheme) {
                case "http":
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

            KakaduEngine kduReader = new KakaduEngine(cacheReader, uri);
            Jpx_source jpx = kduReader.getJpxSource();

            // Retrieve the number of composition layers
            int[] tempVar = new int[1];
            jpx.Count_compositing_layers(tempVar);
            maxFrame = tempVar[0] - 1;

            builtinLUT = KakaduHelper.getLUT(jpx);

            metaData = new MetaData[maxFrame + 1];
            KakaduMeta.cacheMetaData(kduReader.getFamilySrc(), metaData);
            for (int i = 0; i <= maxFrame; i++) {
                if (metaData[i] == null)
                    metaData[i] = new PixelBasedMetaData(256, 256, i); // tbd real size
            }

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

    private void initRemote(JHV_Kdu_cache cache) throws JHV_KduException {
        try {
            // Connect to the JPIP server and add the necessary initial data (the main header as well as the metadata) to cache
            socket = new JPIPSocket(uri, cache);

            JPIPResponse res;
            String req = JPIPQuery.create(JPIPConstants.META_REQUEST_LEN, "stream", "0", "metareq", "[*]!!");
            do {
                res = socket.send(req, cache);
            } while (!res.isResponseComplete());

            if (!cache.isDataBinCompleted(JPIPDatabinClass.MAIN_HEADER_DATABIN, 0, 0)) {
                req = JPIPQuery.create(JPIPConstants.MIN_REQUEST_LEN, "stream", "0");
                do {
                    res = socket.send(req, cache);
                } while (!res.isResponseComplete() && !cache.isDataBinCompleted(JPIPDatabinClass.MAIN_HEADER_DATABIN, 0, 0));
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

    /**
     * This function is used as a callback function which is called by
     * {@link J2KRender} when it has finished decoding an image.
     */
    void setImageData(ImageData newImageData) {
        int frame = newImageData.getMetaData().getFrameNumber();
        if (frame != trueFrame) {
            trueFrame = frame;
            ++fpsCount;
        }

        if (dataHandler != null) {
            dataHandler.handleData(newImageData);
        }
    }

    @Override
    public float getCurrentFramerate() {
        long currentTime = System.currentTimeMillis();
        long delta = currentTime - fpsTime;

        if (delta > 1000) {
            fps = 1000 * fpsCount / (float) delta;
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
    @Override
    public JHVDate getNextTime(AnimationMode mode, int deltaT) {
        int next = targetFrame + 1;
        switch (mode) {
        case STOP:
            if (next > maxFrame) {
                return null;
            }
            break;
        case SWING:
            if (targetFrame == maxFrame) {
                Layers.setAnimationMode(AnimationMode.SWINGDOWN);
                return metaData[targetFrame - 1].getViewpoint().time;
            }
            break;
        case SWINGDOWN:
            if (targetFrame == 0) {
                Layers.setAnimationMode(AnimationMode.SWING);
                return metaData[1].getViewpoint().time;
            }
            return metaData[targetFrame - 1].getViewpoint().time;
        default: // LOOP
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

        if (-lastDiff < currentDiff) {
            return frame - 1;
        } else {
            return frame;
        }
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

    @Override
    public String getName() {
        MetaData m = metaData[0];
        if (m instanceof HelioviewerMetaData) {
            return ((HelioviewerMetaData) m).getFullName();
        } else {
            String name = uri.getPath();
            return name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.'));
        }
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public LUT getDefaultLUT() {
        if (builtinLUT != null) {
            return new LUT(getName() + " built-in", builtinLUT/* , builtinLUT */);
        }
        return getAssociatedLUT();
    }

    private LUT getAssociatedLUT() {
        MetaData m = metaData[0];
        if (m instanceof HelioviewerMetaData) {
            return LUT.get((HelioviewerMetaData) m);
        }
        return null;
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
        executor.execute(this, camera, vp, camera == null ? null : camera.getViewpoint(), targetFrame, factor);
    }

    void signalRenderFromReader(JP2ImageParameter params) {
        if (isAbolished || params.frame != targetFrame)
            return;
        EventQueue.invokeLater(() -> executor.execute(this, params, false));
    }

    KakaduEngine getRenderEngine(Kdu_thread_env threadEnv) throws KduException, IOException {
        Thread.currentThread().setName("Render " + getName());
        KakaduEngine engine = new KakaduEngine(cacheRender, uri);
        engine.getCompositor().Set_thread_env(threadEnv, null);
        return engine;
    }

    protected void signalReader(JP2ImageParameter params) {
        if (reader != null)
            reader.signalReader(params);
    }

    // Recalculates the image parameters used within the jp2-package
    JP2ImageParameter calculateParameter(Camera camera, Viewport vp, Position.Q p, int frame, double factor) {
        MetaData m = metaData[frame];
        Region mr = m.getPhysicalRegion();
        Region r = ViewROI.updateROI(camera, vp, p, m);

        double ratio = 2 * camera.getWidth() / vp.height;
        int totalHeight = (int) (mr.height / ratio + .5);

        ResolutionLevel res = cacheStatus.getResolutionSet(frame).getNextResolutionLevel(totalHeight, totalHeight);

        double currentMeterPerPixel = mr.width / res.width;
        int imageWidth = (int) Math.ceil(r.width / currentMeterPerPixel); // +1 account for floor ??
        int imageHeight = (int) Math.ceil(r.height / currentMeterPerPixel);

        double posX = (r.ulx - mr.ulx) / mr.width * res.width;
        double posY = (r.uly - mr.uly) / mr.height * res.height;
        int imagePositionX = (int) Math.floor(+posX);
        int imagePositionY = (int) Math.floor(-posY);

        SubImage subImage = new SubImage(imagePositionX, imagePositionY, imageWidth, imageHeight, res.width, res.height);

        int maxDim = Math.max(subImage.width, subImage.height);
        double adj = 1;
        if (maxDim > JHVGlobals.hiDpiCutoff && Layers.isMoviePlaying() && !ImageViewerGui.getGLListener().isRecording()) {
            adj = JHVGlobals.hiDpiCutoff / (double) maxDim;
            if (adj > 0.5)
                adj = 1;
            else if (adj > 0.25)
                adj = 0.5;
            else if (adj > 0.125)
                adj = 0.25;
            else if (adj > 0.0625)
                adj = 0.125;
            else if (adj > 0.03125)
                adj = 0.0625;
        }
        factor = Math.min(factor, adj);

        int level = res.level;
        AtomicBoolean status = cacheStatus.getFrameStatus(frame, level);
        boolean frameLevelComplete = status != null && status.get();
        boolean priority = !frameLevelComplete && !Layers.isMoviePlaying();

        JP2ImageParameter params = new JP2ImageParameter(p, subImage, res, frame, factor, priority);
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
    public String getXMLMetaData() {
        String xml = null;
        try {
            KakaduEngine kduTmp = new KakaduEngine(cacheReader, uri);
            xml = KakaduMeta.getXml(kduTmp.getFamilySrc(), trueFrame + 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return xml;
    }

}
