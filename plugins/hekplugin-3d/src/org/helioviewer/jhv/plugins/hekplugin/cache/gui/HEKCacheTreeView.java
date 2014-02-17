package org.helioviewer.jhv.plugins.hekplugin.cache.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.components.tristateCheckbox.TristateCheckBox;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCache;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheController;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheModel;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheExpansionModel;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheLoadingModel;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheTreeModel;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheSelectionModel;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKPath;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKConstants;

/**
 * A Tree View visualizing the data provided by a HEKCacheModel
 * 
 * The Swing Library is not thread safe, which means that only one thread may
 * create or modify swing objects. This is the AWT Event thread. Whenever Swing
 * calls a listener method, it is executed on the AWT Event thread, so you may
 * modify Swing objects here.
 * 
 * 
 * We use the "separable model architecture." The designers of Swing did this
 * because it was "difficult to write a generic controller that didn't know
 * specifics about the [particular] view."
 * 
 * @author Malte Nuhn
 */
public class HEKCacheTreeView extends JTree implements TreeModelListener {

    private static final long serialVersionUID = 1L;

    private HEKCacheSelectionModel selectionModel;
    private HEKCacheExpansionModel expansionModel;
    @SuppressWarnings("unused")
    private HEKCacheTreeModel treeModel;
    private HEKCacheLoadingModel loadingModel;
    private HEKCacheModel cacheModel;
    private HEKCacheController cacheController;

    // private Vector<HEKTreeListener> treeListeners = new
    // Vector<HEKTreeListener>();

    JCheckBoxTreeRenderer checkBoxCellRenderer;

    public HEKCacheTreeView(HEKCache cache) {
        super();
        init();
        this.cacheModel = cache.getModel();
        this.cacheController = cache.getController();
        this.loadingModel = cache.getLoadingModel();
        this.expansionModel = cache.getExpansionModel();
        this.selectionModel = cache.getSelectionModel();

        this.setTreeModel(cache.getTreeModel());
        this.setOpaque(false);

    }

    /**
     * @see org.helioviewer.jhv.internal_plugins.overlay.solarevents.SolarEventRenderer#SolarEventRenderer
     */
    private void init() {
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        checkBoxCellRenderer = new JCheckBoxTreeRenderer();
        setCellRenderer(checkBoxCellRenderer);
        addMouseListener(new JCheckBoxTreeMouseListener());
        this.setBackground(Color.WHITE);

        // let the TreeCellRenderer choose the rowHeight
        this.setRowHeight(0);
    }

    /**
     * Update the model to be displayed
     * 
     * @param t
     *            - model to be displayed
     */
    public void setTreeModel(HEKCacheTreeModel t) {
        // the order of these instructions is important!
        this.treeModel = t;
        super.setModel(t);
        // important, since we care about the expansion state by our self
        super.treeModelListener = this;
    }

    /**
     * Update the controller
     * 
     * @param t
     *            - model to be displayed
     */
    public void setController(HEKCacheController t) {
        this.cacheController = t;
    }

    /**
     * Returns the selection state of the given path
     * 
     * @param path
     * @return
     */
    public int getState(TreePath path) {

        if (path == null) {
            return 0;
        }

        Object[] p = path.getPath();

        if (p != null & p.length > 0 & p[p.length - 1] instanceof HEKPath) {
            HEKPath e = (HEKPath) p[p.length - 1];
            return this.selectionModel.getState(e);
        }
        return 0;
    }

    /**
     * TreeCellRenderer needed to draw the checkboxes into the tree
     * 
     * @author Malte Nuhn
     */
    private class JCheckBoxTreeRenderer extends DefaultTreeCellRenderer {

        private static final long serialVersionUID = 8761123761371402102L;

        /**
         * Initialize the renderer
         */
        public JCheckBoxTreeRenderer() {
            super();
        }

        /**
         * This is the function that actually renders the individual objects
         */
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            // System.out.println("Rendering" + value);

            JPanel panel = new JPanel();

            // add the checkbox to the left
            panel.setLayout(new java.awt.BorderLayout(5, 0));
            panel.setOpaque(false);

            int rowState = getState(getPathForRow(row));
            TristateCheckBox checkBox = new TristateCheckBox("", rowState);

            // checkbox
            checkBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
            checkBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
            // checkboxes are in the same enabled state as this treeView

            checkBox.setEnabled(tree.isEnabled());
            // retrieve the state of this row

            // we need this panel to have a common size for all rows
            JPanel compoPanel = new JPanel();

            compoPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

            compoPanel.setOpaque(false);
            compoPanel.add(checkBox);

