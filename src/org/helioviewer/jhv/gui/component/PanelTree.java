package org.helioviewer.jhv.gui.component;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.helioviewer.jhv.ui.Panel;
import org.helioviewer.jhv.ui.Screen;

import com.jidesoft.swing.SearchableUtils;

// The branches and items of a searchable scroll as one tree. The screen owns the rows and the selection; the tree keeps what is expanded.
@SuppressWarnings("serial")
final class PanelTree extends JTree {

    private record Row(int depth, int item, String label) {
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
        setToggleClickCount(0);
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
            for (TreePath path : changed) {
                if (e.isAddedPath(path))
                    screen.command(owner, item(path), Panel.Command.SELECT_ADD);
            }
        }));
        SearchableUtils.installSearchable(this).setRecursive(true);
    }

    private static int item(TreePath path) {
        return ((Row) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject()).item;
    }

    void update(Panel p, int control) {
        int end = p.scrollEnd(control);
        ArrayList<Row> newRows = new ArrayList<>();
        int depth = 0;
        for (int i = control + 1; i < end; i++) {
            switch (p.kind(i)) {
                case TREE -> newRows.add(new Row(depth++, p.value(i, Panel.IntField.ID), p.string(i, Panel.IntField.LABEL)));
                case ITEM -> newRows.add(new Row(depth, p.value(i, Panel.IntField.ID), p.string(i, Panel.IntField.LABEL)));
                case TREE_END -> depth--;
                default -> throw new IllegalArgumentException("Swing tree has no " + p.kind(i));
            }
        }
        if (!newRows.equals(rows)) {
            rows = newRows;
            rebuild();
            for (int i = control + 1; i < end; i++) {
                if (p.kind(i) == Panel.Kind.TREE && p.has(i, Panel.OPEN))
                    expandPath(paths.get(p.value(i, Panel.IntField.ID)));
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

    private void rebuild() {
        root.removeAllChildren();
        paths.clear();
        ArrayList<DefaultMutableTreeNode> parents = new ArrayList<>(List.of(root));
        for (Row row : rows) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(row);
            parents.get(row.depth).add(node);
            parents.subList(row.depth + 1, parents.size()).clear();
            parents.add(node);
            paths.put(row.item, new TreePath(node.getPath()));
        }
        model.reload();
    }

}
