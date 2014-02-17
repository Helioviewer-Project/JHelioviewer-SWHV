package org.helioviewer.jhv.plugins.hekplugin;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCache;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheListener;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheLoadingModel;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheModel;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheSelectionModel;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheTreeModel;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKPath;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKStupidDownloader;
import org.helioviewer.jhv.plugins.hekplugin.cache.gui.HEKCacheTreeView;
import org.helioviewer.jhv.plugins.hekplugin.cache.gui.HEKCacheTreeViewContainer;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodelplugin.overlay.OverlayPanel;

/**
 * Represents the UI components which manage the HEK event catalog.
 * 
 * @author Malte Nuhn
 * */
public class HEKPluginPanel extends OverlayPanel implements ActionListener, HEKCacheListener, LayersListener {

    private static final long serialVersionUID = 1L;

    // UI Components
    private JPanel buttonPanel = new JPanel(new BorderLayout());
    private JProgressBar progressBar = new JProgressBar();
    private HEKCacheTreeView tree = new HEKCacheTreeView(HEKCache.getSingletonInstance());
    private JScrollPane treeView = new JScrollPane(tree);
    private JButton cancelButton = new JButton(new ImageIcon(HEKPlugin.getResourceUrl("/images/hekCancel.png")));
    private JButton reloadButton = new JButton(new ImageIcon(HEKPlugin.getResourceUrl("/images/hekReload.png")));
    private HEKCacheTreeViewContainer container = new HEKCacheTreeViewContainer();

    private HEKCacheModel cacheModel;
    private HEKCache cache;
    private HEKCacheSelectionModel selectionModel;
    @SuppressWarnings("unused")
    private HEKCacheTreeModel treeModel;
    private HEKCacheLoadingModel loadingModel;

    /**
     * Default constructor
     * 
     * @param hekCache
     * */
    public HEKPluginPanel(HEKCache hekCache) {

        this.cache = hekCache;
        this.cacheModel = hekCache.getModel();
        this.selectionModel = hekCache.getSelectionModel();
        this.treeModel = hekCache.getTreeModel();
        this.loadingModel = hekCache.getLoadingModel();

        // set up visual components
        initVisualComponents();

        // register as layers listener
        LayersModel.getSingletonInstance().addLayersListener(this);
        HEKCache.getSingletonInstance().getModel().addCacheListener(this);
    }

    /**
     * Force a redraw of the main window
     */
    private void fireRedraw() {
        LayersModel.getSingletonInstance().viewChanged(null, new ChangeEvent(new SubImageDataChangedReason(null)));
    }

    /**
     * Update the plugin's currently displayed interval.
     * 
     * The plugin is currently stafeFUL, so keep in mind that just calling this
     * method without triggering any other update method might not be a good
     * decision.
     * 
     * @param newInterval
     *            - the interval that should be displayed
     * 
     * @see org.helioviewer.jhv.plugins.overlay.hek.cache.HEKCacheModel#setCurInterval
     * 
     */
    public void setCurInterval(Interval<Date> newPosition) {
        if (!HEKCache.getSingletonInstance().getModel().getCurInterval().equals(newPosition)) {
            HEKCache.getSingletonInstance().getController().setCurInterval(newPosition);
        }
    }

    /**
     * Request the plugin to download and display the Events available in the
     * catalogue
     * 
     * The interval to be requested depends on the current state of the plug-in.
     * 
     * @see org.helioviewer.jhv.plugins.overlay.hek.cache.HEKCacheController#requestStructure
     * 
     */
    public void getStructure() {
        Interval<Date> selected = HEKCache.getSingletonInstance().getModel().getCurInterval();
        HEKCache.getSingletonInstance().getController().requestStructure(selected);
    }

