package org.helioviewer.jhv.layers.selector;

import java.awt.Component;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.component.CollapsiblePane;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;

// Fills the three section wrappers for the selected layer. Image layers get a
// split rendering/geometry pair (cached per layer); other layer types get their
// generic options panel in the Layer options wrapper only.
public final class LayerOptionSections implements Layers.Listener, Interfaces.LazyComponent {

    private record ImagePanels(ImageLayerRenderingPanel rendering, ImageLayerGeometryPanel geometry, ImageLayerManagePanel manage) {}

    private final JPanel layerOptionsWrapper;
    private final JPanel geometryWrapper;
    private final JPanel manageWrapper;
    private final Map<ImageLayer, ImagePanels> cache = new IdentityHashMap<>();
    @Nullable
    private ImageLayerManagePanel currentManage; // the manage panel currently shown, polled for live readout

    public LayerOptionSections(JPanel layerOptionsWrapper, JPanel geometryWrapper, JPanel manageWrapper) {
        this.layerOptionsWrapper = layerOptionsWrapper;
        this.geometryWrapper = geometryWrapper;
        this.manageWrapper = manageWrapper;
        Layers.addListener(this);
        UITimer.register(this); // poll the readout so its frame count updates live as a download lands
    }

    // Called ~10 Hz by UITimer; updateReadout is memoized, so it only rebuilds when the count changes.
    @Override
    public void lazyRepaint() {
        if (currentManage != null)
            currentManage.updateReadout();
    }

    public void setSelectedLayer(@Nullable Layer layer) {
        layerOptionsWrapper.removeAll();
        geometryWrapper.removeAll();
        manageWrapper.removeAll();
        currentManage = null;

        if (layer instanceof ImageLayer il) {
            ImagePanels p = cache.computeIfAbsent(il, k -> new ImagePanels(new ImageLayerRenderingPanel(il), new ImageLayerGeometryPanel(il), new ImageLayerManagePanel(il)));
            ComponentUtils.setEnabled(p.rendering(), il.isEnabled());
            ComponentUtils.setEnabled(p.geometry(), il.isEnabled());
            ComponentUtils.setEnabled(p.manage(), il.isEnabled());
            layerOptionsWrapper.add(p.rendering());
            geometryWrapper.add(p.geometry());
            manageWrapper.add(p.manage());
            currentManage = p.manage();
            p.manage().updateReadout();
        } else if (layer != null) {
            Component generic = LayerOptions.getOptionsPanel(layer);
            if (generic != null) {
                ComponentUtils.setEnabled(generic, layer.isEnabled());
                layerOptionsWrapper.add(generic);
            }
        }
        // Retitle the enclosing "Layer options" section to match the selected layer, e.g.
        // "SUVI 171 Layer Options", "Grid Layer Options".
        CollapsiblePane optionsPane = enclosingPane(layerOptionsWrapper);
        if (optionsPane != null) {
            optionsPane.setTitle(layer == null ? "Layer Options" : layer.getName() + " Layer Options");
            // Default the options open on every layer switch; hiding them is opt-in each time.
            if (layer != null)
                optionsPane.setExpanded(true);
        }

        // Hide the geometry controls entirely (not just leave them empty) unless the selected layer
        // actually has geometry options.
        geometryWrapper.setVisible(layer instanceof ImageLayer);
        revalidateAll();
    }

    @Nullable
    private static CollapsiblePane enclosingPane(Component c) {
        for (Component p = c; p != null; p = p.getParent())
            if (p instanceof CollapsiblePane pane)
                return pane;
        return null;
    }

    private void revalidateAll() {
        layerOptionsWrapper.revalidate();
        layerOptionsWrapper.repaint();
        geometryWrapper.revalidate();
        geometryWrapper.repaint();
        manageWrapper.revalidate();
        manageWrapper.repaint();
    }

    @Override
    public void layerAdded(int index, Layer layer) {}

    @Override
    public void layerRemoved(int index, Layer layer) {
        if (layer instanceof ImageLayer il)
            cache.remove(il);
    }

    @Override
    public void layersCleared() {
        cache.clear();
    }

    @Override
    public void nameUpdated(Layer layer) {}

    @Override
    public void layerUpdated(Layer layer) {
        if (layer instanceof ImageLayer il && cache.get(il) instanceof ImagePanels p) {
            p.rendering().refresh(layer);
            p.manage().refresh(layer);
            p.manage().forceReadoutRefresh();
        }
    }

    @Override
    public void timeUpdated(Layer layer) {
        if (layer instanceof ImageLayer il && cache.get(il) instanceof ImagePanels p) {
            p.manage().updateReadout();
        }
    }
}