            panel.add(compoPanel, BorderLayout.WEST);

            Component comp;

            // if we are trying to render a HEKPath
            if (value instanceof HEKPath) {

                // ... just get a nice string, and use the default renderer to
                // render that string
                HEKPath path = (HEKPath) value;
                String eventType = path.getLastPart();
                String toRender = HEKConstants.getSingletonInstance().acronymToString(eventType);

                int downloadableEvents = HEKCache.getSingletonInstance().getModel().getDownloadableChildrenEventsRecursive(path, cacheModel.getCurInterval(), true);

                Vector<HEKPath> downloadedEvents = HEKCache.getSingletonInstance().getModel().getChildrenEventsRecursive(path, cacheModel.getCurInterval(), true);
                int numDownloadedEvents = downloadedEvents.size();

                HEKCache.getSingletonInstance().getSelectionModel().filterSelectedPaths(downloadedEvents);
                int numSelectedEvents = downloadedEvents.size();
                // TODO Malte Nuhn - Implement the total number of downloadable
                // events
                toRender = toRender + " (" + numSelectedEvents + "/" + (numDownloadedEvents + downloadableEvents) + ")";

                comp = super.getTreeCellRendererComponent(tree, toRender, selected, expanded, leaf, row, hasFocus);

                if (path.getType() != null || path.getFRM() != null) {

                    String acronym = HEKConstants.ACRONYM_FALLBACK;

                    if (path.getFRM() != null) {
                        acronym = path.getParent().getLastPart();
                    } else if (path.getType() != null) {
                        acronym = path.getLastPart();
                    }

                    BufferedImage iconImage = HEKConstants.getSingletonInstance().acronymToBufferedImage(acronym, false);

                    // more robustness
                    if (iconImage == null) {
                        iconImage = HEKConstants.getSingletonInstance().acronymToBufferedImage(HEKConstants.ACRONYM_FALLBACK, false);
                    }

                    int loading = loadingModel.getState(path, true);

                    if (cacheModel != null && loading != HEKCacheLoadingModel.PATH_NOTHING) {
                        BufferedImage loadingImage;
                        String overlayMode = "No Overlay";

                        if ((loading & HEKCacheLoadingModel.PATH_LOADING) != 0) {
                            loadingImage = HEKConstants.getSingletonInstance().getOverlayBufferedImage("LOADING", false);
                            overlayMode = "Loading";
                        } else {
                            loadingImage = HEKConstants.getSingletonInstance().getOverlayBufferedImage("QUEUED", false);
                            overlayMode = "Queued";
                        }

                        BufferedImage stack[] = { iconImage, loadingImage };
                        BufferedImage imageStack = IconBank.stackImages(stack, 1.0, 1.0);

                        if (imageStack != null) {
                            iconImage = imageStack;
                        } else {
                            Log.warn("Could not generate Image Stack (" + acronym + " + " + overlayMode + ")");
                        }
                    }
                    if (iconImage != null) {
                        this.setIcon(new ImageIcon(iconImage));
                    } else {
                        Log.warn("Could not load event icon (" + acronym + ")");
                    }
                } else {
                    // must be the root node
                    BufferedImage hekLogo = HEKConstants.getSingletonInstance().getOverlayBufferedImage("HEK", false);
                    int loading = loadingModel.getState(cacheModel.getRoot(), true);

                    if (loading != HEKCacheLoadingModel.PATH_NOTHING) {
                        BufferedImage loadingImage;
                        String overlayMode = "No Overlay";

                        if ((loading & HEKCacheLoadingModel.PATH_LOADING) != 0) {
                            loadingImage = HEKConstants.getSingletonInstance().getOverlayBufferedImage("LOADING", false);
                            overlayMode = "Loading";
                        } else {
                            loadingImage = HEKConstants.getSingletonInstance().getOverlayBufferedImage("QUEUED", false);
                            overlayMode = "Queued";
                        }

                        BufferedImage stack[] = { hekLogo, loadingImage };
                        BufferedImage imageStack = IconBank.stackImages(stack, 1.0, 1.0);

                        if (imageStack != null) {
                            hekLogo = imageStack;
                        } else {
                            Log.warn("Could not generate Image Stack (heklogo + " + overlayMode + ")");
                        }
                    }
                    if (hekLogo != null) {
                        this.setIcon(new ImageIcon(hekLogo));
                    } else {
                        Log.warn("Could not load hek icon");
                    }
                }

            } else {

                // ... if we do not know what it is, just use the default
                // renderer
                comp = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            }

