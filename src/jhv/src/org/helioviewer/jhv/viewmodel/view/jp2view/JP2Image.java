package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.io.IOException;
import java.net.URI;

import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_thread_env;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.imagedata.SubImage;
import org.helioviewer.jhv.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.ObserverMetaData;
import org.helioviewer.jhv.viewmodel.metadata.PixelBasedMetaData;
import org.helioviewer.jhv.viewmodel.view.ViewROI;
import org.helioviewer.jhv.viewmodel.view.jp2view.cache.JP2ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.cache.JP2ImageCacheStatusInitial;
import org.helioviewer.jhv.viewmodel.view.jp2view.cache.JP2ImageCacheStatusLocal;
import org.helioviewer.jhv.viewmodel.view.jp2view.cache.JP2ImageCacheStatusRemote;
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

public class JP2Image {

    private static final String[] SUPPORTED_EXTENSIONS = { ".jp2", ".jpx" };

    private final URI uri;

    private JHV_Kdu_cache cacheReader;
    private Kdu_cache cacheRender;

    private final int frameCount;
    private final int[] builtinLUT;

    private JPIPSocket socket;

    private final JP2ImageCacheStatus imageCacheStatus;

    private J2KReader reader;

    final MetaData[] metaDataList;

    /**
     * Constructor
     *
     * To open an image an URI must be given and this should be made unique. All
     * initialization for this object is done in the constructor or in methods
     * called by the constructor. Either the constructor throws an exception or
     * the image was opened successfully.
     *
     * @param _uri
     *            URI representing the location of the image
     */
    public JP2Image(URI _uri, JP2View _view) throws Exception {
        uri = _uri;

        String name = uri.getPath().toLowerCase();
        boolean supported = false;
        for (String ext : SUPPORTED_EXTENSIONS)
            if (name.endsWith(ext))
                supported = true;
        if (!supported)
            throw new JHV_KduException("File extension not supported");

        try {
            JP2ImageCacheStatusInitial initialCacheStatus = new JP2ImageCacheStatusInitial();
            String scheme = uri.getScheme().toLowerCase();
            switch (scheme) {
                case "http":
                case "jpip":
                    cacheReader = new JHV_Kdu_cache();
                    cacheRender = new Kdu_cache();
                    cacheRender.Attach_to(cacheReader);
                    // cache.Set_preferred_memory_limit(60 * 1024 * 1024);
                    initRemote(cacheReader, initialCacheStatus);
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
            frameCount = tempVar[0];

            builtinLUT = KakaduHelper.getLUT(jpx);

            metaDataList = new MetaData[frameCount];
            KakaduMeta.cacheMetaData(kduReader.getFamilySrc(), metaDataList);
            for (int i = 0; i < frameCount; i++) {
                if (metaDataList[i] == null)
                    metaDataList[i] = new PixelBasedMetaData(256, 256, i); // tbd real size
            }

            if (cacheReader != null) { // remote
                imageCacheStatus = new JP2ImageCacheStatusRemote(kduReader, frameCount - 1);
                imageCacheStatus.setVisibleStatus(0, initialCacheStatus.getVisibleStatus(0));
                reader = new J2KReader(_view, this);
            } else {
                imageCacheStatus = new JP2ImageCacheStatusLocal(kduReader, frameCount - 1);
            }
        } catch (KduException e) {
            e.printStackTrace();
            throw new JHV_KduException("Failed to create Kakadu machinery: " + e.getMessage(), e);
        }
    }

    private void initRemote(JHV_Kdu_cache cache, JP2ImageCacheStatus status) throws JHV_KduException {
        try {
            // Connect to the JPIP server and add the necessary initial data (the main header as well as the metadata) to cache
            socket = new JPIPSocket(uri, cache, status);

            JPIPResponse res;
            String req = JPIPQuery.create(JPIPConstants.META_REQUEST_LEN, "stream", "0", "metareq", "[*]!!");
            do {
                socket.send(req);
                res = socket.receive(cache, status);
            } while (!res.isResponseComplete());

            if (!cache.isDataBinCompleted(JPIPDatabinClass.MAIN_HEADER_DATABIN, 0, 0)) {
                req = JPIPQuery.create(JPIPConstants.MIN_REQUEST_LEN, "stream", "0");
                do {
                    socket.send(req);
                    res = socket.receive(cache, status);
                } while (!res.isResponseComplete() && !cache.isDataBinCompleted(JPIPDatabinClass.MAIN_HEADER_DATABIN, 0, 0));
            }
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
        MetaData m = metaDataList[frame];
        Region mr = m.getPhysicalRegion();
        Region r = ViewROI.updateROI(camera, vp, p, m);

        double ratio = 2 * camera.getWidth() / vp.height;
        int totalHeight = (int) (mr.height / ratio);

        ResolutionLevel res = imageCacheStatus.getResolutionSet(frame).getNextResolutionLevel(totalHeight, totalHeight);

        double currentMeterPerPixel = mr.width / res.width;
        int imageWidth = (int) Math.ceil(r.width / currentMeterPerPixel + 1); // +1 account for floor
        int imageHeight = (int) Math.ceil(r.height / currentMeterPerPixel + 1);

        double posX = (r.ulx - mr.ulx) / mr.width * res.width;
        double posY = (r.uly - mr.uly) / mr.height * res.height + 1;
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

        JP2ImageParameter params = new JP2ImageParameter(this, p, subImage, res, frame, factor);

        int level = res.level;
        if (!imageCacheStatus.imageComplete(frame, level) && (!Layers.isMoviePlaying() || level < oldLevel)) {
            imageCacheStatus.downgradeVisibleStatus(level);
            signalReader(params);
        }
        oldLevel = level;

        return params;
    }

    private int oldLevel = 1000;

    URI getURI() {
        return uri;
    }

    String getName() {
        MetaData m = metaDataList[0];
        if (m instanceof ObserverMetaData) {
            return ((ObserverMetaData) m).getFullName();
        } else {
            String name = uri.getPath();
            return name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.'));
        }
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

    public int getMaximumFrameNumber() {
        return frameCount - 1;
    }

    public ResolutionLevel getResolutionLevel(int frame, int level) {
        return imageCacheStatus.getResolutionSet(frame).getResolutionLevel(level);
    }

    int getNumComponents(int frame) {
        return imageCacheStatus.getResolutionSet(frame).numComps;
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

    void abolish() {
        if (isAbolished)
            return;
        isAbolished = true;

        new Thread(() -> {
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

    JHV_Kdu_cache getReaderCache() {
        return cacheReader;
    }

    JP2ImageCacheStatus getImageCacheStatus() {
        return imageCacheStatus;
    }

    LUT getDefaultLUT() {
        if (builtinLUT != null) {
            return new LUT(getName() + " built-in", builtinLUT/* , builtinLUT */);
        }
        return getAssociatedLUT();
    }

    private LUT getAssociatedLUT() {
        MetaData metaData = metaDataList[0];
        if (metaData instanceof HelioviewerMetaData) {
            return LUT.get((HelioviewerMetaData) metaData);
        }
        return null;
    }

    // very slow
    public String getXML(int boxNumber) {
        String xml = null;
        try {
            KakaduEngine kduTmp = new KakaduEngine(cacheReader, uri);
            xml = KakaduMeta.getXml(kduTmp.getFamilySrc(), boxNumber);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return xml;
    }

}
