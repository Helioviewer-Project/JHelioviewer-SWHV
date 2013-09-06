package org.helioviewer.jhv.internal_plugins.selectedLayer;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JTextField;

import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodelplugin.filter.FilterAlignmentDetails;

/**
 * This panel (which is part of the compact-panel) shows the name of the
 * currently selected layer. It implements FilterAlignmentDetails to provide
 * positioning information in the compact-panel
 * 
 * @author mnuhn
 */
public class SelectedLayerPanel extends JPanel implements FilterAlignmentDetails {

    private static final long serialVersionUID = 1L;
    private JTextField selectedLayerField = new JTextField("No Layer Selected");

    /**
     * Default Constructor
     */
    public SelectedLayerPanel() {
        this.setLayout(new BorderLayout());
        selectedLayerField.setEditable(false);
        selectedLayerField.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectedLayerField.setColumns(11);
        add(selectedLayerField, BorderLayout.CENTER);
    }

    /**
     * Reads the name from the given Layer and displays it in the Panel
     * 
     * @param view
     *            - View who's name is to be displayed in this panel
     */
    public SelectedLayerPanel(View view) {
        this();

        ImageInfoView imageInfoView = null;

        if (view != null) {
            imageInfoView = view.getAdapter(ImageInfoView.class);
        }

        if (imageInfoView != null) {

            selectedLayerField.setText(imageInfoView.getName());

        } else {

            selectedLayerField.setText("Unknown Layer");
            selectedLayerField.setEnabled(false);

        }
    }

    /**
     * {@inheritDoc}
     */
    public int getDetails() {
        return FilterAlignmentDetails.POSITION_LAYERNAME;
    }

    /**
     * Override the setEnabled method in order to keep the containing
     * components' enabledState synced with the enabledState of this component.
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        selectedLayerField.setEnabled(enabled);
    }

}
