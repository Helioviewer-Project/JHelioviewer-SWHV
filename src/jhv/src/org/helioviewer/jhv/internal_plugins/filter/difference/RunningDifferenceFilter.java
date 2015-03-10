package org.helioviewer.jhv.internal_plugins.filter.difference;

import java.util.LinkedList;
import java.util.List;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.gui.states.StateController;
import org.helioviewer.jhv.gui.states.ViewStateEnum;
import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodel.filter.FilterListener;
import org.helioviewer.viewmodel.filter.FrameFilter;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.viewmodel.view.opengl.shader.GLSLShader;

/**
 * Filter applying running difference to some movie
 *
 * @author Helge Dietert
 */
public class RunningDifferenceFilter implements FrameFilter, Filter {
    /**
     * Flag to indicate whether this filter should be considered active
     */
    private boolean isActive = true;
    private boolean baseDifference = false;
    private boolean baseDifferenceNoRot = false;
    private boolean runDiffNoRot = false;

    /**
     * Observer listener
     */
    private final List<FilterListener> listeners = new LinkedList<FilterListener>();
    /**
     * Given time machine to access the previous frame
     */
    // private TimeMachineData timeMachineData;
    private final GLTextureHelper.GLTexture tex = new GLTextureHelper.GLTexture();
    private ImageData currentFrame;
    private float truncationValue = 0.2f;
    private JHVJPXView jpxView;

    /**
     * @see org.helioviewer.viewmodel.filter.ObservableFilter#addFilterListener(org.helioviewer.viewmodel.filter.FilterListener)
     */
    @Override
    public void addFilterListener(FilterListener l) {
        listeners.add(l);
    }

    /**
     * @return the isActive
     */
    public boolean isActive() {
        return isActive;
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
                        GLSLShader.setIsDifference(0.26f);
                    } else {
                        GLSLShader.setIsDifference(0.99f);
                    }
                } else {
                    if (this.runDiffNoRot) {
                        GLSLShader.setIsDifference(0.25f);
                    } else {
                        GLSLShader.setIsDifference(1.0f);
                    }
                }
            } else {
                if (jpxView.getBaseDifferenceMode()) {
                    GLSLShader.setIsDifference(0.26f);
                } else {
                    GLSLShader.setIsDifference(0.25f);
                }
            }
            ImageData previousFrame;
            if (!baseDifference) {
                previousFrame = jpxView.getPreviousImageData();
            } else {
                previousFrame = jpxView.getBaseDifferenceImageData();
            }
            if (this.currentFrame != previousFrame) {
                GLSLShader.setTruncationValue(this.truncationValue);

                gl.glActiveTexture(GL2.GL_TEXTURE2);
                GLTextureHelper.moveImageDataToGLTexture(gl, previousFrame, 0, 0, previousFrame.getWidth(), previousFrame.getHeight(), tex);
                gl.glActiveTexture(GL2.GL_TEXTURE0);
            }
        } else {
            GLSLShader.setIsDifference(0.0f);
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
