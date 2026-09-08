package org.helioviewer.jhv.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeSelectionModel;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.component.HTMLPane;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataSourcesParser;
import org.helioviewer.jhv.io.DataSourcesTree;
import org.helioviewer.jhv.time.TimeUtils;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public final class ImageDialog extends StandardDialog implements DataSources.Listener {

    public interface Handler {
        void setDefaultTimeRange(long start, long end);

        void loadDatasets(List<DataSourcesTree.SourceItem> items);
    }

    private final Handler handler;
    private final DataSourcesTree sourcesTree;
    private final HTMLPane datasetExtent = new HTMLPane();
    private final JButton availabilityButton = new JButton("Available data");
    private final AbstractAction load = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            loadSelectedDatasets();
        }
    };
    private final JButton actionButton = new JButton(load);

    public ImageDialog(Handler _handler) {
        super(MainFrame.get(), "New Image Layer", false);
        handler = _handler;
        sourcesTree = new DataSourcesTree(this::loadSelectedDatasets);
        sourcesTree.addTreeSelectionListener(e -> selectionChanged());
        selectionChanged();
        availabilityButton.addActionListener(e -> {
            List<DataSourcesTree.SourceItem> items = sourcesTree.getSelectedItems();
            String url = items.size() == 1 ? getAvailabilityURL(items.getFirst()) : null;
            if (url != null)
                DesktopIntegration.openURL(url);
        });
        DataSources.setListener(this);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);
        setType(Window.Type.UTILITY);
    }

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = ComponentUtils.hideAction(this);
        setDefaultCancelAction(close);
        setDefaultAction(load);
        setInitFocusedComponent(sourcesTree);

        JButton cancelButton = new JButton(close);
        cancelButton.setText("Cancel");
        getRootPane().setDefaultButton(actionButton);

        ButtonPanel panel = new ButtonPanel();
        panel.add(availabilityButton, ButtonPanel.OTHER_BUTTON);
        panel.add(cancelButton, ButtonPanel.CANCEL_BUTTON);
        panel.add(actionButton, ButtonPanel.AFFIRMATIVE_BUTTON);
        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        JScrollPane scrollPane = new JScrollPane(sourcesTree);
        scrollPane.setPreferredSize(new Dimension(250, 350));

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(datasetExtent, BorderLayout.SOUTH);
        return content;
    }

    @Nullable
    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    public void showDialog(boolean changeMode) {
        setTitle(changeMode ? "Change Dataset" : "New Image Layer");
        actionButton.setText(changeMode ? "Change" : "Add");
        sourcesTree.getSelectionModel().setSelectionMode(
                changeMode ? TreeSelectionModel.SINGLE_TREE_SELECTION : TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        selectionChanged();
        if (!isVisible()) {
            pack();
            JPanel layersPanel = MainFrame.getLayersPanel();
            Point location = new Point(layersPanel.getWidth(), 0);
            SwingUtilities.convertPointToScreen(location, layersPanel);
            setLocation(location);
            setVisible(true);
        } else {
            toFront();
        }
    }

    public void selectDataset(String server, int sourceId) {
        sourcesTree.setSelectedItem(server, sourceId);
    }

    private void selectionChanged() {
        List<DataSourcesTree.SourceItem> items = sourcesTree.getSelectedItems();
        DataSourcesTree.SourceItem item = items.size() == 1 ? items.getFirst() : null;
        load.setEnabled(!items.isEmpty());
        availabilityButton.setEnabled(getAvailabilityURL(item) != null);
        String text = item != null ? "Extent: " + TimeUtils.formatShort(item.start) + " - " + TimeUtils.formatShort(item.end)
                : items.isEmpty() ? "" : items.size() + " datasets selected";
        datasetExtent.setText("<div style='text-align:center'>" + text + "</div>");
    }

    @Nullable
    private static String getAvailabilityURL(@Nullable DataSourcesTree.SourceItem item) {
        if (item == null) return null;

        DataSources.Server server = DataSources.getServer(item.server);
        String availability = server == null ? null : server.availabilityURL();
        return availability == null ? null : availability + "ID=" + item.sourceId;
    }

    @Override
    public void setupSources(DataSourcesParser parser) {
        DataSourcesTree.SourceItem item = sourcesTree.setParsedData(parser);
        if (item != null)
            handler.setDefaultTimeRange(item.end - 2 * TimeUtils.DAY_IN_MILLIS, item.end);
    }

    private void loadSelectedDatasets() {
        List<DataSourcesTree.SourceItem> items = sourcesTree.getSelectedItems();
        if (items.isEmpty())
            return;
        setVisible(false);
        handler.loadDatasets(items);
    }
}
