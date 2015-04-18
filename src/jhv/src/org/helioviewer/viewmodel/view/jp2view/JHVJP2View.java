package org.helioviewer.viewmodel.view.jp2view;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.net.URI;

import kdu_jni.Jp2_palette;
import kdu_jni.KduException;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.RenderListener;
import org.helioviewer.jhv.gui.filters.lut.DefaultTable;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.metadata.ObserverMetaData;
import org.helioviewer.viewmodel.metadata.PixelBasedMetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.jp2view.J2KRender.RenderReasons;
import org.helioviewer.viewmodel.view.jp2view.concurrency.BooleanSignal;
import org.helioviewer.viewmodel.view.jp2view.concurrency.ReasonSignal;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;
import org.helioviewer.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.viewmodel.view.jp2view.image.SubImage;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.viewportimagesize.StaticViewportImageSize;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSizeAdapter;

/**
 * Implementation of View for JPG2000 images.
 *
 * <p>
 * This class represents the gateway to the heart of the helioviewer project. It
 * is responsible for reading and decoding JPG2000 images. Therefore, it manages
 * two Threads: One Thread for communicating with the JPIP server, the other one
 * for decoding the images.
 *
 * <p>
 * For decoding the images, the kakadu library is used. Unfortunately, kakaku is
 * not threadsafe, so be careful! Although kakadu is a and highly optimized
 * library, the decoding process is the bottleneck for speeding up the
 * application.
 *
 */
public class JHVJP2View extends AbstractView implements JP2View, RenderListener {

    public enum ReaderMode {
        NEVERFIRE, ONLYFIREONCOMPLETE, ALWAYSFIREONNEWDATA, SIGNAL_RENDER_ONCE
    };

    // Member related to the view chain
    protected Viewport viewport;
    protected Region region, lastRegion;

    // Member related to JP2
    protected boolean isMainView;
    protected boolean isPersistent;
    protected JP2Image jp2Image;
    public volatile JP2ImageParameter imageViewParams;

    // Reader
    protected J2KReader reader;
    protected ReaderMode readerMode = ReaderMode.ALWAYSFIREONNEWDATA;
    final BooleanSignal readerSignal = new BooleanSignal(false);

    // Renderer
    protected J2KRender render;
    final ReasonSignal<RenderReasons> renderRequestedSignal = new ReasonSignal<RenderReasons>();

    // Renderer-ThreadGroup - This group is necessary to identify all renderer
    // threads
    public static final ThreadGroup renderGroup = new ThreadGroup("J2KRenderGroup");

    private ImageData previousImageData;
    private ImageData baseDifferenceImageData;

    /**
     * Default constructor.
     *
     * <p>
     * When the view is not marked as a main view, it is assumed, that the view
     * will only serve one single image and will not have to perform any kind of
     * update any more. The effect of this assumption is, that the view will not
     * try to reconnect to the JPIP server when the connection breaks and that
     * there will be no other timestamps used than the first one.
     *
     * @param isMainView
     *            Whether the view is a main view or not
     */
    public JHVJP2View(boolean isMainView) {
        this.isMainView = isMainView;
        if (isMainView) {
            Displayer.addRenderListener(this);
        }
        isPersistent = isMainView;
    }

    /**
     * Returns the JPG2000 image managed by this class.
     *
     * @return JPG2000 image
     */
    public JP2Image getJP2Image() {
        return jp2Image;
    }