            // and add it to the panel (on the right)
            panel.add(comp, BorderLayout.CENTER);

            return (panel);
        }

    }

    /**
     * Helper class needed to register clicks onto the checkbox.
     * 
     * @author Malte Nuhn
     */
    private class JCheckBoxTreeMouseListener extends MouseAdapter {

        public HEKPath getHEKPath(TreePath path) {
            if (path == null) {
                return null;
            }

            Object[] p = path.getPath();

            if (p != null & p.length > 0 & p[p.length - 1] instanceof HEKPath) {
                HEKPath e = (HEKPath) p[p.length - 1];
                return e;
            }

            return null;

        }

        /**
         * Fires if the mouse gets pressed somewhere on the tree
         */
        public void mousePressed(MouseEvent e) {

            if (e.getSource() instanceof HEKCacheTreeView) {
                HEKCacheTreeView tree = (HEKCacheTreeView) e.getSource();

                if (!tree.isEnabled()) {
                    return;
                }

                int selRow = getRowForLocation(e.getX(), e.getY());

                if (selRow != -1) {

                    HEKPath selPath = getHEKPath(getPathForRow(selRow));

                    // if now row is selected, do it now
                    if (!isRowSelected(selRow)) {
                        setSelectionRow(selRow);
                    }

                    // invert selection state
                    selectionModel.invertState(selPath);
                    cacheController.fireEventsChanged(selPath);

                    // move into controller
                    // TODO move into loading watcher
                    HashMap<HEKPath, Vector<Interval<Date>>> selected = selectionModel.getSelection(cacheModel.getCurInterval());
                    HashMap<HEKPath, Vector<Interval<Date>>> needed = HEKCache.getSingletonInstance().needed(selected);
                    HashMap<HEKPath, Vector<Interval<Date>>> nonQueued = HEKCache.getSingletonInstance().getLoadingModel().filterState(needed, HEKCacheLoadingModel.PATH_NOTHING);
                    cacheController.requestEvents(nonQueued);

                }
            }
        }
    }

    public void treeNodesChanged(TreeModelEvent event) {
        HEKPath affected = (HEKPath) event.getTreePath().getLastPathComponent();
        syncExpandedState(affected);
    }

    public void treeNodesInserted(TreeModelEvent event) {
        HEKPath affected = (HEKPath) event.getTreePath().getLastPathComponent();
        syncExpandedState(affected);
    }

    public void treeNodesRemoved(TreeModelEvent event) {
        HEKPath affected = (HEKPath) event.getTreePath().getLastPathComponent();
        syncExpandedState(affected);
    }

    public void treeStructureChanged(TreeModelEvent event) {
        HEKPath affected = (HEKPath) event.getTreePath().getLastPathComponent();
        syncExpandedState(affected);
    }

    private void syncExpandedState(HEKPath root) {
        // the root is currently ignored, and the whole state is refreshed
        Vector<HEKPath> expanded = expansionModel.getExpandedPaths(true);
        Vector<HEKPath> closed = expansionModel.getExpandedPaths(false);

        // find the maximum depth available

        int maxDepth = 0;

        for (HEKPath p : expanded) {
            maxDepth = p.getDepth() > maxDepth ? p.getDepth() : maxDepth;
        }

        for (HEKPath p : closed) {
            maxDepth = p.getDepth() > maxDepth ? p.getDepth() : maxDepth;
        }

        // go from deepest down to the root node - since collapse will enforce
        // the node to be visible
        // (and thus expand other nodes for this)

        for (int depth = maxDepth; depth > 0; depth--) {
            for (HEKPath p : expanded) {
                if (p.getDepth() == depth) {
                    this.expandPath(new TreePath(p.getTreePath()));
                }
            }

            for (HEKPath p : closed) {
                if (p.getDepth() == depth) {
                    this.collapsePath(new TreePath(p.getTreePath()));
                }
            }
        }

    }

    protected void setExpandedState(TreePath path, boolean state) {
        HEKPath p = (HEKPath) (path.getLastPathComponent());
        super.setExpandedState(path, state);
        // TODO: REALLY OVERWRITE == TRUE?
        expansionModel.setExpandedState(p, state, true);

    }

    @Override
    // full width selection
    protected void paintComponent(Graphics g) {
        int[] rows = getSelectionRows();
        if (rows != null && rows.length > 0) {
            Rectangle b = getRowBounds(rows[0]);
            g.setColor(UIManager.getColor("Tree.selectionBackground"));
            g.fillRect(0, b.y, getWidth(), b.height);
        }// if
        super.paintComponent(g);
    }

}
