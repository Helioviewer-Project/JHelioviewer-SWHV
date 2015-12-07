package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_region_compositor;
import kdu_jni.Kdu_thread_env;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.filters.lut.DefaultTable;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.io.APIResponseDump;
import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.ObserverMetaData;
import org.helioviewer.jhv.viewmodel.view.ViewROI;
import org.helioviewer.jhv.viewmodel.view.jp2view.cache.JP2ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.cache.JP2ImageCacheStatusLocal;
import org.helioviewer.jhv.viewmodel.view.jp2view.cache.JP2ImageCacheStatusRemote;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_Kdu_cache;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduEngine;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduHelper;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduUtils;

/**
 * This class can open JPEG2000 images. Modified to improve the JPIP
 * communication.
 *
 * @author caplins
 * @author Benjamin Wamsler
 * @author Juan Pablo
 */
public class JP2Image {

    /** An array of the file extensions this class currently supports */
    private static final String[] SUPPORTED_EXTENSIONS = { ".JP2", ".JPX" };

    /** This is the URI that uniquely identifies the image. */
    private final URI uri;

    /** This is the URI from whch the whole file can be downloaded via http */
    private final URI downloadURI;

    /** This is the object in which all transmitted data is stored */
    private JHV_Kdu_cache cacheReader;
    private Kdu_cache cacheRender;

    private KakaduEngine kduReader;

    /** The number of composition layers for the image. */
    private final int frameCount;
    private final int[] builtinLUT;

    private JPIPSocket socket;

    /**
     * The number of output components (should be the number of 8 bits
     * channels). Currently only value of 1 and 3 are supported (corresponding
     * to grayscale and RGB images).
     */
    private final int numComponents;

    private final JP2ImageCacheStatus imageCacheStatus;

    // Reader
    public enum ReaderMode {
        NEVERFIRE, ONLYFIREONCOMPLETE, ALWAYSFIREONNEWDATA, SIGNAL_RENDER_ONCE
    }

    private J2KReader reader;
    private ReaderMode readerMode = ReaderMode.ALWAYSFIREONNEWDATA;

    final MetaData[] metaDataList;

    /**
     * Constructor
     *
     * <p>
     * To open an image an URI must be given and this should be made unique. All
     * initialization for this object is done in the constructor or in methods
     * called by the constructor. Either the constructor throws an exception or
     * the image was opened successfully.
     *
     * @param newUri
     *            URI representing the location of the image
     * @param downloadURI
     *            In case the file should be downloaded to the local filesystem,
     *            use this URI as the source.
     * @throws IOException
     * @throws JHV_KduException
     */
    public JP2Image(URI _uri, URI _downloadURI) throws Exception {
        uri = _uri;
        downloadURI = _downloadURI;

        String name = uri.getPath().toUpperCase();
        boolean supported = false;
        for (String ext : SUPPORTED_EXTENSIONS)
            if (name.endsWith(ext))
                supported = true;
        if (!supported)
            throw new JHV_KduException("File extension not supported");

        try {
            String scheme = uri.getScheme().toUpperCase();
            if (scheme.equals("JPIP")) {
                cacheReader = new JHV_Kdu_cache();
                cacheRender = new Kdu_cache();
                cacheRender.Attach_to(cacheReader);
                // cache.Set_preferred_memory_limit(60 * 1024 * 1024);
                initRemote(cacheReader);
            } else if (scheme.equals("FILE")) {
                // nothing
            } else
                throw new JHV_KduException(scheme + " scheme not supported!");

            kduReader = new KakaduEngine(cacheReader, uri, null);

            // Retrieve the number of composition layers
            int[] tempVar = new int[1];
            kduReader.getJpxSource().Count_compositing_layers(tempVar);
            frameCount = tempVar[0];

            builtinLUT = KakaduHelper.getLUT(kduReader.getJpxSource());

            numComponents = KakaduHelper.getNumComponents(kduReader.getCompositor(), 0);

            metaDataList = new MetaData[frameCount];
            KakaduUtils.cacheMetaData(kduReader.getFamilySrc(), metaDataList);

            if (cacheReader != null) { // remote
                imageCacheStatus = new JP2ImageCacheStatusRemote(kduReader.getCompositor(), getMaximumFrameNumber());
            } else {
                imageCacheStatus = new JP2ImageCacheStatusLocal(kduReader.getCompositor(), getMaximumFrameNumber());
            }
        } catch (KduException ex) {
            ex.printStackTrace();
            throw new JHV_KduException("Failed to create Kakadu machinery: " + ex.getMessage(), ex);
        }
    }

