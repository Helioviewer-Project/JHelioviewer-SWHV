package org.helioviewer.viewmodel.view.jp2view;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

import kdu_jni.Jp2_palette;
import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_channel_mapping;
import kdu_jni.Kdu_codestream;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_ilayer_ref;
import kdu_jni.Kdu_istream_ref;
import kdu_jni.Kdu_region_compositor;
import kdu_jni.Kdu_thread_env;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.MathUtils;
import org.helioviewer.jhv.io.APIResponseDump;
import org.helioviewer.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.viewmodel.imagecache.LocalImageCacheStatus;
import org.helioviewer.viewmodel.imagecache.RemoteImageCacheStatus;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.jp2view.concurrency.BooleanSignal;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.viewmodel.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.viewmodel.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_Kdu_cache;
import org.helioviewer.viewmodel.view.jp2view.kakadu.KakaduUtils;

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

    protected int[] localIntBuffer = new int[0];

    /** This is the URI that uniquely identifies the image. */
    private final URI uri;

    /** This is the URI from whch the whole file can be downloaded via http */
    private final URI downloadURI;

    /** This is the object in which all transmitted data is stored */
    private JHV_Kdu_cache cache;

    /**
     * This extended version of Jp2_threadsafe_family_src can open any file
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

    /** The number of composition layers for the image. */
    private int frameCount;

    private int numLUTs = 0;
    private int[] builtinLUT = null;

    /** An object with all the resolution layer information. */
    private ResolutionSet resolutionSet;
    private int resolutionSetCompositionLayer = -1;

    /**
     * This is a little tricky variable to specify that the file contains
     * multiple frames
     */
    private boolean isJpx = false;

    protected MetaData[] metaDataList;

    private int referenceCounter = 0;
    private JPIPSocket socket;

    /**
     * The number of output components (should be the number of 8 bits
     * channels). Currently only value of 1 and 3 are supported (corresponding
     * to grayscale and RGB images).
     */
    private int numComponents;

    private Kdu_thread_env threadEnv;

    private ImageCacheStatus imageCacheStatus;

    // Reader
    public enum ReaderMode {
        NEVERFIRE, ONLYFIREONCOMPLETE, ALWAYSFIREONNEWDATA, SIGNAL_RENDER_ONCE
    }

    private J2KReader reader;
    protected BooleanSignal readerSignal = new BooleanSignal(false);
    private ReaderMode readerMode = ReaderMode.ALWAYSFIREONNEWDATA;

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
    public JP2Image(URI newUri, URI downloadURI) throws IOException, JHV_KduException, Exception {
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

        if (cache != null) {
            imageCacheStatus = new RemoteImageCacheStatus(getMaximumFrameNumber());
            cache.setImageCacheStatus(imageCacheStatus);
        } else {
            imageCacheStatus = new LocalImageCacheStatus(getMaximumFrameNumber());
        }

        metaDataList = new MetaData[frameCount];
        KakaduUtils.cacheMetaData(familySrc, metaDataList);
    }

    /**
     * Initializes the Jp2_threadsafe_family_src for a remote file. (JPIP comms
     * happen here).
     *
     * @throws JHV_KduException
     * @throws IOException
     */
    private void initRemote() throws JHV_KduException {
        // Create the JPIP-socket necessary for communications
        JPIPResponse res;
        socket = new JPIPSocket();

        try {
            // Connect to the JPIP server
            res = (JPIPResponse) socket.connect(uri);
            // Create the cache object and add the first response to it
            cache = new JHV_Kdu_cache();
            cache.addJPIPResponseData(res);

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

    private void configureLUT() throws KduException {
        Jp2_palette palette = jpxSrc.Access_codestream(0).Access_palette();

        numLUTs = palette.Get_num_luts();
        if (numLUTs == 0)
            return;

        int[] lut = new int[palette.Get_num_entries()];
        float[] red = new float[lut.length];
        float[] green = new float[lut.length];
        float[] blue = new float[lut.length];

        palette.Get_lut(0, red);
        palette.Get_lut(1, green);
        palette.Get_lut(2, blue);

        for (int i = 0; i < lut.length; i++) {
            lut[i] = 0xFF000000 | ((int) ((red[i] + 0.5f) * 0xFF) << 16) | ((int) ((green[i] + 0.5f) * 0xFF) << 8) | ((int) ((blue[i] + 0.5f) * 0xFF));
        }
        builtinLUT = lut;
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
            int numThreads = Kdu_global.Kdu_get_num_processors();
            threadEnv = new Kdu_thread_env();
            threadEnv.Create();
            for (int i = 1; i < numThreads; i++)
                threadEnv.Add_thread();

            compositor.Set_thread_env(threadEnv, null);
            compositor.Set_surface_initialization_mode(false);

            // I create references here so the GC doesn't try to collect the
            // Kdu_dims obj
            Kdu_dims ref1 = new Kdu_dims(), ref2 = new Kdu_dims();

            // A layer must be added to determine the image parameters
            compositor.Add_ilayer(0, ref1, ref2);

            {
                // Retrieve the number of composition layers
                {
                    int[] tempVar = new int[1];
                    jpxSrc.Count_compositing_layers(tempVar);
                    frameCount = tempVar[0];
                }

                Kdu_codestream stream = compositor.Access_codestream(compositor.Get_next_istream(new Kdu_istream_ref(), false, true));

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
            compositor.Remove_ilayer(new Kdu_ilayer_ref(), true);

            configureLUT();
        } catch (KduException ex) {
            ex.printStackTrace();
            throw new JHV_KduException("Failed to create Kakadu machinery: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void startReader(JHVJP2View view) {
        try {
            reader = new J2KReader(view, this);
            reader.start();
            readerSignal.signal();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    protected ReaderMode getReaderMode() {
        return readerMode;
    }

    /**
     * Returns true if the image is remote or if image is note open.
     *
     * @return True if the image is remote image, false otherwise
     */
    protected boolean isRemote() {
        return cache != null;
    }

    /**
     * Returns whether the image contains multiple frames.
     *
     * A image consisting of multiple frames is also called a 'movie'.
     *
     * @return True, if the image contains multiple frames, false otherwise
     */
    protected boolean isMultiFrame() {
        return isJpx && frameCount > 1;
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
    protected URI getDownloadURI() {
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
    public ResolutionSet getResolutionSet() {
        return resolutionSet;
    }

    /**
     * Increases the reference counter.
     *
     * This counter is used to count all views, which are using this JP2Image as
     * their data source. The counter is decreased when calling
     * {@link #abolish()}.
     */
    protected synchronized void addReference() {
        referenceCounter++;
    }

    /**
     * Closes the image out. Destroys all objects and performs cleanup
     * operations. I use the 'abolish' name to distinguish it from what the
     * Kakadu library uses.
     */
    protected synchronized void abolish() {
        referenceCounter--;
        if (referenceCounter > 0)
            return;
        if (referenceCounter < 0) {
            throw new IllegalStateException("JP2Image abolished more than once: " + uri);
        }

        if (reader != null) {
            reader.abolish();
            reader = null;
        }

        APIResponseDump.getSingletonInstance().removeResponse(uri);

        try {
            if (compositor != null) {
                compositor.Halt_processing();
                compositor.Remove_ilayer(new Kdu_ilayer_ref(), true);
                compositor.Native_destroy();
                threadEnv.Native_destroy();
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
            Kdu_codestream stream = compositor.Access_codestream(compositor.Get_next_istream(new Kdu_istream_ref(), false, true));

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
    /*
     * in preservation - not needed void deactivateColorLookupTable(int
     * numLayer) throws KduException { for (int i = 0; i < numLUTs; i++) {
     * jpxSrc.Access_layer(numLayer).Access_channels().Set_colour_mapping(i, 0,
     * -1, numLayer); } }
     */

    // Returns the cache reference
    protected JHV_Kdu_cache getCacheRef() {
        return cache;
    }

    protected ImageCacheStatus getImageCacheStatus() {
        return imageCacheStatus;
    }

    protected int getMaximumAccessibleFrameNumber() {
        return imageCacheStatus.getImageCachedPartiallyUntil();
    }

    // Returns the compositor reference
    protected Kdu_region_compositor getCompositorRef() {
        return compositor;
    }

    // Returns the built-in color lookup table.
    protected int[] getBuiltInLUT() {
        return builtinLUT;
    }

    public String getXML(int boxNumber) {
        String xml = null;

        try {
            xml = KakaduUtils.getXml(familySrc, boxNumber);
        } catch (JHV_KduException e) {
            e.printStackTrace();
        }
        return xml;
    }

}
