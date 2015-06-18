package org.helioviewer.viewmodel.view;

import java.net.URI;

import org.helioviewer.base.Region;
import org.helioviewer.base.Viewport;
import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.cache.ImageCacheStatus;

/**
 * View to manage an image data source.
 *
 * @author Ludwig Schmidt
 */
public interface View {

    public enum AnimationMode {
        LOOP {
            @Override
            public String toString() {
                return "Loop";
            }
        },
        STOP {
            @Override
            public String toString() {
                return "Stop";
            }
        },
        SWING {
            @Override
            public String toString() {
                return "Swing";
            }
        }
    }

    /**
     * Returns the URI representing the location of the image.
     *
     * @return URI representing the location of the image.
     */
    public URI getUri();
    /**
     * Returns the name the image.
     *
     * This might be the filename, but it can be something else extracted from
     * the meta data.
     *
     * @return Name of the image
     */
    public String getName();
    /**
     * Returns, whether the image is a remote image (e.g. jpip).
     *
     * @return true, if the image is accessed remotely, false otherwise
     */
    public boolean isRemote();
    /**
     * Returns the download uri the image.
     *
     * This is the uri from which the whole file can be downloaded and stored
     * locally
     *
     * @return download uri
     */
    public URI getDownloadURI();

    public boolean getBaseDifferenceMode();

    public boolean getDifferenceMode();

    public ImageData getBaseDifferenceImageData();

    public ImageData getPreviousImageData();

    public ImageData getImageData();

    public MetaData getMetaData();

    public boolean setRegion(Region r);

    public boolean setViewport(Viewport r);

   /**
     * Returns the image cache status.
     *
     * @return image cache status
     */
    public ImageCacheStatus getImageCacheStatus();

    /**
     * Returns the frame rate on which the View is operating right now.
     *
     * The number has not been recalculated every frame, so changes on the desired
     * frame rate may not be visible immediately.
     *
     * @return average actual frame rate
     */
    public float getActualFramerate();

    public boolean isMultiFrame();

    /**
     * Returns the current frame number.
     *
     * @return current frame number
     */
    public int getCurrentFrameNumber();

    /**
     * Returns the maximum frame number.
     *
     * @return maximum frame number
     */
    public int getMaximumFrameNumber();

    /**
     * Returns the last accessible frame number.
     *
     * @return maximum accessible frame number
     */
    public int getMaximumAccessibleFrameNumber();

    // <!- only for Layers
    public void setFrame(int frame);
    public int getFrame(ImmutableDateTime time);
    public ImmutableDateTime getFrame(int frame);
    // -->

}
