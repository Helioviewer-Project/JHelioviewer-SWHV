package org.helioviewer.viewmodel.view;

import java.net.URI;

import org.helioviewer.base.Region;
import org.helioviewer.base.Viewport;
import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;
import org.helioviewer.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.MetaData;

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

    public void abolish();

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

    public ImageData getBaseDifferenceImageData();

    public ImageData getPreviousImageData();

    public ImageData getImageData();

    public LUT getDefaultLUT();

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

    public void setImageLayer(RenderableImageLayer imageLayer);

    public RenderableImageLayer getImageLayer();

    public void setDataHandler(ViewDataHandler dataHandler);

    public void removeDataHandler();

    public ImmutableDateTime getFrameDateTime(int frame);

    // <!- only for Layers
    public void setFrame(int frame);
    public int getFrame(ImmutableDateTime time);
    public MetaData getMetaData(ImmutableDateTime time);
    // -->

}
