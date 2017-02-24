package org.helioviewer.jhv.viewmodel.view;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.viewmodel.imagedata.ImageDataHandler;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public interface View {

    enum AnimationMode {
        Loop, Stop, Swing, SwingDown
    }

    APIRequest getAPIRequest();
    void setAPIRequest(APIRequest apiRequest);

    void abolish();

    void render(Camera camera, Viewport vp, double factor);

    /**
     * Returns the URI representing the location of the image.
     *
     * @return URI representing the location of the image.
     */
    URI getURI();
    /**
     * Returns the name the image.
     *
     * This might be the filename, but it can be something else extracted from
     * the meta data.
     *
     * @return Name of the image
     */
    String getName();

    LUT getDefaultLUT();

    /**
     * Returns the frame rate on which the View is operating right now.
     *
     * The number has not been recalculated every frame, so changes on the desired
     * frame rate may not be visible immediately.
     *
     * @return average actual frame rate
     */
    float getCurrentFramerate();

    boolean isMultiFrame();

    /**
     * Returns the current frame number.
     *
     * @return current frame number
     */
    int getCurrentFrameNumber();

    /**
     * Returns the maximum frame number.
     *
     * @return maximum frame number
     */
    int getMaximumFrameNumber();

    void setImageLayer(ImageLayer imageLayer);

    ImageLayer getImageLayer();

    void setDataHandler(ImageDataHandler dataHandler);

    boolean isDownloading();

    boolean isComplete();

    AtomicBoolean getFrameCacheStatus(int frame);

    JHVDate getFrameTime(int frame);
    JHVDate getFirstTime();
    JHVDate getLastTime();

    // <!- only for Layers
    JHVDate getNextTime(AnimationMode mode, int deltaT);
    void setFrame(JHVDate time);
    JHVDate getFrameTime(JHVDate time);
    MetaData getMetaData(JHVDate time);
    // -->

}
