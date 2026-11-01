package org.helioviewer.jhv.gui.component;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.text.Position;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.helioviewer.jhv.ui.Panel;
import org.helioviewer.jhv.ui.Screen;

import com.jidesoft.swing.SearchableUtils;

// A tree view's branches and items as one tree. The screen owns the rows and the selection; the tree keeps what is expanded.
@SuppressWarnings("serial")
final class PanelTree extends JTree {

    private record Row(int depth, int item, String label, String tip, boolean leaf) {
        @Override
        public String toString() {
            return label;
        }
    }

    private final Screen screen;
    private final Consumer<Runnable> act;
    private final int owner;
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    private final DefaultTreeModel model = new DefaultTreeModel(root);
    private final HashMap<Integer, TreePath> paths = new HashMap<>();
    private List<Row> rows = List.of();

    PanelTree(Panel p, int control, Screen _screen, Consumer<Runnable> _act) {
        screen = _screen;
        act = _act;
        owner = p.value(control, Panel.IntField.OWNER);
        setModel(model);
        setRootVisible(false);
        setShowsRootHandles(true);
        setToggleClickCount(p.value(control, Panel.IntField.DOUBLE_CLICK_COLUMNS) == 0 ? 0 : 2);
        ToolTipManager.sharedInstance().registerComponent(this);
        getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        if (getCellRenderer() instanceof DefaultTreeCellRenderer renderer) {
            renderer.setOpenIcon(null);
            renderer.setClosedIcon(null);
            renderer.setLeafIcon(null);
        }
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = e.getClickCount() == 2 && getRowForLocation(e.getX(), e.getY()) != -1 ? getPathForLocation(e.getX(), e.getY()) : null;
                if (path != null)
                    act.accept(() -> screen.command(owner, item(path), Panel.Command.DOUBLE_CLICK));
            }
        });
        addTreeSelectionListener(e -> act.accept(() -> {
            TreePath[] changed = e.getPaths();
            for (TreePath path : changed) {
                if (!e.isAddedPath(path))
                    screen.command(owner, item(path), Panel.Command.SELECT_REMOVE);
            }
            TreePath[] selection = getSelectionPaths();
            if (selection != null) {
                for (int i = 0; i < selection.length; i++)
                    screen.command(owner, item(selection[i]), i == 0 ? Panel.Command.SELECT_ONLY : Panel.Command.SELECT_ADD);
            }
        }));
        SearchableUtils.installSearchable(this).setRecursive(true);
    }

    private static int item(TreePath path) {
        return ((Row) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject()).item;
    }

    void update(Panel p, int control) {
        getSelectionModel().setSelectionMode(p.has(control, Panel.MULTIPLE)
                ? TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION : TreeSelectionModel.SINGLE_TREE_SELECTION);
        int end = p.treeViewEnd(control);
        ArrayList<Row> newRows = new ArrayList<>();
        int depth = 0;
        for (int i = control + 1; i < end; i++) {
            switch (p.kind(i)) {
                case TREE -> newRows.add(new Row(depth++, p.value(i, Panel.IntField.ID), p.string(i, Panel.IntField.LABEL), p.string(i, Panel.IntField.TIP), false));
                case ITEM -> newRows.add(new Row(depth, p.value(i, Panel.IntField.ID), p.string(i, Panel.IntField.LABEL), p.string(i, Panel.IntField.TIP), true));
                case TREE_END -> depth--;
                default -> throw new IllegalArgumentException("Swing tree has no " + p.kind(i));
            }
        }
        if (!newRows.equals(rows)) {
            HashMap<List<String>, Boolean> expansion = new HashMap<>();
            for (TreePath path : paths.values()) {
                if (((DefaultMutableTreeNode) path.getLastPathComponent()).getAllowsChildren())
                    expansion.put(labels(path), isExpanded(path));
            }
            rows = newRows;
            rebuild();
            for (int i = control + 1; i < end; i++) {
                if (p.kind(i) == Panel.Kind.TREE) {
                    TreePath path = paths.get(p.value(i, Panel.IntField.ID));
                    if (expansion.getOrDefault(labels(path), p.has(i, Panel.OPEN)))
                        expandPath(path);
                }
            }
        }
        for (int i = control + 1; i < end; i++) {
            if (p.kind(i) != Panel.Kind.TREE_END) {
                TreePath path = paths.get(p.value(i, Panel.IntField.ID));
                boolean selected = p.has(i, Panel.SELECTED);
                if (isPathSelected(path) != selected) {
                    if (selected)
                        addSelectionPath(path);
                    else
                        removeSelectionPath(path);
                }
            }
        }
    }

    @Override
    public TreePath getNextMatch(String prefix, int startingRow, Position.Bias bias) {
        return null;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        TreePath path = e == null ? null : getPathForLocation(e.getX(), e.getY());
        return path == null ? null : ((Row) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject()).tip;
    }

    private static List<String> labels(TreePath path) {
        ArrayList<String> labels = new ArrayList<>();
        for (int i = 1; i < path.getPathCount(); i++)
            labels.add(path.getPathComponent(i).toString());
        return labels;
    }

    private void rebuild() {
        root.removeAllChildren();
        paths.clear();
        ArrayList<DefaultMutableTreeNode> parents = new ArrayList<>(List.of(root));
        for (Row row : rows) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(row, !row.leaf);
            parents.get(row.depth).add(node);
            parents.subList(row.depth + 1, parents.size()).clear();
            parents.add(node);
            paths.put(row.item, new TreePath(node.getPath()));
        }
        model.reload();
    }

}
