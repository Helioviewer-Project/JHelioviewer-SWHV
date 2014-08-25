package org.helioviewer.swhv.gui.layerpanel.layercontainer;

import org.helioviewer.globalstate.GlobalStateContainer;
import org.helioviewer.swhv.gui.layerpanel.SWHVLayerModel;
import org.helioviewer.swhv.mvc.SWHVAbstractModel;

/*
 * A representation of the layers in table form (the display order of the tree)
 */
public class SWHVLayerContainerModel extends SWHVAbstractModel {

    private SWHVLayerModel[] layers = new SWHVLayerModel[0];
    private static SWHVLayerContainerModel singletonInstance = new SWHVLayerContainerModel();

    private SWHVLayerContainerModel() {
    }

    public void addLayer(SWHVLayerModel layer) {
        synchronized (GlobalStateContainer.getSingletonInstance().getLayerContainerModel()) {
            if (this.getLayers().length != 0) {
                int len = this.getLayers().length;
                SWHVLayerModel[] newLayers = new SWHVLayerModel[len + 1];
                for (int i = 0; i < len; i++) {
                    newLayers[i] = this.getLayers()[i];
                }
                newLayers[len] = layer;
                layer.setPosition(len);
                this.setLayers(newLayers);
            } else {
                this.setLayers(new SWHVLayerModel[1]);
                this.getLayers()[0] = layer;
                layer.setPosition(0);
            }
            fireLayerAdded(layer);
        }
    }

    public void addLayer(SWHVLayerModel layer, int position) {
        synchronized (GlobalStateContainer.getSingletonInstance().getLayerContainerModel()) {
            int len = this.getLayers().length;
            SWHVLayerModel[] newLayers = new SWHVLayerModel[len + 1];
            for (int i = 0; i < position; i++) {
                newLayers[i] = this.getLayers()[i];
            }
            newLayers[position] = layer;
            layer.setPosition(position);
            for (int i = position + 1; i < len + 1; i++) {
                newLayers[i] = this.getLayers()[i - 1];
                newLayers[i].setPosition(i);
            }
            this.setLayers(newLayers);
            fireLayerAdded(layer, position);
        }
    }

    public void removeLayer(int position) {
        synchronized (GlobalStateContainer.getSingletonInstance().getLayerContainerModel()) {
            int len = this.getLayers().length;
            SWHVLayerModel[] newLayers = new SWHVLayerModel[len - 1];
            for (int i = 0; i < position; i++) {
                newLayers[i] = this.getLayers()[i];
            }
            for (int i = position + 1; i < len; i++) {
                newLayers[i - 1] = this.getLayers()[i];
                newLayers[i - 1].setPosition(i - 1);
            }
            this.setLayers(newLayers);
            fireLayerRemoved(position);
        }
    }

    public SWHVLayerModel getLayer(int position) {
        return this.getLayers()[position];
    }

    @Override
    public String toString() {
        String str = "";
        for (int i = 0; i < getLayers().length; i++) {
            str += getLayers()[i].toString() + "\n";
        }
        return str;
    }

    public SWHVLayerModel[] getLayers() {
        return layers;
    }

    public void setLayers(SWHVLayerModel[] layers) {
        this.layers = layers;
    }

    public void fireLayerAdded(SWHVLayerModel layer) {
        synchronized (this.listenerPanel) {
            for (int i = 0; i < listenerPanel.length; i++) {
                SWHVLayerContainerModelListener listener = (SWHVLayerContainerModelListener) listenerPanel[i];
                listener.layerAdded(this, layer);
            }
        }
    }

    public void fireLayerAdded(SWHVLayerModel layer, int position) {
        synchronized (this.listenerPanel) {
            for (int i = 0; i < listenerPanel.length; i++) {
                SWHVLayerContainerModelListener listener = (SWHVLayerContainerModelListener) listenerPanel[i];
                listener.layerAdded(this, layer, position);
            }
        }
    }

    public void fireLayerRemoved(int position) {
        synchronized (this.listenerPanel) {
            for (int i = 0; i < listenerPanel.length; i++) {
                SWHVLayerContainerModelListener listener = (SWHVLayerContainerModelListener) listenerPanel[i];
                listener.layerRemoved(this, position);
            }
        }
    }

    public void fireLayerFolded() {
        synchronized (this.listenerPanel) {
            for (int i = 0; i < listenerPanel.length; i++) {
                SWHVLayerContainerModelListener listener = (SWHVLayerContainerModelListener) listenerPanel[i];
                listener.layerFolded(this);
            }
        }
    }

    public void fireLayerActivated(int position) {
        synchronized (this.listenerPanel) {
            for (int i = 0; i < listenerPanel.length; i++) {
                SWHVLayerContainerModelListener listener = (SWHVLayerContainerModelListener) listenerPanel[i];
                listener.layerActivated(position);
            }
        }
    }

    public SWHVLayerModel findActive() {
        int i = 0;
        while (i < this.layers.length && !this.layers[i].isActive()) {
            i++;
        }
        if (i < this.layers.length) {
            return layers[i];
        }
        return null;
    }

    public SWHVLayerModel getActiveLayer() {
        int i = 0;
        while (i < layers.length && !layers[i].isActive()) {
            i++;
        }
        if (i < layers.length) {
            return layers[i];
        }
        return null;
    }

    public static SWHVLayerContainerModel getSingletonInstance() {
        return singletonInstance;
    }

}
