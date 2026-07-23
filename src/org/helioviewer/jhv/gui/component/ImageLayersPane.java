package org.helioviewer.jhv.gui.component;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.MainFrame;

import com.jidesoft.swing.JideButton;

// Inner container for the nested sections shown under the "Image Layers" pane.
// The layer-options and geometry wrappers are owned and filled by MainFrame's
// LayerOptionSections controller; this pane only assembles the child sections.
// Transport control is a separate top-level pane (it drives timelines and events too),
// not a child here.
@SuppressWarnings("serial")
public final class ImageLayersPane extends JPanel {

    // Indent the child section bars so they read as nested under the "Image Layers" header.
    private static final int CHILD_INDENT = 12;

    public ImageLayersPane(JComponent timeRange, JComponent layers, JPanel layerOptionsWrapper, JPanel geometryWrapper, JPanel manageWrapper) {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(BorderFactory.createEmptyBorder(0, CHILD_INDENT, 0, 0));
        // The master time range belongs with the image layers in spirit — it defines the span new
        // layers load into — so it sits at the very top, with the Sync button inline to its right
        // (moved off the New Layer row so that row, the widest, can shrink the sidebar).
        JideButton syncButton = new JideButton(Buttons.syncLayers);
        syncButton.setToolTipText("Synchronize time intervals of all layers to the range above");
        syncButton.addActionListener(e -> MainFrame.getLayersSectionPanel().syncLayers());
        JPanel timeRow = new JPanel(new BorderLayout());
        timeRow.add(timeRange, BorderLayout.CENTER);
        timeRow.add(syncButton, BorderLayout.LINE_END);
        add(timeRow);
        add(layers);

        // Layer options holds the rendering controls plus the geometry/crop controls (no separate
        // title). Collapsing Layer options hides both; geometry hides entirely when the selected
        // layer has none (LayerOptionSections toggles geometryWrapper's visibility).
        JPanel layerOptionsContent = new JPanel();
        layerOptionsContent.setLayout(new BoxLayout(layerOptionsContent, BoxLayout.PAGE_AXIS));
        layerOptionsContent.add(manageWrapper); // readout + download/metadata/refresh, at the top
        layerOptionsContent.add(layerOptionsWrapper);
        layerOptionsContent.add(geometryWrapper);
        add(new CollapsiblePane("Layer options", layerOptionsContent, true, true));
    }
}