    /**
     * Sets the JPG2000 image used by this class.
     *
     * This functions sets up the whole infrastructure needed for using the
     * image, including the two threads.
     *
     * <p>
     * Thus, this functions also works as a constructor.
     *
     * @param newJP2Image
     */
    public void setJP2Image(JP2Image newJP2Image) {

        if (jp2Image != null && reader != null) {
            abolish();
        }

        MetaData metaData = newJP2Image.metaDataList[0];
        if (region == null) {
            if (!(metaData instanceof PixelBasedMetaData)) {
                region = StaticRegion.createAdaptedRegion(metaData.getPhysicalLowerLeft(), metaData.getPhysicalImageSize());
            }
            if (viewport == null) {
                viewport = StaticViewport.createAdaptedViewport(100, 100);
            }
        }
        jp2Image = newJP2Image;

        imageViewParams = calculateParameter(newJP2Image.getQualityLayerRange().getEnd(), 0);

        if (isMainView) {
            jp2Image.setParentView(this);
        }
        jp2Image.addReference();

        try {
            reader = new J2KReader(this);
            render = new J2KRender(this);
            startDecoding();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setStartLUT();
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
    public void setReaderMode(ReaderMode readerMode) {
        this.readerMode = readerMode;
    }

    /**
     * Returns the reader mode.
     *
     * @return Current reader mode.
     * @see #setReaderMode(ReaderMode)
     */
    public ReaderMode getReaderMode() {
        return readerMode;
    }

    /**
     * Sets, whether this view is persistent.
     *
     * This value only has effect, when the image is a remote image. A
     * persistent view will close its socket after receiving the first frame. By
     * default, main views are not persistent.
     *
     * @param isPersistent
     *            True, if this view is persistent
     * @see #isPersistent
     */
    public void setPersistent(boolean isPersistent) {
        this.isPersistent = isPersistent;
    }

    /**
     * Returns the built-in color lookup table.
     *
     */
    public int[] getBuiltInLUT() {
        try {
            jp2Image.getLock().lock();
            Jp2_palette palette = jp2Image.getJpxSource().Access_codestream(0).Access_palette();

            if (palette.Get_num_luts() == 0)
                return null;

            int[] lut = new int[palette.Get_num_entries()];

            float[] red = new float[palette.Get_num_entries()];
            float[] green = new float[palette.Get_num_entries()];
            float[] blue = new float[palette.Get_num_entries()];

            palette.Get_lut(0, red);
            palette.Get_lut(1, green);
            palette.Get_lut(2, blue);

            for (int i = 0; i < lut.length; i++) {
                lut[i] = 0xFF000000 | ((int) ((red[i] + 0.5f) * 0xFF) << 16) | ((int) ((green[i] + 0.5f) * 0xFF) << 8) | ((int) ((blue[i] + 0.5f) * 0xFF));
            }

            return lut;

        } catch (KduException e) {
            e.printStackTrace();
        } finally {
            jp2Image.getLock().unlock();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public boolean setViewport(Viewport v) {
        boolean viewportChanged = (viewport == null ? v == null : !viewport.equals(v));
        viewport = v;

        if (setImageViewParams(calculateParameter())) {
            return true;
        } else if (viewportChanged && imageViewParams.resolution.getZoomLevel() == jp2Image.getResolutionSet().getMaxResolutionLevels()) {
            renderRequestedSignal.signal(RenderReasons.OTHER);
            return true;
        }

        return viewportChanged;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageData getSubimageData() {
        return imageData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaData getMetaData() {
        int frameNumber = 0;
        if (imageData != null) {
            frameNumber = imageData.getFrameNumber();
        }

        return jp2Image.metaDataList[frameNumber];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCurrentNumQualityLayers() {
        return imageViewParams.qualityLayers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaximumNumQualityLayers() {
        return jp2Image.getQualityLayerRange().getEnd();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNumQualityLayers(int newNumQualityLayers) {
        if (newNumQualityLayers >= 1 && newNumQualityLayers <= getMaximumNumQualityLayers()) {
            setImageViewParams(null, null, newNumQualityLayers, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Region getRegion() {
        Region result = lastRegion;
        return result;
    }

    /**
     * @return newest region, even if no new data has been retrieved, yet
     */
    public Region getNewestRegion() {
        return region;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setRegion(Region r) {
        boolean changed = region == null ? r == null : !region.equals(r);
        region = r;
        changed |= setImageViewParams(calculateParameter());

        return changed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        int frameNumber = 0;
        if (imageData != null) {
            frameNumber = imageData.getFrameNumber();
        }

        MetaData metaData = jp2Image.metaDataList[frameNumber];
        if (metaData instanceof ObserverMetaData) {
            ObserverMetaData observerMetaData = (ObserverMetaData) metaData;
            return observerMetaData.getFullName();
        } else {
            String name = jp2Image.getURI().getPath();
            return name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.'));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getUri() {
        return jp2Image.getURI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getDownloadURI() {
        return jp2Image.getDownloadURI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRemote() {
        return jp2Image.isRemote();
    }

    /**
     * Returns whether the reader is connected to a JPIP server or not.
     *
     * @return True, if connected to a JPIP server, false otherwise
     */
    public boolean isConnectedToJPIP() {
        if (reader != null)
            return reader.isConnected();

        return false;
    }

    /**
     * Destroy the resources associated with this object.
     */
    public void abolish() {
        if (reader != null) {
            reader.abolish();
            reader = null;
        }
        if (render != null) {
            render.abolish(jp2Image);
            render = null;
        }
        //jp2Image.abolish();
    }

    /**
     * Starts the J2KReader/J2KRender threads.
     */
    protected void startDecoding() {
        render.start();
        reader.start();
        readerSignal.signal();
    }

    /**
     * Recalculates the image parameters.
     *
     * <p>
     * This function maps between the set of parameters used within the view
     * chain and the set of parameters used within the jp2-package.
     *
     * <p>
     * To achieve this, calls {@link #calculateParameter(int, int)} with the
     * currently used number of quality layers and the first frame.
     *
     * @return Set of parameters used within the jp2-package
     */
    protected JP2ImageParameter calculateParameter() {
        return calculateParameter(getCurrentNumQualityLayers(), 0);
    }

    /**
     * Recalculates the image parameters.
     *
     * This function maps between the set of parameters used within the view
     * chain and the set of parameters used within the jp2-package.
     *
     * <p>
     * To achieve this, calls
     * {@link #calculateParameter(Viewport, Region, int, int)} with the current
     * region and viewport and the given number of quality layers and frame
     * number.
     *
     * @param numQualityLayers
     *            Number of quality layers to use
     * @param frameNumber
     *            Frame number to show (has to be 0 for single images)
     * @return Set of parameters used within the jp2-package
     */
    protected JP2ImageParameter calculateParameter(int numQualityLayers, int frameNumber) {
        return calculateParameter(viewport, region, numQualityLayers, frameNumber);
    }

    /**
     * Recalculates the image parameters.
     *
     * This function maps between the set of parameters used within the view
     * chain and the set of parameters used within the jp2-package.
     *
     * <p>
     * To achieve this, calculates the set of parameters used within the
     * jp2-package according to the given requirements from the view chain.
     *
     * @param v
     *            Viewport the image will be displayed in
     * @param r
     *            Physical region
     * @param numQualityLayers
     *            Number of quality layers to use
     * @param frameNumber
     *            Frame number to show (has to be 0 for single images)
     * @return Set of parameters used within the jp2-package
     */
    protected JP2ImageParameter calculateParameter(Viewport v, Region r, int numQualityLayers, int frameNumber) {
        MetaData metaData = jp2Image.metaDataList[frameNumber];
        int h = 512;
        int w = 512;
        if (GL3DState.get() != null && GL3DState.get().getActiveCamera() != null) {
            GL3DCamera cam = GL3DState.get().getActiveCamera();
            h = (int) (r.getHeight() / (cam.getCameraWidth() * 2.) * GL3DState.get().getViewportHeight() * 2);
            w = (int) (r.getWidth() / (cam.getCameraWidth() * 2.) * GL3DState.get().getViewportHeight() * 2);
        }

        int totalHeight = (int) (h * metaData.getPhysicalImageWidth() / r.getWidth());
        int totalWidth = (int) (w * metaData.getPhysicalImageWidth() / r.getWidth());
        ResolutionLevel res = jp2Image.getResolutionSet().getNextResolutionLevel(new Dimension(totalWidth, totalHeight));

        double imageMeterPerPixel = metaData.getPhysicalImageWidth() / res.getResolutionBounds().getWidth();

        int imageWidth = (int) Math.round(r.getWidth() / imageMeterPerPixel);
        int imageHeight = (int) Math.round(r.getHeight() / imageMeterPerPixel);

        Vector2dInt imagePosition = ViewHelper.calculateInnerViewportOffset(r, metaData.getPhysicalRegion(), new ViewportImageSizeAdapter(new StaticViewportImageSize(res.getResolutionBounds().width, res.getResolutionBounds().height)));

        SubImage subImage = new SubImage(imagePosition.getX(), imagePosition.getY(), imageWidth, imageHeight);
        return new JP2ImageParameter(subImage, res, numQualityLayers, frameNumber);
    }

    /**
     * Sets the current ImageViewParams to the ones specified. Any parameter
     * that should remain unchanged should be specified null. (Isn't
     * auto-unboxing just convenient as hell sometimes?)
     *
     * @param _roi
     *            Pixel region to display
     * @param _resolution
     *            Resolution level to use
     * @param _qualityLayers
     *            Number of quality layers to use
     * @param _compositionLayer
     *            Frame number to use
     * @param _doReload
     *            If true, the image is reloaded after updating the parameters
     * @return true, if the parameters actually has changed, false otherwise
     */
    protected boolean setImageViewParams(SubImage _roi, ResolutionLevel _resolution, Integer _qualityLayers, Integer _compositionLayer, boolean _doReload) {
        return setImageViewParams(new JP2ImageParameter((_roi == null ? imageViewParams.subImage : _roi), (_resolution == null ? imageViewParams.resolution : _resolution), (_qualityLayers == null ? imageViewParams.qualityLayers : _qualityLayers), (_compositionLayer == null ? imageViewParams.compositionLayer : _compositionLayer)), _doReload);
    }

    /**
     * Method calls setImageViewParams(SubImage, ResolutionLevel, Integer,
     * Integer, boolean) with the boolean set to true.
     *
     * @param _roi
     *            Pixel region to display
     * @param _resolution
     *            Resolution level to use
     * @param _qualityLayers
     *            Number of quality layers to use
     * @param _compositionLayer
     *            Frame number to use
     * @return true, if the parameters actually has changed, false otherwise
     */
    protected boolean setImageViewParams(SubImage _roi, ResolutionLevel _resolution, Integer _qualityLayers, Integer _compositionLayer) {
        return setImageViewParams(_roi, _resolution, _qualityLayers, _compositionLayer, true);
    }

    /**
     * Calls {@link #setImageViewParams(JP2ImageParameter, boolean)} with the
     * boolean set to true.
     *
     * @param newParams
     *            New set of parameters to use
     * @return true, if the parameters actually has changed, false otherwise
     */
    protected boolean setImageViewParams(JP2ImageParameter newParams) {
        return setImageViewParams(newParams, true);
    }

    /**
     * Sets the image parameters, if the given ones are valid.
     *
     * Also, triggers an update of the image using the new set of parameters, if
     * desired.
     *
     * @param newParams
     *            New set of parameters to use
     * @param reload
     *            if true, triggers an update of the image
     * @return true, if the parameters actually has changed, false otherwise
     */
    protected boolean setImageViewParams(JP2ImageParameter newParams, boolean reload) {
        if (imageViewParams.equals(newParams)) {
            return false;
        }

        if (newParams.subImage.width == 0 || newParams.subImage.height == 0) {
            if (imageData == null) {
                return false;
            }
            // setSubimageData(null, null, 0, 1.);
            return true;
        }
        imageViewParams = newParams;

        if (reload) {
            readerSignal.signal();
        }
        return true;
    }

    /*
     * NOTE: The following section is for communications with the two threads,
     * J2KReader and J2KRender. Thus, the visibility is set to "default" (also
     * known as "package"). These functions should not be used by any other
     * class.
     */

    /**
     * Returns the current set of parameters.
     *
     * @return Current set of parameters
     */
    public JP2ImageParameter getImageViewParams() {
        return imageViewParams;
    }

    /**
     * Returns the current internal region (before decoding).
     *
     * @return current internal region
     */
    Region getRegionPrelook() {
        return region;
    }

    /**
     * Sets the new image data for the given region.
     *
     * <p>
     * This function is used as a callback function which is called by
     * {@link J2KRender} when it has finished decoding an image.
     *
     * @param newImageData
     *            New image data
     * @param roi
     *            Area the image contains, to find the corresponding
     * @param compositionLayer
     *            Composition Layer rendered, to update meta data
     *            {@link org.helioviewer.viewmodel.region.Region}
     */
    void setSubimageData(ImageData newImageData, SubImage roi, int compositionLayer, double zoompercent, boolean fullyLoaded) {
        MetaData metaData = jp2Image.metaDataList[compositionLayer];

        ImmutableDateTime dtc = null;
        if (metaData instanceof ObserverMetaData)
            dtc = metaData.getDateTime();

        if (compositionLayer == 0) {
            this.baseDifferenceImageData = newImageData;
        }
        newImageData.setFrameNumber(compositionLayer);

        if (metaData instanceof HelioviewerMetaData) {
            HelioviewerMetaData hvmd = (HelioviewerMetaData) metaData;
            newImageData.setRegion(hvmd.roiToRegion(roi, zoompercent));
            newImageData.setDateMillis(hvmd.getDateTime().getMillis());
        }
        newImageData.setFullyLoaded(fullyLoaded);

        if (this.imageData != null && compositionLayer == this.imageData.getFrameNumber() + 1) {
            this.previousImageData = this.imageData;
        } else if (this.previousImageData != null && this.previousImageData.getFrameNumber() - compositionLayer > 2) {
            this.previousImageData = newImageData;
        } else if (this.imageData != null && compositionLayer == this.imageData.getFrameNumber() - 1) {
            this.previousImageData = this.imageData;
        }
        newImageData.setMETADATA(metaData);

        this.imageData = newImageData;

        fireFrameChanged(this, dtc);
    }

    private void fireFrameChanged(JHVJP2View aView, ImmutableDateTime aDateTime) {
        EventQueue.invokeLater(new Runnable() {
            private JHVJP2View theView;
            private ImmutableDateTime theDateTime;

            @Override
            public void run() {
                Displayer.fireFrameChanged(theView, theDateTime);
            }

            public Runnable init(JHVJP2View theView, ImmutableDateTime theDateTime) {
                this.theView = theView;
                this.theDateTime = theDateTime;
                return this;
            }
        }.init(aView, aDateTime));
    }

    /**
     * Returns whether this view is used as a main view.
     *
     * @return Whether this view is used as a main view
     */
    boolean isMainView() {
        return isMainView;
    }

    /**
     * Returns, whether this view is persistent.
     *
     * @return True, if this view is persistent, false otherwise.
     * @see #setPersistent(boolean)
     */
    boolean isPersistent() {
        return isPersistent;
    }

    /**
     * Recalculate the image parameters.
     *
     * This might be useful, if some assumption have changed, such as the
     * resolution set.
     */
    void updateParameter() {
        setImageViewParams(calculateParameter());
    }

    @Override
    public ImageData getImageData() {
        return this.imageData;
    }

    @Override
    public ImageData getPreviousImageData() {
        return this.previousImageData;
    }

    @Override
    public ImageData getBaseDifferenceImageData() {
        return this.baseDifferenceImageData;
    }

    public void setPreviousImageData(ImageData previousImageData) {
        this.previousImageData = previousImageData;
    }

    public void refresh() {
        readerSignal.signal();
    }

    @Override
    public void render() {
        renderRequestedSignal.signal(RenderReasons.NEW_DATA);
    }

    public void removeRenderListener() {
        Displayer.removeRenderListener(this);
    }

    @Override
    public void setStartLUT() {
        int[] builtIn = this.getBuiltInLUT();
        if (builtIn != null) {
            LUT builtInLut = new LUT("built-in", builtIn/* , builtIn */);
            lut = builtInLut;
            return;
        }

        MetaData metaData = jp2Image.metaDataList[0];
        if (metaData instanceof HelioviewerMetaData) {
            HelioviewerMetaData hvMetaData = (HelioviewerMetaData) metaData;
            String colorKey = DefaultTable.getSingletonInstance().getColorTable(hvMetaData);
            if (colorKey != null) {
                lut = LUT.getStandardList().get(colorKey);
                return;
            }
        }
        // no LUT found, try gray as last resort
        lut = gray;
    }
}
