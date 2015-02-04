package org.helioviewer.jhv.internal_plugins.filter.difference;

import java.util.LinkedList;
import java.util.List;

import javax.media.opengl.GL2;
import javax.media.opengl.GLException;
import javax.media.opengl.glu.GLU;

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
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
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
    private volatile boolean baseDifferenceNoRot = false;
    private volatile boolean runDiffNoRot = false;

    /**
     * Observer listener
     */
    private final List<FilterListener> listeners = new LinkedList<FilterListener>();
    /**
     * Given time machine to access the previous frame
     */
    // private TimeMachineData timeMachineData;
    private final DifferenceShader shader = new DifferenceShader();
    private static GLTextureHelper textureHelper;
    private int diffTex = -1;
    private ImageData currentFrame;
    private float truncationValue = 0.2f;
    private JHVJPXView jpxView;
    private JHVJP2View jp2View;

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

        if (jpxView == null)
            return data;
        ImageData previousFrame;
        if (!baseDifference) {
            previousFrame = jpxView.getPreviousImageData();
        } else {
            previousFrame = jpxView.getBaseDifferenceImageData();
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

    public void setActive(boolean isActive) {
        this.isActive = isActive;
        jpxView.setDifferenceMode(isActive);
        jpxView.setFullyLoadedMode(isActive);
        notifyAllListeners();
    }

    @Override
    public void setJP2View(JHVJP2View jp2View) {
        this.jp2View = jp2View;
        if (jp2View instanceof JHVJPXView) {
            this.jpxView = (JHVJPXView) jp2View;
        }
    }

    @Override
    public void setState(String state) {
    }

    @Override
    public String getState() {
        return Boolean.toString(this.isActive);
    }

    @Override
    public void applyGL(GL2 gl) {

        if (isActive) {
            if (StateController.getInstance().getCurrentState().getType() == ViewStateEnum.View3D) {
                if (jpxView.getBaseDifferenceMode()) {
                    if (this.baseDifferenceNoRot) {
                        shader.setIsDifference(gl, 0.26f);
                    } else {
                        shader.setIsDifference(gl, 0.99f);
                    }
                } else {
                    if (this.runDiffNoRot) {
                        shader.setIsDifference(gl, 0.25f);
                    } else {
                        shader.setIsDifference(gl, 1.0f);
                    }
                }
            } else {
                if (jpxView.getBaseDifferenceMode()) {
                    shader.setIsDifference(gl, 0.26f);
                } else {
                    shader.setIsDifference(gl, 0.25f);
                }
            }
            shader.bind(gl);
            ImageData previousFrame;
            if (!baseDifference) {
                previousFrame = jpxView.getPreviousImageData();
            } else {
                previousFrame = jpxView.getBaseDifferenceImageData();
            }
            if (this.currentFrame != previousFrame) {
                shader.setTruncationValue(gl, this.truncationValue);

                gl.glActiveTexture(GL2.GL_TEXTURE2);
                textureHelper.moveImageDataToGLTexture(gl, previousFrame, 0, 0, previousFrame.getWidth(), previousFrame.getHeight(), diffTex);
                gl.glActiveTexture(GL2.GL_TEXTURE0);
            }
        } else {
            shader.setIsDifference(gl, 0.0f);
            shader.bind(gl);
        }
    }

    @Override
    public GLShaderBuilder buildFragmentShader(GLShaderBuilder shaderBuilder) {
        shader.build(shaderBuilder);

        GL2 gl = shaderBuilder.getGL();
        int[] tmp = new int[1];

        if (diffTex != -1) {
            tmp[0] = diffTex;
            gl.glDeleteTextures(1, tmp, 0);
        }
        gl.glGenTextures(1, tmp, 0);
        diffTex = tmp[0];

        textureHelper = new GLTextureHelper();

        return shaderBuilder;
    }

    @Override
    protected void finalize() {
        if (diffTex != -1) {
            GL2 gl = (GL2) GLU.getCurrentGL();

            int[] tmp = new int[1];
            tmp[0] = diffTex;
            gl.glDeleteTextures(1, tmp, 0);
        }
    }

    public void setTruncationvalue(float truncationValue) {
        this.truncationValue = truncationValue;
    }

    public void setBaseDifference(boolean selected) {
        this.baseDifference = selected;
        jpxView.setBaseDifferenceMode(selected);
    }

    public void setBaseDifferenceRot(boolean baseDifferenceRot) {
        this.baseDifferenceNoRot = baseDifferenceRot;
    }

    public void setRunDiffNoRot(boolean runDiffNoRot) {
        this.runDiffNoRot = runDiffNoRot;
    }
}
