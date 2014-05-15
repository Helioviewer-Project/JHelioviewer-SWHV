package org.helioviewer.jhv.internal_plugins.filter.difference;

import java.util.LinkedList;
import java.util.List;

import javax.media.opengl.GL;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.gui.states.StateController;
import org.helioviewer.jhv.gui.states.ViewStateEnum;
import org.helioviewer.viewmodel.filter.FilterListener;
import org.helioviewer.viewmodel.filter.FrameFilter;
import org.helioviewer.viewmodel.filter.GLFragmentShaderFilter;
import org.helioviewer.viewmodel.filter.ObservableFilter;
import org.helioviewer.viewmodel.filter.StandardFilter;
import org.helioviewer.viewmodel.imagedata.ColorMask;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.viewmodel.view.TimeMachineData;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;

/**
 * Filter applying running difference to some movie
 *
 * @author Helge Dietert
 */
public class RunningDifferenceFilter implements FrameFilter, StandardFilter, ObservableFilter, GLFragmentShaderFilter {
    /**
     * Flag to indicate whether this filter should be considered active
     */
    private volatile boolean isActive = true;
    private volatile boolean baseDifference = false;

    /**
     * Observer listener
     */
    private final List<FilterListener> listeners = new LinkedList<FilterListener>();
    /**
     * Given time machine to access the previous frame
     */
    private TimeMachineData timeMachineData;
    private final DifferenceShader shader = new DifferenceShader();
    private int lookupDiff;
    private ImageData currentFrame;
    private float truncationValue = 0.05f;

    /**
     * @see org.helioviewer.viewmodel.filter.ObservableFilter#addFilterListener(org.helioviewer.viewmodel.filter.FilterListener)
     */
    @Override
    public void addFilterListener(FilterListener l) {
        listeners.add(l);
    }

    /**
     * @see org.helioviewer.viewmodel.filter.StandardFilter#apply(org.helioviewer.viewmodel.imagedata.ImageData)
     */
    @Override
    public ImageData apply(ImageData data) {
        // If its not active we don't filter at all
        if (!isActive)
            return data;

        if (data == null)
            return null;

        if (timeMachineData == null)
            return data;
        ImageData previousFrame;
        if(!baseDifference){
            previousFrame = timeMachineData.getPreviousFrame();
        }
        else{
            previousFrame =timeMachineData.getBaseDifferenceFrame();
        }
        if (previousFrame != null) {
            // Filter according to the data type
            if (data.getImageTransport() instanceof Byte8ImageTransport) {
                // Just one channel
                byte[] newPixelData = ((Byte8ImageTransport) data.getImageTransport()).getByte8PixelData();
                byte[] prevPixelData = ((Byte8ImageTransport) previousFrame.getImageTransport()).getByte8PixelData();
                if (newPixelData.length != prevPixelData.length) {
                    Log.warn("Pixel data has not the same size!! New size " + newPixelData.length + " old size " + prevPixelData.length);
                    return null;
                }
                byte[] pixelData = new byte[newPixelData.length];
                double tr = 16;
                for (int i = 0; i < newPixelData.length; i++) {

                    // pixelData[i] = (byte) ((((newPixelData[i] << 4>>>1) -
                    // (prevPixelData[i] << 4>>>1))) + 0x80);
                    int h1 = newPixelData[i];
                    int h2 = prevPixelData[i];
                    int diff = h1 - h2;
                    if (diff < -tr) {
                        diff = (int) (-tr);
                    } else if (diff > tr) {
                        diff = (int) tr;
                    }

                    pixelData[i] = (byte) ((int) ((((diff) / tr + 1) * (255. / 2.))));

                }
                final ColorMask colorMask = new ColorMask();

                return new SingleChannelByte8ImageData(data.getWidth(), data.getHeight(), pixelData, colorMask);
            }
        }
        System.out.println("No other frame available");
        return data;
    }

    /**
     * @see org.helioviewer.viewmodel.filter.Filter#forceRefilter()
     */
    @Override
    public void forceRefilter() {
        // This plugin always filter the data
    }

    /**
     * @return the isActive
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * @see org.helioviewer.viewmodel.filter.Filter#isMajorFilter()
     */
    @Override
    public boolean isMajorFilter() {
        return true;
    }

    /**
     * Inform all listener about a change of the state
     */
    protected void notifyAllListeners() {
        for (FilterListener f : listeners) {
            f.filterChanged(this);
        }
    }

    /**
     * @see org.helioviewer.viewmodel.filter.ObservableFilter#removeFilterListener(org.helioviewer.viewmodel.filter.FilterListener)
     */
    @Override
    public void removeFilterListener(FilterListener l) {
        listeners.remove(l);
    }

    /**
     * @param isActive
     *            the isActive to set
     */
    public void setActive(boolean isActive) {
        this.isActive = isActive;
        timeMachineData.setActive(isActive);
        notifyAllListeners();
    }

    /**
     * @see org.helioviewer.viewmodel.filter.FrameFilter#setTimeMachineData(org.helioviewer.viewmodel.view.TimeMachineData)
     */
    @Override
    public void setTimeMachineData(TimeMachineData data) {
        // System.out.println("Time machine data is set");
        timeMachineData = data;
        if (timeMachineData != null)
            timeMachineData.setPreviousCache(1);
        else
            System.out.println("Empty time machine");
    }

    @Override
    public void setState(String state) {
        // setContrast(Float.parseFloat(state));
        // panel.setValue(contrast);
    }

    @Override
    public String getState() {
        return Boolean.toString(this.isActive);
    }

    @Override
    public void applyGL(GL gl) {
        if (isActive) {
            if(StateController.getInstance().getCurrentState().getType() == ViewStateEnum.View3D){
                shader.setIsDifference(gl, 1.0f);
            }
            else{
                shader.setIsDifference(gl, 0.25f);
            }
            shader.bind(gl);
            ImageData previousFrame;
            if(!baseDifference){
                previousFrame = timeMachineData.getPreviousFrame();
            }
            else{
                previousFrame =timeMachineData.getBaseDifferenceFrame();
            }
            if(this.currentFrame != previousFrame){
                shader.setTruncationValue(gl, this.truncationValue);
                gl.glActiveTexture(shader.mode);

                shader.activateDifferenceTexture(gl);
                gl.glBindTexture(GL.GL_TEXTURE_2D, lookupDiff);
                GLTextureHelper th = new GLTextureHelper();
                th.moveImageDataToGLTexture(gl, previousFrame, 0, 0, previousFrame.getWidth(), previousFrame.getHeight(), lookupDiff);
            }
        } else {
            shader.setIsDifference(gl, 0.0f);
        }
    }

    @Override
    public GLShaderBuilder buildFragmentShader(GLShaderBuilder shaderBuilder) {
        shader.build(shaderBuilder);
        GLTextureHelper textureHelper = new GLTextureHelper();
        textureHelper.delTextureID(shaderBuilder.getGL(), lookupDiff);
        lookupDiff = textureHelper.genTextureID(shaderBuilder.getGL());

        return shaderBuilder;
    }

    public void setTruncationvalue(float truncationValue) {
        this.truncationValue = truncationValue;
    }

    public void setBaseDifference(boolean selected) {
        this.baseDifference = selected;
        System.out.println("BASEDIFF" + this.baseDifference);
    }
}
