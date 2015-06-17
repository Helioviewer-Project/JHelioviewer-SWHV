package org.helioviewer.viewmodel.view.jp2view;

import java.awt.Dimension;
import java.net.URI;

import org.helioviewer.base.Region;
import org.helioviewer.base.Viewport;
import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.RenderListener;
import org.helioviewer.jhv.gui.filters.lut.DefaultTable;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.metadata.ObserverMetaData;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.jp2view.concurrency.BooleanSignal;
import org.helioviewer.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.viewmodel.view.jp2view.image.SubImage;

/**
 * Implementation of View for JPG2000 images.
 * <p>
 * This class represents the gateway to the heart of the helioviewer project. It
 * is responsible for reading and decoding JPG2000 images. Therefore, it manages
 * two threads: one thread for communicating with the JPIP server, the other one
 * for decoding the images.
 *
 */
public class JHVJP2View extends AbstractView implements RenderListener {

    public enum ReaderMode {
        NEVERFIRE, ONLYFIREONCOMPLETE, ALWAYSFIREONNEWDATA, SIGNAL_RENDER_ONCE
    }

    protected Viewport viewport;
    protected Region region;

    // Member related to JP2
    protected JP2Image jp2Image;
    protected JP2ImageParameter imageViewParams;

    // Reader
    protected J2KReader reader;
    protected ReaderMode readerMode = ReaderMode.ALWAYSFIREONNEWDATA;
    final BooleanSignal readerSignal = new BooleanSignal(false);

    // Renderer
    protected J2KRender render;
    final BooleanSignal renderSignal = new BooleanSignal(false);

    // Renderer-ThreadGroup - This group is necessary to identify all renderer threads
    public static final ThreadGroup renderGroup = new ThreadGroup("J2KRenderGroup");

    private ImageData previousImageData;
    private ImageData baseDifferenceImageData;

    public JHVJP2View() {
        Displayer.addRenderListener(this);
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
            region = new Region(metaData.getPhysicalLowerLeft(), metaData.getPhysicalSize());
        }

        jp2Image = newJP2Image;
        jp2Image.addReference();
        imageViewParams = calculateParameter(region, 0);

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
     * {@inheritDoc}
     */
    @Override
    public boolean setViewport(Viewport v) {
        boolean viewportChanged = (viewport == null ? v == null : !viewport.equals(v));
        viewport = v;

        if (setImageViewParams(calculateParameter(region, imageViewParams.compositionLayer), true)) {
            return true;
        } else if (viewportChanged && imageViewParams.resolution.getZoomLevel() == jp2Image.getResolutionSet().getMaxResolutionLevels()) {
            renderSignal.signal();
            return true;
        }

        return viewportChanged;
    }

