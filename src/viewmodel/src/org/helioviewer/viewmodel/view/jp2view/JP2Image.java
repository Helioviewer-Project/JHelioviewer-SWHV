package org.helioviewer.viewmodel.view.jp2view;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import kdu_jni.Jp2_palette;
import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_codestream_source;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_channel_mapping;
import kdu_jni.Kdu_codestream;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_region_compositor;
import kdu_jni.Kdu_tile;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.base.math.MathUtils;
import org.helioviewer.viewmodel.io.APIResponseDump;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.metadata.MetaDataConstructor;
import org.helioviewer.viewmodel.metadata.MetaDataContainer;
import org.helioviewer.viewmodel.view.cache.ImageCacheStatus;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.viewmodel.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.viewmodel.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_Kdu_cache;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_Kdu_thread_env;
import org.helioviewer.viewmodel.view.jp2view.kakadu.KakaduUtils;
import org.w3c.dom.CharacterData;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class can open JPEG2000 images, yeah baby! Modified to improve the JPIP
 * communication.
 *
 * @author caplins
 * @author Benjamin Wamsler
 * @author Juan Pablo
 */
public class JP2Image {

    /** An array of the file extensions this class currently supports */
    public static final String[] SUPPORTED_EXTENSIONS = { ".JP2", ".JPX" };

    private static int numJP2Images = 0;

    /** This is the URI that uniquely identifies the image. */
    private final URI uri;

    /** This is the URI from whch the whole file can be downloaded via http */
    private final URI downloadURI;

    /**
     * This is the object in which all transmitted data is stored. It has the
     * ability to write itself to disk, and read a relevant cache file from
     * disk.
     */
    private JHV_Kdu_cache cache;

    /**
     * The this extended version of Jp2_threadsafe_family_src can open any file
     * conforming to the jp2 specifications (.jp2, .jpx, .mj2, etc). The reason
     * for extending this class is that the Acquire/Release_lock() functions
     * needed to be implemented.
     */
    private Jp2_threadsafe_family_src familySrc = new Jp2_threadsafe_family_src();

    /** The Jpx_source object is capable of opening jp2 and jpx sources. */
    private Jpx_source jpxSrc = new Jpx_source();

    /**
     * The compositor object takes care of all the rendering via its process
     * function.
     */
    private Kdu_region_compositor compositor = new Kdu_region_compositor();

    /** The range of valid quality layers for the image. */
    private Interval<Integer> qLayerRange;

    /** The range of valid composition layer indices for the image. */
    private Interval<Integer> layerRange;

    /** An object with all the resolution layer information. */
    private ResolutionSet resolutionSet;
    private int resolutionSetCompositionLayer = -1;

    /**
     * This is a little tricky variable to specify that the file contains
     * multiple frames
     */
    private boolean isJpx = false;

    /** cache path */
    private static File cachePath;

    public MetaData[] metaDataList;
    private NodeList[] xmlCache;
    private hvXMLMetadata hvMetadata = new hvXMLMetadata();

    private JHVJP2View parentView;
    private final ReentrantLock lock = new ReentrantLock();
    private int referenceCounter = 0;
    private JPIPSocket socket;

    /**
     * The number of output components (should be the number of 8 bits
     * channels). Currently only value of 1 and 3 are supported (corresponding
     * to grayscale and RGB images).
     */
    private int numComponents;

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
     * @throws IOException
     * @throws JHV_KduException
     */
    public JP2Image(URI newUri) throws IOException, JHV_KduException {
        this(newUri, newUri);
    }

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
    public JP2Image(URI newUri, URI downloadURI) throws IOException, JHV_KduException {
        numJP2Images++;

        uri = newUri;
        this.downloadURI = downloadURI;
        String name = uri.getPath().toUpperCase();
        boolean supported = false;
        for (String ext : SUPPORTED_EXTENSIONS)
            if (name.endsWith(ext))
                supported = true;
        if (!supported)
            throw new JHV_KduException("File extension not supported.");

        isJpx = name.endsWith(".JPX");

        String scheme = uri.getScheme().toUpperCase();
        if (scheme.equals("JPIP"))
            initRemote();
        else if (scheme.equals("FILE"))
            initLocal();
        else
            throw new JHV_KduException(scheme + " scheme not supported!");

        createKakaduMachinery();

        metaDataList = new MetaData[layerRange.getEnd() + 1];
        xmlCache = new NodeList[layerRange.getEnd() + 1];

        cacheXMLs();
    }

