package org.helioviewer.jhv.gui.components;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.CommandLine;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataSourcesListener;
import org.helioviewer.jhv.io.DataSourcesParser;
import org.helioviewer.jhv.io.DataSourcesTree;
import org.helioviewer.jhv.gui.dialogs.ObservationDialog;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public class ImageSelectorPanel extends JPanel implements DataSourcesListener {

    private final DataSourcesTree sourcesTree = new DataSourcesTree();
    private static boolean first = true;

    public ImageSelectorPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
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

        CommandLine.loadRequest();

        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        if (item == null) { // not valid
            Message.err("Could not retrieve data sources", "The list of available data could not be fetched, so you cannot use the GUI to add data." +
                        System.getProperty("line.separator") +
                        "This may happen if you do not have an internet connection or there are server problems. You can still open local files.", false);
        } else if (first) {
            first = false;

            long startTime = item.end - 2 * TimeUtils.DAY_IN_MILLIS;
            long endTime = item.end;
            ObservationDialog.getInstance().setStartTime(startTime);
            ObservationDialog.getInstance().setEndTime(endTime);

            if (Boolean.parseBoolean(Settings.getSingletonInstance().getProperty("startup.loadmovie"))) {
                doLoad(null, startTime, endTime, ObservationDialog.getInstance().getCadence());
            }
        }
    }

    public String getAvailabilityURL() {
        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        return item == null ? null : DataSources.getServerSetting(item.server, "availability.images") + "#IID" + item.sourceId;
    }

    public void setupLayer(APIRequest req) {
        sourcesTree.setSelectedItem(req.server, req.sourceId);
    }

    public boolean doLoad(ImageLayer layer, long startTime, long endTime, int cadence) {
        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        if (item == null) { // not valid
            Message.err("Data is not selected", "There is no information on what to add", false);
            return false;
        }

        ImageLayer imageLayer = layer == null ? ImageLayer.create(null) : layer;
        imageLayer.load(new APIRequest(item.server, item.sourceId, startTime, endTime, cadence));
        return true;
    }

}