    private int getTrueFrameNumber() {
        int frameNumber = 0;
        if (imageData != null) {
            frameNumber = imageData.getFrameNumber();
        }
        return frameNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaData getMetaData() {
        return jp2Image.metaDataList[getTrueFrameNumber()];
    }

    public String getXMLMetaData() {
        return jp2Image.getXML(getTrueFrameNumber() + 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        MetaData metaData = jp2Image.metaDataList[getTrueFrameNumber()];
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
    public boolean setRegion(Region r) {
        boolean changed = region == null ? r == null : !region.equals(r);
        region = r;
        changed |= setImageViewParams(calculateParameter(region, imageViewParams.compositionLayer), true);

        return changed;
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
        if (reader != null) {
            return reader.isConnected();
        }
        return false;
    }

    /**
     * Destroy the resources associated with this object.
     */
    public void abolish() {
        Displayer.removeRenderListener(this);

        if (reader != null) {
            reader.abolish();
            reader = null;
        }
        if (render != null) {
            render.abolish();
            render = null;
        }
        jp2Image.abolish();
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
     * @param frameNumber
     *            Frame number to show (has to be 0 for single images)
     * @return Set of parameters used within the jp2-package
     */
    protected JP2ImageParameter calculateParameter(Region r, int frameNumber) {
        MetaData m = jp2Image.metaDataList[frameNumber];

        double mWidth = m.getPhysicalSize().x;
        double mHeight = m.getPhysicalSize().y;
        double rWidth = r.getWidth();
        double rHeight = r.getHeight();

        double ratio = Displayer.getViewportHeight() / Displayer.getActiveCamera().getCameraWidth();
        int w = (int) (rWidth * ratio);
        int h = (int) (rHeight * ratio);

        ratio = mWidth / rWidth;
        int totalWidth = (int) (w * ratio);
        int totalHeight = (int) (h * ratio);

        ResolutionLevel res = jp2Image.getResolutionSet().getNextResolutionLevel(new Dimension(totalWidth, totalHeight));
        int viewportImageWidth = res.getResolutionBounds().width;
        int viewportImageHeight = res.getResolutionBounds().height;

        double currentMeterPerPixel = mWidth / viewportImageWidth;
        int imageWidth = (int) Math.round(rWidth / currentMeterPerPixel);
        int imageHeight = (int) Math.round(rHeight / currentMeterPerPixel);

        GL3DVec2d rUpperLeft = r.getUpperLeftCorner();
        GL3DVec2d mUpperLeft = m.getPhysicalUpperLeft();
        double displacementX = rUpperLeft.x - mUpperLeft.x;
        double displacementY = rUpperLeft.y - mUpperLeft.y;

        int imagePositionX = (int) Math.round(displacementX / mWidth * viewportImageWidth);
        int imagePositionY = -(int) Math.round(displacementY / mHeight * viewportImageHeight);
        SubImage subImage = new SubImage(imagePositionX, imagePositionY, imageWidth, imageHeight);

        return new JP2ImageParameter(subImage, res, frameNumber);
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

    JP2ImageParameter getImageViewParams() {
        synchronized (imageViewParams) {
            try {
                return (JP2ImageParameter) imageViewParams.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
        return null;
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
    void setSubimageData(ImageData newImageData, JP2ImageParameter params) {
        MetaData metaData = jp2Image.metaDataList[params.compositionLayer];

        newImageData.setFrameNumber(params.compositionLayer);
        newImageData.setLocalRotation(metaData.getRotationObs());

        if (metaData instanceof HelioviewerMetaData) {
            newImageData.setRegion(((HelioviewerMetaData) metaData).roiToRegion(params.subImage, params.resolution.getZoomPercent()));
        }

        if (params.compositionLayer == 0) {
            baseDifferenceImageData = newImageData;
        }

        if (imageData != null && Math.abs(params.compositionLayer - imageData.getFrameNumber()) == 1) {
            previousImageData = imageData;
        } else if (previousImageData != null && previousImageData.getFrameNumber() - params.compositionLayer > 2) {
            previousImageData = newImageData;
        }

        imageData = newImageData;
        fireFrameChanged(this, metaData.getDateObs());
    }

    protected void fireFrameChanged(JHVJP2View aView, ImmutableDateTime aDateTime) {
        Displayer.fireFrameChanged(aView, aDateTime);
    }

    @Override
    public ImageData getImageData() {
        return imageData;
    }

    @Override
    public ImageData getPreviousImageData() {
        return previousImageData;
    }

    @Override
    public ImageData getBaseDifferenceImageData() {
        return baseDifferenceImageData;
    }

    @Override
    public void render() {
        renderSignal.signal();
    }

    private void setStartLUT() {
        int[] builtIn = jp2Image.getBuiltInLUT();
        if (builtIn != null) {
            LUT builtInLut = new LUT("built-in", builtIn/* , builtIn */);
            lut = builtInLut;
            return;
        }

        MetaData metaData = jp2Image.metaDataList[0];
        if (metaData instanceof HelioviewerMetaData) {
            String colorKey = DefaultTable.getSingletonInstance().getColorTable((HelioviewerMetaData) metaData);
            if (colorKey != null) {
                lut = LUT.getStandardList().get(colorKey);
                return;
            }
        }
        // no LUT found, try gray as last resort
        lut = gray;
    }

}
