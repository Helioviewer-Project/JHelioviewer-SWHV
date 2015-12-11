package org.helioviewer.jhv.viewmodel.view;

import java.net.URI;

import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;
import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus.CacheStatus;
import org.helioviewer.jhv.viewmodel.imagedata.ImageDataHandler;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

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

    public void render(Camera camera, Viewport vp, double factor);

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
     * Returns the download uri the image.
     *
     * This is the uri from which the whole file can be downloaded and stored
     * locally
     *
     * @return download uri
     */
    public URI getDownloadURI();


    public LUT getDefaultLUT();

    public CacheStatus getImageCacheStatus(int frame);

    /**
     * Returns the frame rate on which the View is operating right now.
     *
     * The number has not been recalculated every frame, so changes on the desired
     * frame rate may not be visible immediately.
     *
     * @return average actual frame rate
     */
    public float getCurrentFramerate();

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

    public void setImageLayer(RenderableImageLayer imageLayer);

    public RenderableImageLayer getImageLayer();

    public void setDataHandler(ImageDataHandler dataHandler);

    public JHVDate getFrameDateTime(int frame);

    // <!- only for Layers
    public void setFrame(JHVDate time);
    public int getFrame(JHVDate time);
    public MetaData getMetaData(JHVDate time);
    // -->

}
