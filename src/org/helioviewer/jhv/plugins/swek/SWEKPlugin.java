package org.helioviewer.jhv.plugins.swek;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTree;

import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.components.base.JHVTreeCell;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.timelines.Timelines;
import org.json.JSONObject;

public class SWEKPlugin extends Plugin {

    private static final JPanel swekPanel = new JPanel(new BorderLayout());

    private static final SWEKLayer layer = new SWEKLayer(null);
    private static final EventTimelineLayer etl = new EventTimelineLayer(null);

    public SWEKPlugin() {
        super("Space Weather Event Knowledgebase", "Visualize space weather relevant events");

        JTree tree = new JTree(SWEKConfig.load());
        tree.setRootVisible(false);
        tree.setEditable(true);
        tree.setShowsRootHandles(true);
        tree.setSelectionModel(null);
        tree.setCellRenderer(new JHVTreeCell.Renderer());
        tree.setCellEditor(new JHVTreeCell.Editor());
        tree.setRowHeight(0); // force calculation of nodes heights
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        swekPanel.add(tree);
    }

    @Override
    public void install() {
        JHVFrame.getLeftContentPane().add("Space Weather Event Knowledgebase", swekPanel, true);
        JHVFrame.getLeftContentPane().revalidate();
        JHVFrame.getLayers().add(layer);
        Timelines.getLayers().add(etl);
    }

    @Override
    public void uninstall() {
        JHVFrame.getLeftContentPane().remove(swekPanel);
        JHVFrame.getLeftContentPane().revalidate();
        JHVFrame.getLayers().remove(layer);
        Timelines.getLayers().remove(etl);
    }

    @Override
    public void saveState(JSONObject jo) {
        // functionality to be restored later
    }

    @Override
    public void loadState(JSONObject jo) {
        // functionality to be restored later
    }

}
