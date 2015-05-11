package org.helioviewer.viewmodel.view.jp2view;

import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
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
public class JHVJPXView extends JHVJP2View implements TimedMovieView {

    // Caching
    protected ImageCacheStatus imageCacheStatus;
    protected int lastRenderedCompositionLayer = -1;

    private final LinkedMovieManager linkedMovieManager = LinkedMovieManager.getSingletonInstance();

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
    public JHVJPXView(boolean isMainView) {
        super(isMainView);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setJP2Image(JP2Image newJP2Image) {
        if (!isMainView) {
            super.setJP2Image(newJP2Image);
            imageCacheStatus = ((JHVJPXView) jp2Image.getParentView()).getImageCacheStatus();
            return;
        }

        if (jp2Image != null) {
            abolish(false);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentFrame(int frameNumber) {
        setCurrentFrame(frameNumber, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentFrame(int frameNumber, boolean forceSignal) {
        frameNumber = Math.max(0, Math.min(getMaximumFrameNumber(), frameNumber));

        if (forceSignal) {
            linkedMovieManager.setCurrentFrame(getFrameDateTime(frameNumber), forceSignal);
        } else {
            boolean changed = setCurrentFrameNumber(frameNumber, forceSignal);
            // may come twice, but jpx.setCurrentFrameNumber has a check
            if (changed) {
                linkedMovieManager.setCurrentFrame(getFrameDateTime(frameNumber), forceSignal);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentFrame(ImmutableDateTime time) {
        setCurrentFrame(time, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentFrame(ImmutableDateTime time, boolean forceSignal) {
        if (time == null)
            return;

        int frameNumber = -1;
        long timeMillis = time.getMillis();
        long lastDiff, currentDiff = -Long.MAX_VALUE;
        do {
            lastDiff = currentDiff;
            if (jp2Image.metaDataList[++frameNumber].getDateTime() == null) {
                return;
            }
            currentDiff = jp2Image.metaDataList[frameNumber].getDateTime().getMillis() - timeMillis;
        } while (currentDiff < 0 && frameNumber < jp2Image.getCompositionLayerRange().getEnd());

        if (-lastDiff < currentDiff) {
            setCurrentFrameNumber(frameNumber - 1, forceSignal);
        } else {
            setCurrentFrameNumber(frameNumber, forceSignal);
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
        return jp2Image.getCompositionLayerRange().getEnd();
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
    public ImmutableDateTime getCurrentFrameDateTime() {
        return jp2Image.metaDataList[getCurrentFrameNumber()].getDateTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImmutableDateTime getFrameDateTime(int frameNumber) {
        return jp2Image.metaDataList[frameNumber].getDateTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAnimationMode(AnimationMode mode) {
        if (render != null) {
            render.setAnimationMode(mode);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDesiredRelativeSpeed(int framesPerSecond) {
        if (render != null) {
            render.setMovieRelativeSpeed(framesPerSecond);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDesiredAbsoluteSpeed(int observationSecondsPerSecond) {
        if (render != null) {
            render.setMovieAbsoluteSpeed(observationSecondsPerSecond);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void linkMovie() {
        linkedMovieManager.linkMovie(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unlinkMovie() {
        linkedMovieManager.unlinkMovie(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getActualFramerate() {
        if (render != null)
            return render.getActualMovieFramerate();
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pauseMovie() {
        if (!isMoviePlaying()) {
            return;
        }
        readerSignal.signal();
        if (render != null) {
            render.setMovieMode(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void playMovie() {
        if (getMaximumFrameNumber() > 0) {
            if (linkedMovieManager.isMaster(this)) {
                if (render != null) {
                    render.setMovieMode(true);
                }
                readerSignal.signal();
                if (readerMode != ReaderMode.ONLYFIREONCOMPLETE) {
                    renderRequestedSignal.signal(RenderReasons.MOVIE_PLAY);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMoviePlaying() {
        if (render != null) {
            return render.isMovieMode() || (this != linkedMovieManager.getMasterMovie() && linkedMovieManager.isPlaying());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void abolish() {
        abolish(true);
    }

    /**
     * Abolishes the jpx view
     *
     * @param unlinkMovie
     *            true, if the movie should be unlinked before abolishing it
     */
    public void abolish(boolean unlinkMovie) {
        if (unlinkMovie) {
            linkedMovieManager.unlinkMovie(this);
        }
        super.abolish();
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
     * @return true, if the frame number has changed
     */
    public boolean setCurrentFrameNumber(int frameNumber, boolean forceSignal) {
        if (frameNumber != imageViewParams.compositionLayer || forceSignal) {
            imageViewParams.compositionLayer = frameNumber;

            readerSignal.signal();
            if (readerMode != ReaderMode.ONLYFIREONCOMPLETE) {
                renderRequestedSignal.signal(RenderReasons.MOVIE_PLAY);
            }
            return true;
        }
        return false;
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

    /**
     * @see org.helioviewer.viewmodel.view.MovieView#setReuseBuffer(boolean)
     */
    @Override
    public void setReuseBuffer(boolean reuseBuffer) {
        render.setReuseBuffer(reuseBuffer);
    }

    /**
     * @see org.helioviewer.viewmodel.view.MovieView#isReuseBuffer()
     */
    @Override
    public boolean isReuseBuffer() {
        return render.isReuseBuffer();
    }

    @Override
    public long getCurrentDateMillis() {
        HelioviewerMetaData metadata = (HelioviewerMetaData) jp2Image.metaDataList[getCurrentFrameNumber()];
        return metadata.getDateTime().getMillis();
    }

    @Override
    public int getDesiredRelativeSpeed() {
        return this.render.getMovieRelativeSpeed();
    }

}