    /**
	* Sets up the visual sub components and the visual part of the component
	* itself.
	* */
    private void initVisualComponents() {

        // set general appearance
        setLayout(new GridBagLayout());

        this.setPreferredSize(new Dimension(150, 200));

        progressBar.setIndeterminate(true);

        cancelButton.addActionListener(this);
        reloadButton.addActionListener(this);

        tree.setModel(HEKCache.getSingletonInstance().getTreeModel());
        tree.setController(HEKCache.getSingletonInstance().getController());
        tree.setRootVisible(false);

        container.setMain(treeView);
        container.update();

        HEKCache.getSingletonInstance().getController().fireEventsChanged((HEKCache.getSingletonInstance().getController().getRootPath()));

        setEnabled(true);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;

        this.add(container, c);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.weightx = 1.0;
        c2.weighty = 0.0;
        c2.gridx = 0;
        c2.gridy = 1;

        this.add(progressBar, c2);
        this.setLoading(false);

        buttonPanel.add(reloadButton, BorderLayout.EAST);
        buttonPanel.add(cancelButton, BorderLayout.EAST);

        GridBagConstraints c3 = new GridBagConstraints();
        c3.fill = GridBagConstraints.NONE;
        c3.anchor = GridBagConstraints.EAST;
        c3.weightx = 0.0;
        c3.weighty = 0.0;
        c3.gridx = 1;
        c3.gridy = 1;

        this.add(reloadButton, c3);
        this.add(cancelButton, c3);
    }

    /**
     * Updates components.
     * */
    public void updateComponents() {
    }

    public void actionPerformed(ActionEvent act) {

        if (act.getSource().equals(cancelButton)) {
            HEKStupidDownloader.getSingletonInstance().cancelDownloads();
        }

        if (act.getSource().equals(reloadButton)) {
            getStructure();
        }

        if (act.getActionCommand().equals("request")) {
            // move into controller
            // TODO move into loading watcher
            HashMap<HEKPath, Vector<Interval<Date>>> selected = selectionModel.getSelection(cacheModel.getCurInterval());
            HashMap<HEKPath, Vector<Interval<Date>>> needed = cache.needed(selected);
            HashMap<HEKPath, Vector<Interval<Date>>> nonQueued = HEKCache.getSingletonInstance().getLoadingModel().filterState(needed, HEKCacheLoadingModel.PATH_NOTHING);
            HEKCache.getSingletonInstance().getController().requestEvents(nonQueued);
        }

    }

    public void setEnabled(boolean b) {
        // super.setEnabled(b);
        if (b == false) {
            HEKCache.getSingletonInstance().getExpansionModel().expandToLevel(0, true, true);
            HEKPath rootPath = HEKCache.getSingletonInstance().getController().getRootPath();
            HEKCache.getSingletonInstance().getController().fireEventsChanged(rootPath);
        }
        tree.setEnabled(b);
    }

    public void activeLayerChanged(int idx) {
         View view = LayersModel.getSingletonInstance().getActiveView();
    }

    public void layerAdded(int idx) {
        Thread threadUpdate = new Thread(new Runnable() {
            public void run() {
                Date start = LayersModel.getSingletonInstance().getFirstDate();
                Date end = LayersModel.getSingletonInstance().getLastDate();
                if (start != null && end != null) {
                    Interval<Date> range = new Interval<Date>(start, end);
                    HEKCache.getSingletonInstance().getController().setCurInterval(range);
                    getStructure();
                }
            }
        });
        threadUpdate.start();
    }

    public void layerChanged(int idx) {
    }

    public void layerRemoved(View oldView, int oldIdx) {
    }

    public void subImageDataChanged() {
    }

    public void timestampChanged(int idx) {
        // Not used anymore
    }

    public void viewportGeometryChanged() {
    }

    public void cacheStateChanged() {
        // anything loading?
        boolean loading = loadingModel.getState(cacheModel.getRoot(), true) != HEKCacheLoadingModel.PATH_NOTHING;
        this.setLoading(loading);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisible(loading);
        cancelButton.setVisible(loading);
        reloadButton.setVisible(!loading);
    }

    public void eventsChanged(HEKPath path) {
        fireRedraw();
    }

    public void structureChanged(HEKPath path) {
        fireRedraw();
    }

    /**
     * {@inheritDoc}
     */
    public void regionChanged() {
    }

    /**
     * {@inheritDoc}
     */
    public void layerDownloaded(int idx) {
    }
}
