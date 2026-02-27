package org.helioviewer.jhv.gui.components;

import java.awt.Dimension;

import javax.annotation.Nullable;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataSourcesParser;
import org.helioviewer.jhv.io.DataSourcesTree;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public final class ImageSelectorPanel extends JPanel implements DataSources.Listener {

    private final Interfaces.ObservationSelector selector;
    private final DataSourcesTree sourcesTree;

    public ImageSelectorPanel(Interfaces.ObservationSelector _selector) {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setPreferredSize(new Dimension(250, 350));
        selector = _selector;
        sourcesTree = new DataSourcesTree(selector);
        add(new JScrollPane(sourcesTree));
        DataSources.addListener(this);
    }

    public JComponent getFocused() {
        return sourcesTree;
    }

    @Override
    public void setupSources(DataSourcesParser parser) {
        if (!sourcesTree.setParsedData(parser)) // not preferred
            return;

        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        if (item != null) { // valid
            long start = item.end - 2 * TimeUtils.DAY_IN_MILLIS;
            long end = item.end;
            selector.setTime(start, end);
        }
    }

    @Nullable
    public String getAvailabilityURL() {
        DataSourcesTree.SourceItem item = getSelected();
        if (item == null) return null;

        String availability = DataSources.getServerSetting(item.server, "availability.images");
        return availability == null ? null : availability + "ID=" + item.sourceId;
    }

    public void setupLayer(APIRequest req) {
        setSelected(req.server(), req.sourceId());
    }

    public void setSelected(String server, int sourceId) {
        sourcesTree.setSelectedItem(server, sourceId);
    }

    @Nullable
    public DataSourcesTree.SourceItem getSelected() {
        return sourcesTree.getSelectedItem();
    }

    public void load(ImageLayer layer, String server, int sourceId, long startTime, long endTime, int cadence) {
        setSelected(server, sourceId);
        load(layer, new APIRequest(server, sourceId, startTime, endTime, cadence));
    }

    private static void load(ImageLayer layer, APIRequest req) {
        ImageLayer imageLayer = layer == null ? ImageLayer.create(null) : layer;
        imageLayer.load(req);
    }
}
