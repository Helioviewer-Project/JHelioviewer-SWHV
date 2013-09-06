package org.helioviewer.gl3d.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;

import org.helioviewer.jhv.gui.ImageViewerGui;

/**
 * Currently not in use!
 * 
 * Would show the Scene Graph managed by the {@link GL3DSceneGraphView} in a
 * JTree
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DSceneGraphPanel extends JPanel {

    private static final long serialVersionUID = -1999336789185689739L;

    public static final int ROW_HEIGHT = 25;

    public GL3DSceneGraphPanel() {
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        JTree sceneGraphTree = new JTree(new GL3DSceneGraphTreeModel());
        JScrollPane scrollPane = new JScrollPane(sceneGraphTree);
        // GL3DModelVisibilityCellEditor visibilityCellEditor = new
        // GL3DModelVisibilityCellEditor();
        // modelTable.setDefaultEditor(Boolean.class, visibilityCellEditor);
        // modelTable.setDefaultRenderer(String.class, new
        // GL3DModelCellRenderer());
        // // set proper layout
        // modelTable.setTableHeader(null);
        // modelTable.setShowGrid(false);
        // modelTable.setRowSelectionAllowed(true);
        // modelTable.setColumnSelectionAllowed(false);
        // modelTable.setIntercellSpacing(new Dimension(0, 0));
        // modelTable.setRowHeight(ROW_HEIGHT);
        // modelTable.setBackground(Color.white);
        //
        // modelTable.getColumnModel().getColumn(GL3DModelTableModel.COLUMN_VISIBILITY).setMaxWidth(25);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        scrollPane.setPreferredSize(new Dimension(ImageViewerGui.SIDE_PANEL_WIDTH, ROW_HEIGHT * 4 + 2));

        add(scrollPane, BorderLayout.CENTER);
    }

}
