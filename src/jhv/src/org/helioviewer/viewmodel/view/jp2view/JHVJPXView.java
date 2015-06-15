package org.helioviewer.viewmodel.view.jp2view;

import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.cache.ImageCacheStatus;
import org.helioviewer.viewmodel.view.cache.ImageCacheStatus.CacheStatus;
import org.helioviewer.viewmodel.view.cache.LocalImageCacheStatus;
import org.helioviewer.viewmodel.view.cache.RemoteImageCacheStatus;
import org.helioviewer.viewmodel.view.jp2view.J2KRender.RenderReasons;
import org.helioviewer.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.viewmodel.view.jp2view.image.SubImage;

/**
 * Implementation of TimedMovieView for JPX files.
 *
 * <p>
 * This class is an extensions of {@link JHVJP2View} for JPX-Files, providing
 * additional movie commands.
 *
 * <p>
 * For information about image series, see
 * {@link org.helioviewer.viewmodel.view.MovieView} and
 * {@link org.helioviewer.viewmodel.view.TimedMovieView}.
 *
 * @author Markus Langenberg
 */
public class JHVJPXView extends JHVJP2View implements MovieView {

    // Caching
    protected ImageCacheStatus imageCacheStatus;
    protected int lastRenderedCompositionLayer = -1;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setJP2Image(JP2Image newJP2Image) {
        if (jp2Image != null) {
            abolish();
        }

        jp2Image = newJP2Image;

        if (newJP2Image.isRemote()) {
            imageCacheStatus = new RemoteImageCacheStatus(this);
        } else {
            imageCacheStatus = new LocalImageCacheStatus(this);
        }
        jp2Image.setImageCacheStatus(imageCacheStatus);

        super.setJP2Image(newJP2Image);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageCacheStatus getImageCacheStatus() {
        return imageCacheStatus;
    }

    // to be accessed only from Layers
    @Override
    public void setCurrentFrame(ImmutableDateTime time) {
        if (time == null)
            return;

        int frameNumber = -1;
        long timeMillis = time.getMillis();
        long lastDiff, currentDiff = -Long.MAX_VALUE;
        do {
            lastDiff = currentDiff;
            currentDiff = jp2Image.metaDataList[++frameNumber].getDateObs().getMillis() - timeMillis;
        } while (currentDiff < 0 && frameNumber < jp2Image.getMaximumFrameNumber());

        if (-lastDiff < currentDiff) {
            setCurrentFrameNumber(frameNumber - 1);
        } else {
            setCurrentFrameNumber(frameNumber);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCurrentFrameNumber() {
        return imageViewParams.compositionLayer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaximumFrameNumber() {
        return jp2Image.getMaximumFrameNumber();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaximumAccessibleFrameNumber() {
        if (imageCacheStatus != null) {
            return imageCacheStatus.getImageCachedPartiallyUntil();
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImmutableDateTime getFrameDateTime(int frameNumber) {
        if (frameNumber >= 0 && frameNumber <= getMaximumFrameNumber()) {
            return jp2Image.metaDataList[frameNumber].getDateObs();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getCurrentFramerate() {
        if (render != null)
            return render.getActualMovieFramerate();
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void setSubimageData(ImageData newImageData, SubImage roi, int compositionLayer, double zoompercent, boolean fullyLoaded) {
        fullyLoaded = this.imageCacheStatus.getImageStatus(compositionLayer) == CacheStatus.COMPLETE;
        super.setSubimageData(newImageData, roi, compositionLayer, zoompercent, fullyLoaded);
    }

    /**
     * Internal function for setting the current frame number.
     *
     * Before actually setting the new frame number, checks whether that is
     * necessary. If the frame number has changed, also triggers an update of
     * the image.
     *
     * @param frameNumber
     */
    private void setCurrentFrameNumber(int frameNumber) {
        if (frameNumber != imageViewParams.compositionLayer && frameNumber >= 0 && frameNumber <= getMaximumAccessibleFrameNumber()) {
            imageViewParams.compositionLayer = frameNumber;

            readerSignal.signal();
            if (readerMode != ReaderMode.ONLYFIREONCOMPLETE) {
                renderRequestedSignal.signal(RenderReasons.MOVIE_PLAY);
            }
        }
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
     * currently used number of quality layers and the current frame number.
     *
     * @return Set of parameters used within the jp2-package
     */
    @Override
    protected JP2ImageParameter calculateParameter() {
        return calculateParameter(getCurrentNumQualityLayers(), getCurrentFrameNumber());
    }

}