    /**
     * Initializes the Jp2_threadsafe_family_src for a remote file. (JPIP comms
     * happen here).
     *
     * @throws JHV_KduException
     * @throws IOException
     */
    private void initRemote() throws JHV_KduException {
        // Creates the JPIP-socket necessary for communications
        JPIPResponse res;
        socket = new JPIPSocket();

        try {
            // Connects to the JPIP server, stores the first response in the res
            // variable
            res = (JPIPResponse) socket.connect(uri);

            // Parses the first JPIP response for the JPIP target-ID
            String jpipTargetID;

            if (res.getHeader("JPIP-tid") == null)
                throw new JHV_KduException("The target id was not sent by the server");
            else
                jpipTargetID = res.getHeader("JPIP-tid");

            if (jpipTargetID.contains("/")) {
                jpipTargetID = jpipTargetID.substring(jpipTargetID.lastIndexOf("/") + 1);
            }

            // Creates the cache object and adds the first response to it.
            cache = new JHV_Kdu_cache(jpipTargetID, cachePath, !isJpx);
            cache.addJPIPResponseData(res);

            // Download the necessary initial data if there isn't any cache file
            // yet
            if ((cache.getCacheFile() == null) || !cache.getCacheFile().exists()) {

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
            }

            familySrc.Open(cache);

        } catch (SocketTimeoutException e) {
            throw new JHV_KduException("Timeout while communicating with the server:" + System.getProperty("line.separator") + e.getMessage(), e);
        } catch (IOException e) {
            throw new JHV_KduException("Error in the server communication:" + System.getProperty("line.separator") + e.getMessage(), e);
        } catch (KduException e) {
            throw new JHV_KduException("Kakadu engine error opening the image", e);
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

    /**
     * Initializes the Jp2_threadsafe_family_src for a local file.
     *
     * @throws JHV_KduException
     * @throws IOException
     */
    private void initLocal() throws JHV_KduException, IOException {
        // Source is local so it must be a file
        File file = new File(uri);

        // Open the family source
        try {
            familySrc.Open(file.getCanonicalPath(), true);
        } catch (KduException ex) {
            throw new JHV_KduException("Failed to open familySrc", ex);
        }
    }

    /**
     * Creates the Kakadu objects and sets all the data-members in this object.
     *
     * @throws JHV_KduException
     */
    private void createKakaduMachinery() throws JHV_KduException {
        // The amount of cache to allocate to each codestream
        final int CODESTREAM_CACHE_THRESHOLD = 1024 * 256;

        try {
            // Open the jpx source from the family source
            jpxSrc.Open(familySrc, false);

            // I don't know if I should be using the codestream in a persistent
            // mode or not...
            compositor.Create(jpxSrc, CODESTREAM_CACHE_THRESHOLD);
            compositor.Set_thread_env(null, 0);

            // I create references here so the GC doesn't try to collect the
            // Kdu_dims obj
            Kdu_dims ref1 = new Kdu_dims(), ref2 = new Kdu_dims();

            // A layer must be added to determine the image parameters
            compositor.Add_compositing_layer(0, ref1, ref2);

            {
                // Retrieve the number of composition layers
                {
                    int[] tempVar = new int[1];
                    jpxSrc.Count_compositing_layers(tempVar);
                    layerRange = new Interval<Integer>(0, tempVar[0] - 1);
                }

                Kdu_codestream stream = compositor.Access_codestream(compositor.Get_next_codestream(0, false, true));

                {
                    Kdu_coords coordRef = new Kdu_coords();
                    Kdu_tile tile = stream.Open_tile(coordRef);

                    // Retrieve the number of quality layers.
                    qLayerRange = new Interval<Integer>(1, tile.Get_num_layers());

                    // Cleanup
                    tile.Close();
                    tile = null;
                }

                // Retrieve the number of components
                {
                    // Since it gets tricky here I am just grabbing a bunch of
                    // values
                    // and taking the max of them. It is acceptable to think
                    // that an
                    // image is color when its not monochromatic, but not the
                    // other way
                    // around... so this is just playing it safe.
                    Kdu_channel_mapping cmap = new Kdu_channel_mapping();
                    cmap.Configure(stream);

                    int maxComponents = MathUtils.max(cmap.Get_num_channels(), cmap.Get_num_colour_channels(), stream.Get_num_components(true), stream.Get_num_components(false));

                    // numComponents = maxComponents == 1 ? 1 : 3;
                    numComponents = maxComponents; // With new file formats we
                    // may have 2 components

                    cmap.Clear();
                    cmap.Native_destroy();
                    cmap = null;
                }

                // Cleanup
                stream = null;
            }

            updateResolutionSet(0);

            // Remove the layer that was added
            compositor.Remove_compositing_layer(-1, true);

        } catch (KduException ex) {
            ex.printStackTrace();
            throw new JHV_KduException("Failed to create Kakadu machinery: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Sets the parent view of this image.
     *
     * The parent view is used to determine the current frame when accessing
     * meta data.
     *
     * @param _parentView
     *            The new parent view
     * @see #getParentView()
     * @see #getValueFromXML(String, String)
     */
    public void setParentView(JHVJP2View _parentView) {
        parentView = _parentView;
    }

    /**
     * Returns the parent view of this image.
     *
     * The parent view is used to determine the current frame when accessing
     * meta data.
     *
     * @return The current parent view
     * @see #setParentView(JHVJP2View)
     * @see #getValueFromXML(String, String)
     */
    public JHVJP2View getParentView() {
        return parentView;
    }

    /**
     * Returns true if the image is remote or if image is note open.
     *
     * @return True if the image is remote image, false otherwise
     */
    public boolean isRemote() {
        return cache != null;
    }

    /**
     * Returns whether the image contains multiple frames.
     *
     * A image consisting of multiple frames is also called a 'movie'.
     *
     * @return True, if the image contains multiple frames, false otherwise
     */
    public boolean isMultiFrame() {
        int frameCount = layerRange.getEnd() + 1;
        return isJpx && frameCount > 1;
    }

    public Jp2_threadsafe_family_src getFamilySrc() {
        return familySrc;
    }

    private void cacheXMLs() throws JHV_KduException {
        String xml;
        int num = layerRange.getEnd() + 1;

        KakaduUtils.parseAllXMLs(familySrc, xmlCache, num);

        for (int i = 0; i < num; i++) {
            hvMetadata.setNode(xmlCache[i]);
            metaDataList[i] = MetaDataConstructor.getMetaData(hvMetadata);
        }
    }

    private static class hvXMLMetadata implements MetaDataContainer {

        private static NodeList nodeList;

        public void setNode(NodeList nodeList) {
            this.nodeList = nodeList;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String get(String key) {
            try {
                String value = getValueFromXML(nodeList, key, "fits");
                return value;
            } catch (JHV_KduException e) {
                if (e.getMessage() == "XML data incomplete" || e.getMessage().toLowerCase().contains("box not open")) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {}

                    get(key);
                } else if (e.getMessage() != "No XML data present") {
                    e.printStackTrace();
                }
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double tryGetDouble(String key) {
            String string = get(key);
            if (string != null) {
                try {
                    return Double.parseDouble(string);
                } catch (NumberFormatException e) {
                    Log.warn("NumberFormatException while trying to parse value \"" + string + "\" of key " + key);
                    return 0.0;
                }
            }
            return 0.0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int tryGetInt(String key) {
            String string = get(key);
            if (string != null) {
                try {
                    return Integer.parseInt(string);
                } catch (NumberFormatException e) {
                    Log.warn("NumberFormatException while trying to parse value \"" + string + "\" of key " + key);
                    return 0;
                }
            }
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPixelHeight() {
            return tryGetInt("NAXIS2");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPixelWidth() {
            return tryGetInt("NAXIS1");
        }

    }

    /**
     * Method that returns value of specified _keyword from specified _box.
     *
     * @param _keyword
     * @param _box
     * @param _boxNumber
     * @throws JHV_KduException
     */
    private static String getValueFromXML(NodeList nodeList, String _keyword, String _box) throws JHV_KduException {
        try {
            NodeList nodes = ((Element) nodeList.item(0)).getElementsByTagName(_box);
            NodeList value = ((Element) nodeList.item(0)).getElementsByTagName(_keyword);
            Element line = (Element) value.item(0);

            if (line == null)
                return null;

            Node child = line.getFirstChild();
            if (child instanceof CharacterData) {
                CharacterData cd = (CharacterData) child;
                return cd.getData();
            }
            return null;
        } catch (Exception e) {
            throw new JHV_KduException("Failed parsing XML data", e);
        }
    }

    /**
     * Returns the URI representing the location of the image.
     *
     * @return URI representing the location of the image.
     */
    public URI getURI() {
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

    /**
     * Returns the socket, if in remote mode.
     *
     * The socket is returned only one time. After calling this function for the
     * first time, it will always return null.
     *
     * @return Socket connected to the server
     */
    public JPIPSocket getSocket() {
        if (socket == null)
            return null;

        JPIPSocket output = socket;
        socket = null;
        return output;
    }

    /** Returns the number of output components. */
    public int getNumComponents() {
        return numComponents;
    }

    /** Returns the an interval of the valid composition layer indices. */
    public Interval<Integer> getCompositionLayerRange() {
        return layerRange;
    }

    /** Returns the an interval of the valid quality layer values */
    public Interval<Integer> getQualityLayerRange() {
        return qLayerRange;
    }

    /**
     * Gets the ResolutionSet object that contains the Resolution level
     * information.
     */
    public ResolutionSet getResolutionSet() {
        return resolutionSet;
    }

    public static void setCachePath(File newCachePath) {
        cachePath = newCachePath;
    }

    public static File getCachePath() {
        return cachePath;
    }

    /**
     * Increases the reference counter.
     *
     * This counter is used to count all views, which are using this JP2Image as
     * their data source. The counter is decreased when calling
     * {@link #abolish()}.
     */
    public synchronized void addReference() {
        JHV_Kdu_thread_env.getSingletonInstance().updateNumThreads();
        referenceCounter++;
    }

    /**
     * Closes the image out. Destroys all objects and performs cleanup
     * operations. I use the 'abolish' name to distinguish it from what the
     * Kakadu library uses.
     */
    public synchronized void abolish() {
        referenceCounter--;
        if (referenceCounter > 0)
            return;
        if (referenceCounter < 0) {
            throw new IllegalStateException("JP2Image abolished more than once: " + uri);
        }
        numJP2Images--;

        APIResponseDump.getSingletonInstance().removeResponse(uri);

        try {
            if (compositor != null) {
                compositor.Set_thread_env(null, 0);
                compositor.Remove_compositing_layer(-1, true);
                compositor.Native_destroy();
            }
            if (jpxSrc != null) {
                jpxSrc.Close();
                jpxSrc.Native_destroy();
            }
            if (familySrc != null) {
                familySrc.Close();
                familySrc.Native_destroy();
            }
            if (cache != null) {
                cache.Close();
                cache.Native_destroy();

                JHV_Kdu_cache.updateCacheDirectory(cachePath);
            }
        } catch (KduException ex) {
            ex.printStackTrace();
        } finally {
            compositor = null;
            jpxSrc = null;
            familySrc = null;
            cache = null;
        }
    }

    protected boolean updateResolutionSet(int compositionLayerCurrentlyInUse) {
        if (resolutionSetCompositionLayer == compositionLayerCurrentlyInUse)
            return false;

        resolutionSetCompositionLayer = compositionLayerCurrentlyInUse;

        try {
            Kdu_codestream stream = compositor.Access_codestream(compositor.Get_next_codestream(0, false, true));

            int maxDWT = stream.Get_min_dwt_levels();

            compositor.Set_scale(false, false, false, 1.0f);
            Kdu_dims dims = new Kdu_dims();
            if (!compositor.Get_total_composition_dims(dims))
                return false;

            if (resolutionSet != null) {
                int pixelWidth = resolutionSet.getResolutionLevel(0).getResolutionBounds().width;
                int pixelHeight = resolutionSet.getResolutionLevel(0).getResolutionBounds().height;

                Kdu_coords size = dims.Access_size();
                if (size.Get_x() == pixelWidth && size.Get_y() == pixelHeight)
                    return false;
            }

            resolutionSet = new ResolutionSet(maxDWT + 1);
            resolutionSet.addResolutionLevel(0, KakaduUtils.kdu_dimsToRect(dims));

            for (int i = 1; i <= maxDWT; i++) {
                compositor.Set_scale(false, false, false, 1.0f / (1 << i));
                dims = new Kdu_dims();
                if (!compositor.Get_total_composition_dims(dims))
                    break;
                resolutionSet.addResolutionLevel(i, KakaduUtils.kdu_dimsToRect(dims));
            }
        } catch (KduException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Deactivates the internal color lookup table for the given composition
     * layer.
     *
     * It is not allowed to call this function for a layer which is not loaded
     * yet.
     *
     * @param numLayer
     *            composition layer to deactivate internal color lookup for
     */
    void deactivateColorLookupTable(int numLayer) {
        try {
            lock.lock();
            Jpx_codestream_source jpxStream = jpxSrc.Access_codestream(0);
            Jp2_palette palette = jpxStream.Access_palette();

            for (int i = 0; i < palette.Get_num_luts(); i++) {
                jpxSrc.Access_layer(numLayer).Access_channels().Set_colour_mapping(i, 0, -1, numLayer);
            }

        } catch (KduException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    Lock getLock() {
        return lock;
    }

    /** Returns the cache reference */
    JHV_Kdu_cache getCacheRef() {
        return cache;
    }

    /** Sets the ImageCacheStatus */
    void setImageCacheStatus(ImageCacheStatus imageCacheStatus) {
        if (cache != null)
            cache.setImageCacheStatus(imageCacheStatus);
    }

    /** Returns the compositor reference */
    Kdu_region_compositor getCompositorRef() {
        return compositor;
    }

    /** Returns the jpx source */
    Jpx_source getJpxSource() {
        return jpxSrc;
    }

    /**
     * Returns the number of JP2Image instances currently in use.
     *
     * @return Number of JP2Image instances currently in use
     */
    static int numJP2ImagesInUse() {
        return numJP2Images;
    }

}
