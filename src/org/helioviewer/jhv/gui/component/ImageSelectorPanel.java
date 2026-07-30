package org.helioviewer.jhv.gui.component;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataSourcesParser;
import org.helioviewer.jhv.io.DataSourcesTree;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public final class ImageSelectorPanel extends JPanel implements DataSources.Listener {

    private final Interfaces.DatasetSelectionHandler selectionHandler;
    private final DataSourcesTree sourcesTree;
    private final JButton availabilityButton = new JButton("Available data");

    public ImageSelectorPanel(Interfaces.DatasetSelectionHandler _selectionHandler) {
        setLayout(new BorderLayout());
        selectionHandler = _selectionHandler;
        sourcesTree = new DataSourcesTree(selectionHandler);

        JScrollPane scrollPane = new JScrollPane(sourcesTree);
        scrollPane.setPreferredSize(new Dimension(250, 350));
        add(scrollPane, BorderLayout.CENTER);

        availabilityButton.setEnabled(false);
        availabilityButton.addActionListener(e -> DesktopIntegration.openURL(getAvailabilityURL()));
        sourcesTree.addTreeSelectionListener(e -> availabilityButton.setEnabled(getAvailabilityURL() != null));

        DataSources.addListener(this);
    }

    public JComponent getFocusedComponent() {
        return sourcesTree;
    }

    @Override
    public void setupSources(DataSourcesParser parser) {
        if (!sourcesTree.setParsedData(parser))
            return;

        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        if (item == null)
            return;

        long start = item.end - 2 * TimeUtils.DAY_IN_MILLIS;
        selectionHandler.setDefaultTimeRange(start, item.end);
    }

    @Nullable
    private String getAvailabilityURL() {
        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        if (item == null) return null;

        String availability = DataSources.getServerSetting(item.server, "availability.images");
        return availability == null ? null : availability + "ID=" + item.sourceId;
    }

    public void selectDataset(String server, int sourceId) {
        sourcesTree.setSelectedItem(server, sourceId);
    }

    public void loadSelectedDataset() {
        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        if (item != null)
            selectionHandler.loadDataset(item.server, item.sourceId);
    }

    public JButton getAvailabilityButton() {
        return availabilityButton;
    }
}
