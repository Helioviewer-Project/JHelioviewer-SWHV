package org.helioviewer.jhv.gui.component;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.ui.Panel;
import org.helioviewer.jhv.ui.Screen;

// Editable section headers and rows, using the same controls as the other Swing containers.
@SuppressWarnings("serial")
final class PanelComponentTree extends JTree {

    private final DefaultMutableTreeNode root;
    private final Screen screen;
    private final List<SwingPanel> fragments = new ArrayList<>();

    PanelComponentTree(Panel p, int control, Screen _screen) {
        super(new DefaultMutableTreeNode("Root"));
        root = (DefaultMutableTreeNode) getModel().getRoot();
        screen = _screen;
        setRootVisible(false);
        setEditable(true);
        setShowsRootHandles(true);
        setSelectionModel(null);
        setCellRenderer(new Renderer());
        setCellEditor(new Editor());
        int end = p.treeViewEnd(control);
        for (int i = control + 1; i < end;) {
            if (p.kind(i) != Panel.Kind.SECTION)
                throw new IllegalArgumentException("Editable tree needs sections");
            int section = i++;
            int headerEnd = end(p, i);
            JPanel header = new JPanel(new GridLayout(1, 0, 0, 0));
            header.setOpaque(false);
            header.add(new JLabel(p.string(section, Panel.IntField.LABEL)));
            for (int cell = i + 1; cell < headerEnd;) {
                int cellEnd = end(p, cell);
                header.add(fragment(p, cell, cellEnd + 1));
                cell = cellEnd + 1;
            }
            DefaultMutableTreeNode branch = new DefaultMutableTreeNode(header);
            root.add(branch);
            i = headerEnd + 1;
            while (p.kind(i) != Panel.Kind.SECTION_END) {
                int rowEnd = end(p, i);
                branch.add(new DefaultMutableTreeNode(fragment(p, i, rowEnd + 1)));
                i = rowEnd + 1;
            }
            i++;
        }
        ((DefaultTreeModel) getModel()).reload();
    }

    private SwingPanel fragment(Panel p, int start, int end) {
        SwingPanel fragment = new SwingPanel(screen, p, start, end);
        fragments.add(fragment);
        return fragment;
    }

    private static int end(Panel p, int start) {
        int depth = 0;
        for (int i = start; i < p.size(); i++) {
            if (p.kind(i) == Panel.Kind.ROW)
                depth++;
            else if (p.kind(i) == Panel.Kind.ROW_END && --depth == 0)
                return i;
        }
        throw new IllegalArgumentException("Unclosed editable tree row");
    }

    void update(Panel p, int control) {
        for (SwingPanel fragment : fragments)
            fragment.updateFragment(p);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        Enumeration<?> nodes = root.breadthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
            if (node.getUserObject() instanceof Component component)
                ComponentUtils.setEnabled(component, enabled);
        }
    }

    private static final class Renderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean focus) {
            if (((DefaultMutableTreeNode) value).getUserObject() instanceof Component component)
                return component;
            return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, focus);
        }
    }

    private static final class Editor extends DefaultCellEditor {
        private Component content;

        Editor() {
            super(new JCheckBox());
        }

        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row) {
            content = (Component) ((DefaultMutableTreeNode) value).getUserObject();
            return content;
        }

        @Override
        public Object getCellEditorValue() {
            return content;
        }
    }
}