    /**
     * Initializes the Jp2_family_src for a remote file. (JPIP comms
     * happen here).
     *
     * @throws JHV_KduException
     * @throws IOException
     */
    private void initRemote(JHV_Kdu_cache cache) throws JHV_KduException {
        // Create the JPIP-socket necessary for communications
        JPIPResponse res;
        socket = new JPIPSocket();

        try {
            // Connect to the JPIP server and add the first response to cache
            res = (JPIPResponse) socket.connect(uri);
            cache.addJPIPResponseData(res, null);

            // Download the necessary initial data
            boolean initialDataLoaded = false;
            int numTries = 0;

            do {
                try {
                    KakaduUtils.downloadInitialData(socket, cache);
                    initialDataLoaded = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    numTries++;
                    socket.close();
                    socket = new JPIPSocket();
                    socket.connect(uri);
                }
            } while (!initialDataLoaded && numTries < 5);
        } catch (SocketTimeoutException e) {
            throw new JHV_KduException("Timeout while communicating with the server:" + System.getProperty("line.separator") + e.getMessage(), e);
        } catch (IOException e) {
            throw new JHV_KduException("Error in the server communication:" + System.getProperty("line.separator") + e.getMessage(), e);
        } finally {
            Timer timer = new Timer("WaitForCloseSocket");
            timer.schedule(new TimerTask() {
                @Override
                public synchronized void run() {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            Log.error(">> JP2Image.initRemote() > Error closing socket.", e);
                        }
                        socket = null;
                    }
                }
            }, 5000);
        }
    }

    private KakaduEngine kduRender;

    Kdu_region_compositor getCompositor(Kdu_thread_env threadEnv) throws KduException, IOException {
        if (kduRender == null) {
            Thread.currentThread().setName("Render " + getName(0));
            kduRender = new KakaduEngine(cacheRender, uri, threadEnv);
        }
        return kduRender.getCompositor();
    }

    void destroyEngine() throws KduException {
        if (kduRender != null) {
            kduRender.destroy();
            kduRender = null;
        }
    }

    protected void startReader(JP2View view) {
        if (cacheReader != null) // remote
            try {
                reader = new J2KReader(view, this);
                reader.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    protected void signalReader(JP2ImageParameter params) {
        if (reader != null)
            reader.signalReader(params);
    }

    private JP2ImageParameter oldImageViewParams;

    // Recalculates the image parameters used within the jp2-package
    // Reader signals only for CURRENTFRAME*
    protected JP2ImageParameter calculateParameter(Camera camera, Viewport vp, Position.Q p, int frame, boolean fromReader) {
        MetaData m = metaDataList[frame];
        Region mr = m.getPhysicalRegion();
        Region r = ViewROI.updateROI(camera, vp, p, m);

        double ratio = 2 * camera.getWidth() / vp.height;
        int totalHeight = (int) (mr.height / ratio);

        ResolutionLevel res = imageCacheStatus.getResolutionSet(frame).getNextResolutionLevel(totalHeight, totalHeight);
        int viewportImageWidth = res.getResolutionBounds().width;
        int viewportImageHeight = res.getResolutionBounds().height;

        double currentMeterPerPixel = mr.width / viewportImageWidth;
        int imageWidth = (int) Math.round(r.width / currentMeterPerPixel);
        int imageHeight = (int) Math.round(r.height / currentMeterPerPixel);

        int imagePositionX = +(int) Math.round((r.ulx - mr.ulx) / mr.width * viewportImageWidth);
        int imagePositionY = -(int) Math.round((r.uly - mr.uly) / mr.height * viewportImageHeight);

        SubImage subImage = new SubImage(imagePositionX, imagePositionY, imageWidth, imageHeight, res.getResolutionBounds());

        JP2ImageParameter imageViewParams = new JP2ImageParameter(this, p, subImage, res, frame);

        boolean viewChanged = oldImageViewParams == null ||
                              !(imageViewParams.subImage.equals(oldImageViewParams.subImage) &&
                                imageViewParams.resolution.equals(oldImageViewParams.resolution));
        // ping reader
        if (viewChanged) {
            signalReader(imageViewParams);
        }

        // if (!fromReader && jp2Image.getImageCacheStatus().getImageStatus(frameNumber) == CacheStatus.COMPLETE && newImageViewParams.equals(oldImageViewParams)) {
        //    Displayer.display();
        //    return null;
        //}

        oldImageViewParams = imageViewParams;

        return imageViewParams;
    }

    /**
     * Sets the reader mode.
     *
     * <p>
     * The options are:
     * <ul>
     * <li>NEVERFIRE: The reader basically is disabled and never fires a
     * ChangeEvent.</li>
     * <li>ONLYFIREONCOMPLETE: The reader only fires a ChangeEvent, when the
     * current frame is loaded completely.</li>
     * <li>ALWAYSFIREONNEWDATA: Whenever new data is received, the reader fires
     * a ChangeEvent. This is the default value.</li>
     * </ul>
     *
     * @param readerMode
     * @see #getReaderMode()
     */
    public void setReaderMode(ReaderMode _readerMode) {
        readerMode = _readerMode;
    }

    /**
     * Returns the reader mode.
     *
     * @return Current reader mode.
     * @see #setReaderMode(ReaderMode)
     */
    ReaderMode getReaderMode() {
        return readerMode;
    }

    /**
     * Returns whether the image contains multiple frames.
     *
     * A image consisting of multiple frames is also called a 'movie'.
     *
     * @return True if the image contains multiple frames, false otherwise
     */
    protected boolean isMultiFrame() {
        return frameCount > 1;
    }

    /**
     * Returns the URI representing the location of the image.
     *
     * @return URI representing the location of the image.
     */
    protected URI getURI() {
        return uri;
    }

    /**
     * Returns the download uri the image.
     *
     * This is the uri from which the whole file can be downloaded and stored
     * locally
     *
     * @return download uri
     */
    public URI getDownloadURI() {
        return downloadURI;
    }

    protected String getName(int frame) {
        MetaData metaData = metaDataList[frame];
        if (metaData instanceof ObserverMetaData) {
            return ((ObserverMetaData) metaData).getFullName();
        } else {
            String name = getURI().getPath();
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
    protected JPIPSocket getSocket() {
        if (socket == null)
            return null;

        JPIPSocket output = socket;
        socket = null;
        return output;
    }

    // Returns the number of output components
    protected int getNumComponents() {
        return numComponents;
    }

    public int getMaximumFrameNumber() {
        return frameCount - 1;
    }

    /**
     * Gets the ResolutionSet object that contains the Resolution level
     * information.
     */
    public ResolutionSet getResolutionSet(int frame) {
        return imageCacheStatus.getResolutionSet(frame);
    }

    private volatile boolean isAbolished = false;

    // if instance was built before cancelling
    @Override
    protected void finalize() {
        if (!isAbolished) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    abolish();
                }
            });
        }
    }

    /**
     * Closes the image out. Destroys all objects and performs cleanup
     * operations. I use the 'abolish' name to distinguish it from what the
     * Kakadu library uses.
     */
    protected void abolish() {
        isAbolished = true;

        if (reader != null) {
            reader.abolish();
            reader = null;
        }

        APIResponseDump.getSingletonInstance().removeResponse(uri);

        try {
            destroyEngine();

            if (kduReader != null) {
                kduReader.destroy();
                kduReader = null;
            }
            if (cacheRender != null) {
                cacheRender.Close();
                cacheRender.Native_destroy();
            }
            if (cacheReader != null) {
                cacheReader.Close();
                cacheReader.Native_destroy();
            }
        } catch (KduException ex) {
            ex.printStackTrace();
        } finally {
            cacheRender = null;
            cacheReader = null;
        }
    }

    // Returns the cache reference
    protected JHV_Kdu_cache getCacheRef() {
        return cacheReader;
    }

    protected ImageCacheStatus getImageCacheStatus() {
        return imageCacheStatus;
    }

    LUT getDefaultLUT() {
        if (builtinLUT != null) {
            return new LUT("built-in", builtinLUT/* , builtinLUT */);
        }
        return getAssociatedLUT();
    }

    private LUT getAssociatedLUT() {
        MetaData metaData = metaDataList[0];
        if (metaData instanceof HelioviewerMetaData) {
            String colorKey = DefaultTable.getSingletonInstance().getColorTable((HelioviewerMetaData) metaData);
            if (colorKey != null) {
                return LUT.getStandardList().get(colorKey);
            }
        }
        return null;
    }

    // very slow
    public String getXML(int boxNumber) {
        String xml = null;
        try {
            KakaduEngine kduTmp = new KakaduEngine(cacheReader, uri, null);
            xml = KakaduUtils.getXml(kduTmp.getFamilySrc(), boxNumber);
            kduTmp.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return xml;
    }

}
