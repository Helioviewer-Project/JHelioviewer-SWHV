package org.helioviewer.swhv.gui.layerpanel;

import org.helioviewer.globalstate.GlobalStateContainer;
import org.helioviewer.swhv.gui.layerpanel.layercontainer.SWHVLayerContainerModel;
import org.helioviewer.swhv.mvc.SWHVAbstractModel;

public abstract class SWHVAbstractLayerModel extends SWHVAbstractModel implements SWHVLayerModel {
    private SWHVLayerModel parent;
    private SWHVLayerContainerModel layerContainerModel = GlobalStateContainer.getSingletonInstance().getLayerContainerModel();
    private SWHVLayerModel[] children = new SWHVLayerModel[0];
    private int position;
    private int level = 0;
    private boolean folded = false;
    private boolean visible = true;
    private int type;
    private boolean active = false;
    protected SWHVAbstractOptionPanel optionPanel;

    public void fold() {
        this.setFolded(true);
        hideChildren();
        layerContainerModel.fireLayerFolded();
    }

    public void unFold() {
        this.setFolded(false);
        unhideChildren();
        layerContainerModel.fireLayerFolded();
    }

    @Override
    public void toggleFold() {
        if (this.isFolded()) {
            unFold();
        } else {
            fold();
        }
        this.fireFoldedChanged();
    }

    @Override
    public void hideChildren() {
        synchronized (layerContainerModel) {
            for (int i = 0; i < this.children.length; i++) {
                this.children[i].setVisible(false);
                this.children[i].hideChildren();
            }
        }
    }

    @Override
    public void unhideChildren() {
        synchronized (layerContainerModel) {
            for (int i = 0; i < this.children.length; i++) {
                this.children[i].setVisible(true);
                if (!this.children[i].isFolded()) {
                    children[i].unhideChildren();
                }
            }
        }
    }

    @Override
    public SWHVLayerModel getParent() {
        return parent;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public void setParent(SWHVLayerModel parent) {
        this.parent = parent;
    }

    @Override
    public SWHVLayerModel[] getChildren() {
        return children;
    }

    @Override
    public void addChild(SWHVLayerModel child) {
        synchronized (layerContainerModel) {
            child.setParent(this);
            int len = this.children.length;

            if (len == 0) {
                this.children = new SWHVLayerModel[1];
                this.children[0] = child;
                this.layerContainerModel.addLayer(child, this.getPosition() + 1);

            } else {
                int split = len - 1;
                for (int i = 0; i < len; i++) {
                    if (child.getClass().equals(this.children[i].getClass())) {
                        split = i;
                    }
                }
                this.layerContainerModel.addLayer(child, this.children[split].getPosition() + 1);
                SWHVLayerModel[] newChildren = new SWHVLayerModel[len + 1];
                for (int i = 0; i <= split; i++) {
                    newChildren[i] = this.children[i];
                }
                newChildren[split + 1] = child;
                for (int i = split + 2; i < len + 1; i++) {
                    newChildren[i] = this.children[i - 1];
                }
                this.children = newChildren;
            }
            child.setActive(true, true);
            child.setLevel(this.getLevel() + 1);
            child.fireLevelChanged();

        }
    }

    private int getChildIndex(SWHVLayerModel child) {
        if (this.children.length == 0) {
            return -1;
        }
        int i = 0;
        int len = this.children.length;
        while (i < len && child != this.children[i]) {
            i++;
        }
        return i;
    }

    @Override
    public void removeChild(int i) {
        synchronized (layerContainerModel) {
            int len = this.children.length;
            if (len > 0 && i < len) {
                this.layerContainerModel.removeLayer(this.children[i].getPosition());
                this.children[i].fireRemoved();
                if (0 <= i && i < len) {
                    this.children[i].removeAllChildren();
                    if (len - 1 > 0) {
                        SWHVLayerModel[] newChildren = new SWHVLayerModel[len - 1];
                        for (int j = 0; j < i; j++) {
                            newChildren[j] = this.children[j];
                        }
                        for (int j = i; j < len - 1; j++) {
                            newChildren[j] = this.children[j + 1];
                        }
                        this.children = newChildren;
                    } else {
                        this.children = new SWHVLayerModel[0];
                    }
                }
            }
        }
    }

    public void setRoot() {
        this.layerContainerModel.addLayer(this);
        this.setActive(true, true);
        this.fireLevelChanged();
    }

    public void remove() {

        synchronized (layerContainerModel) {
            if (this.position > 0) {
                if (this.isActive()) {
                    this.layerContainerModel.getLayer(this.position - 1).setActive(true, true);
                }
            }
            if (this.parent != null) {
                this.parent.removeChild(this);
            } else {
                this.removeAllChildren();
                this.layerContainerModel.removeLayer(this.position);
            }
        }
    }

    @Override
    public void removeChild(SWHVLayerModel child) {
        synchronized (layerContainerModel) {
            int i = getChildIndex(child);
            this.removeChild(i);
        }
    }

    @Override
    public void removeAllChildren() {
        synchronized (layerContainerModel) {
            if (this.children.length > 0) {
                for (int i = children.length - 1; i >= 0; i--) {
                    removeChild(i);
                }
            }
        }
    }

    @Override
    public void addChild(SWHVLayerModel[] children) {
        synchronized (layerContainerModel) {
            for (int i = 0; i < this.children.length; i++) {
                this.addChild(children[i]);
            }
        }
    }

    @Override
    public boolean isChild(SWHVLayerModel child) {
        int i = 0;
        int len = this.children.length;
        while (i < len && child != this.children[i]) {
            i++;
        }
        return (i != len);
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public SWHVLayerContainerModel getLayerContainerModel() {
        return layerContainerModel;
    }

    @Override
    public void setLayerContainerModel(SWHVLayerContainerModel layerContainerModel) {
        this.layerContainerModel = layerContainerModel;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active, boolean setPrevious) {
        synchronized (layerContainerModel) {
            if (active) {
                layerContainerModel.fireLayerActivated(this.getPosition());
            }
            SWHVLayerModel previousActiveLayer = this.layerContainerModel.findActive();
            if (previousActiveLayer != null && setPrevious) {
                previousActiveLayer.setActive(false, false);
            }
            this.active = active;
            fireActiveChanged();
            this.getOptionPanel().setActive();
        }
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    @Override
    public boolean isFolded() {
        return folded;
    }

    @Override
    public void fireActiveChanged() {
        for (int i = 0; i < this.listenerPanel.length; i++) {
            SWHVLayerModelListener listener = (SWHVLayerModelListener) this.listenerPanel[i];
            listener.updateActive(this);
        }
    }

    @Override
    public int getPositionAsChild() {
        if (this.getParent() == null) {
            return -1;
        } else {
            int i = 0;
            SWHVLayerModel[] parentChildren = this.parent.getChildren();
            while (parentChildren[i] != this) {
                i++;
            }
            return i;
        }
    }

    @Override
    public int getPositionAsChildByType() {
        if (this.getParent() == null) {
            return -1;
        } else {
            int i = 0;
            int count = 0;
            SWHVLayerModel[] parentChildren = this.parent.getChildren();
            while (parentChildren[i] != this) {
                if (parentChildren[i].getClass().equals(this.getClass())) {
                    count++;
                }
                i++;
            }
            return count;
        }
    }
}
