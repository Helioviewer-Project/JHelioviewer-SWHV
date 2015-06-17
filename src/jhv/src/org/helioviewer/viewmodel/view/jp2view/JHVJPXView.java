package org.helioviewer.viewmodel.view.jp2view;

import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.cache.ImageCacheStatus;
import org.helioviewer.viewmodel.view.cache.LocalImageCacheStatus;
import org.helioviewer.viewmodel.view.cache.RemoteImageCacheStatus;
import org.helioviewer.viewmodel.view.jp2view.J2KRender.RenderReasons;

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
    public int getFrame(ImmutableDateTime time) {
        int frame = -1;
        long timeMillis = time.getMillis();
        long lastDiff, currentDiff = -Long.MAX_VALUE;
        do {
            lastDiff = currentDiff;
            currentDiff = jp2Image.metaDataList[++frame].getDateObs().getMillis() - timeMillis;
        } while (currentDiff < 0 && frame < jp2Image.getMaximumFrameNumber());

        if (-lastDiff < currentDiff) {
            return frame - 1;
        } else {
            return frame;
        }
    }

    /**
     * Before actually setting the new frame number, checks whether that is
     * necessary. If the frame number has changed, also triggers an update of
     * the image.
     *
     * @param frame
     */
    // to be accessed only from Layers
    @Override
    public void setFrame(int frame) {
        if (frame != imageViewParams.compositionLayer &&
            frame >= 0 && frame <= getMaximumAccessibleFrameNumber()) {
            imageViewParams.compositionLayer = frame;

            readerSignal.signal();
            if (readerMode != ReaderMode.ONLYFIREONCOMPLETE) {
                renderRequestedSignal.signal(RenderReasons.MOVIE_PLAY);
            }
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
    public ImmutableDateTime getFrameDateTime(int frame) {
        if (frame >= 0 && frame <= jp2Image.getMaximumFrameNumber()) {
            return jp2Image.metaDataList[frame].getDateObs();
        }
        return null;
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

}
