package org.helioviewer.jhv.layers.selector;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.border.Border;

import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.gui.component.SamplingPanel;
import org.helioviewer.jhv.gui.dialog.ImageDialog;
import org.helioviewer.jhv.gui.time.TimeSelectorPanel;
import org.helioviewer.jhv.io.ImageRequestSettings;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.ViewpointLayer;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
public final class LayersPanel extends JPanel implements Layers.Listener {

    private final ImageDialog imageDialog = new ImageDialog();
    private final LayerListPanel images = new LayerListPanel(true);
    private final LayerListPanel overlays = new LayerListPanel(false);
    private final JPanel header = new JPanel(new BorderLayout());
    private final JPanel timelines = new JPanel(new BorderLayout());
    private final JPanel events = new JPanel(new BorderLayout());
    private final JTabbedPane tabs;
    private ViewpointLayerOptionsPanel viewpointOptions;

    public LayersPanel() {
        super(new BorderLayout());

        JideButton addLayerButton = new JideButton(Buttons.newLayer);
        addLayerButton.addActionListener(e -> imageDialog.showNewLayerSelector());
        JideButton syncButton = new JideButton(Buttons.syncLayers);
        syncButton.setToolTipText("Apply the selected time range and sampling to all layers");
        syncButton.addActionListener(e -> ImageLayers.syncLayersSpan());
        JPanel imageActions = new JPanel(new BorderLayout());
        imageActions.add(addLayerButton, BorderLayout.LINE_START);
        imageActions.add(syncButton, BorderLayout.LINE_END);
        JPanel imagePanel = new JPanel(new BorderLayout());

        // The selector edits the settings and shows what they accepted.
        ImageRequestSettings settings = ImageRequestSettings.instance();
        TimeSelectorPanel timeSelectorPanel = new TimeSelectorPanel();
        timeSelectorPanel.setTime(settings.start(), settings.end());
        timeSelectorPanel.addListener(settings::setInterval);
        settings.addListener(new ImageRequestSettings.Listener() {
            @Override
            public void intervalChanged(long start, long end) {
                timeSelectorPanel.setTime(start, end);
            }
        });
        GridBagConstraints samplingConstraints = new GridBagConstraints();
        samplingConstraints.gridy = 2;
        samplingConstraints.gridx = 0;
        samplingConstraints.weightx = 1;
        samplingConstraints.fill = GridBagConstraints.HORIZONTAL;
        SamplingPanel samplingPanel = new SamplingPanel();
        timeSelectorPanel.add(samplingPanel, samplingConstraints);

        JPanel imageHeader = new JPanel(new BorderLayout());
        imageHeader.add(timeSelectorPanel, BorderLayout.NORTH);
        imageHeader.add(imageActions, BorderLayout.SOUTH);
        imagePanel.add(imageHeader, BorderLayout.NORTH);
        imagePanel.add(images, BorderLayout.CENTER);

        refreshViewpoint();
        tabs = new JTabbedPane() {
            @Override
            public Dimension getPreferredSize() {
                return selectedHeight(super.getPreferredSize(), false);
            }

            @Override
            public Dimension getMinimumSize() {
                return selectedHeight(super.getMinimumSize(), true);
            }

            private Dimension selectedHeight(Dimension size, boolean minimum) {
                Component selected = getSelectedComponent();
                if (selected != null) {
                    int maximumHeight = 0;
                    for (int index = 0; index < getTabCount(); index++) {
                        Component content = getComponentAt(index);
                        Dimension contentSize = minimum ? content.getMinimumSize() : content.getPreferredSize();
                        maximumHeight = Math.max(maximumHeight, contentSize.height);
                    }
                    // Keep the tab strip, borders and widest content; only replace the content height.
                    Dimension selectedSize = minimum ? selected.getMinimumSize() : selected.getPreferredSize();
                    size.height += selectedSize.height - maximumHeight;
                }
                return size;
            }
        };
        tabs.addTab("Images", imagePanel);
        tabs.addTab("Overlays", overlays);
        tabs.addTab("Timelines", timelines);
        tabs.addTab("Events", events);
        tabs.setEnabledAt(2, false);
        tabs.setEnabledAt(3, false);
        tabs.setBackground(UIManager.getColor("Table.background"));
        tabs.setOpaque(true);
        Border contentBorder = BorderFactory.createMatteBorder(1, 0, 1, 0, UIManager.getColor("Separator.foreground"));
        for (int index = 0; index < tabs.getTabCount(); index++)
            ((JComponent) tabs.getComponentAt(index)).setBorder(contentBorder);
        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        Layers.addListener(this);
    }

    private void refreshViewpoint() {
        if (viewpointOptions != null)
            header.remove(viewpointOptions);
        viewpointOptions = new ViewpointLayerOptionsPanel(Layers.getViewpointLayer());
        header.add(viewpointOptions, BorderLayout.NORTH);
        header.revalidate();
        header.repaint();
    }

    @Override
    public void layersCleared() {
        refreshViewpoint();
    }

    @Override
    public void layerUpdated(Layer layer) {
        if (layer == Layers.getViewpointLayer())
            viewpointOptions.refreshMode();
    }

    public int getGridRowHeight() {
        return images.getGridRowHeight();
    }

    public ImageDialog getImageDialog() {
        return imageDialog;
    }

    public void setTransportPanel(JComponent panel) {
        header.add(panel, BorderLayout.SOUTH);
    }

    public void setTimelinesPanel(@Nullable JComponent panel) {
        setTabContent(timelines, panel);
    }

    public void setEventsPanel(@Nullable JComponent panel) {
        setTabContent(events, panel);
    }

    private void setTabContent(JPanel target, @Nullable JComponent panel) {
        target.removeAll();
        if (panel != null)
            target.add(panel, BorderLayout.CENTER);
        int index = tabs.indexOfComponent(target);
        if (panel == null && tabs.getSelectedIndex() == index)
            tabs.setSelectedIndex(0);
        tabs.setEnabledAt(index, panel != null);
        revalidate();
        repaint();
    }

    public void setOptionsPanel(@Nullable Layer layer) {
        if (layer == null) {
            images.setOptionsPanel(null);
            overlays.setOptionsPanel(null);
        } else if (layer instanceof ImageLayer) {
            images.setOptionsPanel(layer);
        } else if (!(layer instanceof ViewpointLayer)) {
            overlays.setOptionsPanel(layer);
        }
    }

}
