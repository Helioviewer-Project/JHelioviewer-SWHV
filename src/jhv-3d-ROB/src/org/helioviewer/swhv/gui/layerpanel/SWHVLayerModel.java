package org.helioviewer.swhv.gui.layerpanel;

import java.util.Date;

import org.helioviewer.swhv.gui.layerpanel.layercontainer.SWHVLayerContainerModel;
import org.helioviewer.swhv.mvc.SWHVModel;

/**
 * @author Freek Verstringe
 * 
 */
public interface SWHVLayerModel extends SWHVModel {
    /**
     * Returns the parent
     * 
     * @return
     */
    public SWHVLayerModel getParent();

    /**
     * Sets the parent
     * 
     * @param parent
     */
    public void setParent(SWHVLayerModel parent);

    /**
     * Return an array of all children
     * 
     * @return
     */
    public SWHVLayerModel[] getChildren();

    /**
     * Add a child
     * 
     * @param child
     */
    public void addChild(SWHVLayerModel child);

    /**
     * Remove the i-th child
     * 
     * @param i
     */
    public void removeChild(int i);

    /**
     * Remove the child by looking it up in the array of children
     * 
     * @param child
     */
    public void removeChild(SWHVLayerModel child);

    /**
     * Remove all children
     */
    public void removeAllChildren();

    /**
     * Add an array of children
     * 
     * @param children
     */
    public void addChild(SWHVLayerModel[] children);

    /**
     * Returns true if the object reference is in the array of children, false
     * otherwise.
     * 
     * @param child
     * @return
     */
    public boolean isChild(SWHVLayerModel child);

    /**
     * Return the containerModel
     * 
     * @return
     */
    public SWHVLayerContainerModel getLayerContainerModel();

    /**
     * set container model
     * 
     * @param layerContainerModel
     */
    public void setLayerContainerModel(SWHVLayerContainerModel layerContainerModel);

    public int getPositionAsChild();

    public int getPositionAsChildByType();

    /**
     * Return position in the list
     * 
     * @return
     */

    public int getPosition();

    /**
     * Set position in the list
     * 
     * @return
     */
    public void setPosition(int position);

    public SWHVLayerController getController();

    public void fireActiveChanged();

    public void fireLevelChanged();

    public void fireFoldedChanged();

    public boolean isActive();

    public void setActive(boolean active, boolean previous);

    public int getLevel();

    public void setLevel(int i);

    public void setVisible(boolean b);

    public void setFolded(boolean b);

    public void hideChildren();

    public boolean isFolded();

    public boolean isVisible();

    public void unhideChildren();

    public SWHVOptionPanel getOptionPanel();

    public void toggleFold();

    public void fireRemoved();

    public void setBeginDate(Date beginDate);

    public void setEndDate(Date endDate);

}
