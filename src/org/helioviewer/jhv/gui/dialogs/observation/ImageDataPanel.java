package org.helioviewer.jhv.gui.dialogs.observation;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.CommandLine;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataSourcesParser;
import org.helioviewer.jhv.io.DataSourcesTree;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.time.TimeUtils;

/**
 * In order to select and load image data from the Helioviewer server this class
 * provides the corresponding user interface. The UI will be displayed within
 * the {@link ObservationDialog}.
 * */
@SuppressWarnings("serial")
public class ImageDataPanel extends JPanel {

    private final TimePanel timePanel = new TimePanel();
    private final CadencePanel cadencePanel = new CadencePanel();
    private final DataSourcesTree sourcesTree = new DataSourcesTree();
    private static boolean first = true;

    ImageDataPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        add(timePanel);
        add(cadencePanel);
        add(new JScrollPane(sourcesTree));
    }

    JComponent getFocused() {
        return sourcesTree;
    }

    public void setupSources(DataSourcesParser parser) {
        if (!sourcesTree.setParsedData(parser)) // not preferred
            return;

        CommandLine.loadRequest();

        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        if (item == null) { // not valid
            Message.err("Could not retrieve data sources", "The list of available data could not be fetched, so you cannot use the GUI to add data." + System.getProperty("line.separator") + "This may happen if you do not have an internet connection or there are server problems. You can still open local files.", false);
        } else if (first) {
            first = false;

            timePanel.setStartTime(item.end - 2 * TimeUtils.DAY_IN_MILLIS);
            timePanel.setEndTime(item.end);

            if (Boolean.parseBoolean(Settings.getSingletonInstance().getProperty("startup.loadmovie"))) {
                loadRemote(null, item);
            }
        }
    }

    String getAvailabilityURL() {
        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        return item == null ? null : DataSources.getServerSetting(item.server, "availability.images") + "#IID" + item.sourceId;
    }

    public int getCadence() {
        return cadencePanel.getCadence();
    }

    private void loadRemote(ImageLayer layer, DataSourcesTree.SourceItem item) { // valid item
        if (layer == null)
            layer = ImageLayer.create(null);
        layer.load(new APIRequest(item.server, item.sourceId, timePanel.getStartTime(), timePanel.getEndTime(), getCadence()));
    }

    void setupLayer(ImageLayer layer) {
        APIRequest req = layer.getAPIRequest();
        if (req != null) {
            sourcesTree.setSelectedItem(req.server, req.sourceId);
            timePanel.setStartTime(req.startTime);
            timePanel.setEndTime(req.endTime);
            cadencePanel.setCadence(req.cadence);
        }
    }

    boolean doLoad(Object layer) {
        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        if (item == null) { // not valid
            Message.err("Data is not selected", "There is no information on what to add", false);
            return false;
        }

        // show message if end date before start date
        if (timePanel.getStartTime() > timePanel.getEndTime()) {
            JOptionPane.showMessageDialog(null, "End date is before start date", "", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        loadRemote((ImageLayer) layer, item);
        return true;
    }

}
