package org.helioviewer.jhv.gui.components;

import java.awt.Dimension;

import javax.annotation.Nullable;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataSourcesParser;
import org.helioviewer.jhv.io.DataSourcesTree;
import org.helioviewer.jhv.gui.interfaces.ObservationSelector;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public class ImageSelectorPanel extends JPanel implements DataSources.Listener {

    private final ObservationSelector selector;
    private final DataSourcesTree sourcesTree;

    public ImageSelectorPanel(ObservationSelector _selector) {
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
        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        return item == null ? null : DataSources.getServerSetting(item.server, "availability.images") + "#IID" + item.sourceId;
    }

    public void setupLayer(APIRequest req) {
        sourcesTree.setSelectedItem(req.server(), req.sourceId());
    }

    public void load(ImageLayer layer, long startTime, long endTime, int cadence) {
        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        if (item == null) { // not valid
            Message.err("Data is not selected", "There is no information on what to add", false);
            return;
        }

        ImageLayer imageLayer = layer == null ? ImageLayer.create(null) : layer;
        imageLayer.load(new APIRequest(item.server, item.sourceId, startTime, endTime, cadence));
    }

}
