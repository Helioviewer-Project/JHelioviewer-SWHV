package org.helioviewer.viewmodelplugin.filter;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.filters.AbstractFilterPanel;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

/**
 * This manager collects all filter control components and creates a panel where
 * all components will occur at the defined area.
 * <p>
 * There are three different areas: Top, Center, Bottom. Within the areas the
 * components will be occur below each other as they were added before.
 *
 * @author Stephan Pagel
 */
public class FilterTabPanelManager implements LayersListener {

    public enum Area {
        TOP, CENTER, BOTTOM
    }

    private final LinkedList<Component> topList = new LinkedList<Component>();
    private final LinkedList<Component> centerList = new LinkedList<Component>();
    private final LinkedList<Component> bottomList = new LinkedList<Component>();
    private final ArrayList<AbstractFilterPanel> abstractFilterPanels = new ArrayList<AbstractFilterPanel>();

    /**
     * Adds a component to the manager. This function calls
     * {@link #add(Component, Area)} with CENTER as default value for the area.
     *
     * @param comp
     *            Component to add.
     * @see #add(Component, Area)
     */
    public void add(Component comp) {
        add(comp, Area.CENTER);
        if (comp instanceof AbstractFilterPanel) {
            this.addAbstractFilterPanel((AbstractFilterPanel) comp);
        }
    }

    /**
     * Adds a component to the manager at the given area. By using
     * {@link #createPanel()} a panel will be created where the component will
     * be occur at the specified area.
     *
     * @param comp
     *            Component to add.
     * @param area
     *            Area where component should occur.
     */
    public void add(Component comp, Area area) {

        switch (area) {
        case TOP:
            topList.add(comp);
            break;
        case CENTER:
            centerList.add(comp);
            break;
        case BOTTOM:
            bottomList.add(comp);
            break;
        }
        if (comp instanceof AbstractFilterPanel) {
            this.addAbstractFilterPanel((AbstractFilterPanel) comp);
        }
    }

    public void addAbstractFilterPanel(AbstractFilterPanel abstractFilterPanel) {
        this.abstractFilterPanels.add(abstractFilterPanel);
    }

    /**
     * Setup a new CompactPanel based on all Components added to the center
     * list, that implement the FilterAlignmentDetails interface
     *
     * @see org.helioviewer.viewmodelplugin.filter# FilterAlignmentDetails
     * @return A JPanel containing all suitable components added to the center
     *         list
     */
    public JPanel createCompactPanel() {

        JPanel compactPanel = new JPanel() {

            private static final long serialVersionUID = 1L;

            /**
             * Override the setEnabled method in order to keep the containing
             * components' enabledState synced with the enabledState of this
             * component.
             */

            @Override
            public void setEnabled(boolean enabled) {
                for (Component c : this.getComponents()) {
                    c.setEnabled(enabled);
                }
            }
        };

        compactPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 5, 2, 5);
        c.weightx = 1;
        c.gridx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        for (Component comp : centerList) {
            if (comp instanceof FilterAlignmentDetails) {
                FilterAlignmentDetails details = (FilterAlignmentDetails) comp;

                if (details.getDetails() == FilterAlignmentDetails.POSITION_OPACITY) {
                    c.gridy = 0;
                    compactPanel.add(comp, c);
                } else if (details.getDetails() == FilterAlignmentDetails.POSITION_SHARPEN) {
                    c.gridy = 1;
                    compactPanel.add(comp, c);
                } else if (details.getDetails() == FilterAlignmentDetails.POSITION_GAMMA) {
                    c.gridy = 2;
                    compactPanel.add(comp, c);
                } else if (details.getDetails() == FilterAlignmentDetails.POSITION_CONTRAST) {
                    c.gridy = 3;
                    compactPanel.add(comp, c);
                } else if (details.getDetails() == FilterAlignmentDetails.POSITION_COLORTABLES) {
                    c.gridy = 4;
                    compactPanel.add(comp, c);
                } else if (details.getDetails() == FilterAlignmentDetails.POSITION_CHANNELMIXER) {
                    c.gridy = 5;
                    compactPanel.add(comp, c);
                }
            }
            c.insets = new Insets(0, 0, 0, 0);
            c.gridy = 6;

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setOpaque(false);
            buttonPanel.add(downloadLayerButton);
            buttonPanel.add(showMetaButton);

            compactPanel.add(buttonPanel, c);
        }
        LayersModel.getSingletonInstance().addLayersListener(this);

        return compactPanel;
    }

    @Override
    public void layerAdded(int idx) {
    }

    @Override
    public void layerRemoved(int oldIdx) {
    }

    @Override
    public void activeLayerChanged(View view) {
        if (view instanceof JHVJP2View) {
            //setActivejp2((JHVJP2View) view);
        }
    }

    private Action downloadLayerAction;
    private Action showMetaAction;
    private final JButton downloadLayerButton = new JButton();
    private final JButton showMetaButton = new JButton();

    public void setActivejp2(final JHVJP2View jp2view) {
        for (AbstractFilterPanel c : this.abstractFilterPanels) {
            c.setEnabled(true);
            c.setJP2View(jp2view);
        }
        downloadLayerAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
            {
                putValue(SHORT_DESCRIPTION, "Download the selected layer");
                putValue(SMALL_ICON, IconBank.getIcon(JHVIcon.DOWNLOAD));
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                LayersModel.getSingletonInstance().downloadLayer(jp2view);
            }
        };
        downloadLayerButton.setAction(downloadLayerAction);
        showMetaAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;
            {
                putValue(SHORT_DESCRIPTION, "Show metadata of the selected layer");
                putValue(SMALL_ICON, IconBank.getIcon(JHVIcon.INFO));
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                LayersModel.getSingletonInstance().showMetaInfo(jp2view);
            }
        };
        showMetaButton.setAction(showMetaAction);
        showMetaButton.revalidate();
    }

}
