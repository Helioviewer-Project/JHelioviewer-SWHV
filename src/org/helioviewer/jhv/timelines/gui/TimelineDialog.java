package org.helioviewer.jhv.timelines.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.band.BandDataset;
import org.helioviewer.jhv.timelines.band.BandType;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import com.jidesoft.swing.SearchableUtils;

@SuppressWarnings("serial")
public final class TimelineDialog extends StandardDialog implements Interfaces.ShowableDialog {

    private final TimelineLayers layers;
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Timelines");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    private final JTree tree = new JTree(treeModel);
    private final AbstractAction load = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            List<BandType> bandTypes = selectedBandTypes();
            if (bandTypes.isEmpty())
                return;
            layers.addBands(bandTypes);
            setVisible(false);
        }
    };

    public TimelineDialog(TimelineLayers _layers) {
        super(MainFrame.get(), "New Timeline Layer", false);
        layers = _layers;
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);
        setType(Window.Type.UTILITY);
    }

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = ComponentUtils.hideAction(this);
        setDefaultCancelAction(close);
        setDefaultAction(load);
        setInitFocusedComponent(tree);

        JButton cancelBtn = new JButton(close);
        cancelBtn.setText("Cancel");
        JButton okBtn = new JButton(load);
        okBtn.setText("Add");
        getRootPane().setDefaultButton(okBtn);

        ButtonPanel panel = new ButtonPanel();
        panel.add(okBtn, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(cancelBtn, ButtonPanel.CANCEL_BUTTON);

        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setToggleClickCount(0);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        if (tree.getCellRenderer() instanceof DefaultTreeCellRenderer renderer) {
            renderer.setOpenIcon(null);
            renderer.setClosedIcon(null);
            renderer.setLeafIcon(null);
        }
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tree.getRowForLocation(e.getX(), e.getY()) != -1)
                    load.actionPerformed(null);
            }
        });
        SearchableUtils.installSearchable(tree).setRecursive(true);
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(350, 350));

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        content.add(scrollPane);
        return content;
    }

    @Nullable
    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    @Override
    public void showDialog() {
        if (!isVisible()) {
            pack();
            setLocationRelativeTo(MainFrame.get());
            setVisible(true);
        } else {
            toFront();
        }
    }

    public void setCatalogs(Map<String, BandDataset[]> catalogs) {
        root.removeAllChildren();
        for (Map.Entry<String, BandDataset[]> catalog : catalogs.entrySet()) {
            DefaultMutableTreeNode catalogNode = new DefaultMutableTreeNode(catalog.getKey());
            root.add(catalogNode);
            LinkedHashMap<String, DefaultMutableTreeNode> groupNodes = new LinkedHashMap<>();
            BandDataset[] datasets = catalog.getValue();
            for (BandDataset dataset : datasets) {
                LinkedHashMap<String, List<BandType>> groupedTypes = new LinkedHashMap<>();
                List<BandType> ungroupedTypes = new ArrayList<>();
                for (BandType type : dataset.bandTypes()) {
                    if (type.getGroups().isEmpty()) {
                        ungroupedTypes.add(type);
                    } else {
                        for (String group : type.getGroups())
                            groupedTypes.computeIfAbsent(group, ignored -> new ArrayList<>()).add(type);
                    }
                }
                groupedTypes.forEach((group, types) -> groupNode(catalogNode, groupNodes, group)
                        .add(new DefaultMutableTreeNode(new BandDataset(dataset.title(), types))));
                if (!ungroupedTypes.isEmpty())
                    catalogNode.add(new DefaultMutableTreeNode(new BandDataset(dataset.title(), ungroupedTypes)));
            }
        }
        treeModel.reload();
        if (root.getChildCount() > 0)
            tree.expandPath(new TreePath(((DefaultMutableTreeNode) root.getFirstChild()).getPath()));
    }

    private List<BandType> selectedBandTypes() {
        TreePath path = tree.getSelectionPath();
        if (path == null)
            return List.of();

        DefaultMutableTreeNode selected = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (selected.getParent() == root)
            return List.of();

        LinkedHashSet<BandType> bandTypes = new LinkedHashSet<>();
        Enumeration<?> nodes = selected.depthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            Object value = ((DefaultMutableTreeNode) nodes.nextElement()).getUserObject();
            if (value instanceof BandDataset dataset)
                bandTypes.addAll(dataset.bandTypes());
        }
        return List.copyOf(bandTypes);
    }

    private static DefaultMutableTreeNode groupNode(DefaultMutableTreeNode catalogNode, LinkedHashMap<String, DefaultMutableTreeNode> groupNodes,
                                                    String group) {
        return groupNodes.computeIfAbsent(group, name -> {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
            catalogNode.add(node);
            return node;
        });
    }

}
