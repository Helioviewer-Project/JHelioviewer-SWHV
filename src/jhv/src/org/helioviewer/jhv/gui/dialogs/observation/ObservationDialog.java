package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

/**
 * The Observation Dialog provides the main interface to get data from the
 * Helioviewer or other servers.
 *
 * In order to select data a panel has to be added to the dialog. The Panel has
 * to be derived from ObservationDialogPanel.
 *
 * @author Stephan Pagel
 * */
@SuppressWarnings({"unchecked","rawtypes","serial"})
public class ObservationDialog extends JDialog implements ActionListener, ShowableDialog {

    private final HashMap<String, ObservationDialogPanel> uiMap = new HashMap<String, ObservationDialogPanel>();

    private final JPanel contentPane = new JPanel();
    private final JPanel uiSelectionPane = new JPanel();
    private final JComboBox uiSelectionComboBox = new JComboBox();
    private final JPanel buttonPane = new JPanel();
    private final JButton btnImages = new JButton("Add layer");
    private final JButton btnClose = new JButton("Cancel");

    private ObservationDialogPanel selectedPane = null;

    private JButton availabilityButton;

    public ObservationDialog(JFrame mainFrame) {
        super(mainFrame, true);
        initVisualComponents();
    }

    /**
     * Sets up the visual sub components and the component itself.
     * */
    private void initVisualComponents() {
        // set dialog settings
        setTitle("Add layer");
        setContentPane(contentPane);

        // set basic layout
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        contentPane.setBorder(BorderFactory.createEmptyBorder(3, 9, 1, 9));
        contentPane.setFocusable(true);

        // set up components
        uiSelectionPane.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        uiSelectionPane.setBorder(BorderFactory.createEtchedBorder());
        uiSelectionPane.add(new JLabel("Data type"));
        uiSelectionPane.add(uiSelectionComboBox);
        availabilityButton = new JButton("Available data");
        uiSelectionPane.add(availabilityButton);
        availabilityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String str = (String) (uiSelectionComboBox.getSelectedItem());
                if (str == "Image data") {
                    String url = Settings.getSingletonInstance().getProperty("availability.images.url");
                    JHVGlobals.openURL(url);
                }
                if (str == "1D time series") {
                    String url = Settings.getSingletonInstance().getProperty("availability.timelines.url");
                    JHVGlobals.openURL(url);
                }
                if (str == "Radio data") {
                    String url = Settings.getSingletonInstance().getProperty("availability.radio.url");
                    JHVGlobals.openURL(url);
                }
            }
        });
        uiSelectionComboBox.addActionListener(this);

        buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 3, 3));
        buttonPane.add(btnClose);
        buttonPane.add(btnImages);

        btnImages.setIcon(IconBank.getIcon(JHVIcon.ADD));
        btnImages.setToolTipText("Request the selected data and display it");

        btnClose.setIcon(IconBank.getIcon(JHVIcon.REMOVE_LAYER));
        btnClose.setToolTipText("Close this dialog");

        final int btnWidth = Math.max(btnClose.getPreferredSize().getSize().width, btnImages.getPreferredSize().getSize().width);

        btnImages.setPreferredSize(new Dimension(btnWidth, 25));
        btnImages.addActionListener(this);

        btnClose.setPreferredSize(new Dimension(btnWidth, 25));
        btnClose.addActionListener(this);
    }

    /**
     * Deactivates and activates the old and new displayed panel.
     * */
    private void setUIContainerPane(final String name) {
        if (selectedPane != null) {
            selectedPane.deselected();
            selectedPane = null;
        }

        if (name != null) {
            selectedPane = uiMap.get(name);

            if (selectedPane != null) {
                selectedPane.selected();
            }
        }

        resetContentPane();
    }

    /**
     * Removes previous panel from the UI and sets the new selected panel to the
     * UI.
     * */
    private void resetContentPane() {
        contentPane.removeAll();

        contentPane.add(uiSelectionPane);
        if (selectedPane != null) {
            contentPane.add(selectedPane);
        }
        contentPane.add(buttonPane);
        contentPane.revalidate();
        pack();
    }

    /**
     * Allows a component or plug-in to add its panel to the dialog in order to
     * select the corresponding data.
     * */
    public void addUserInterface(final String name, final ObservationDialogPanel userInterface) {
        uiMap.put(name, userInterface);
        uiSelectionComboBox.addItem(name);
    }

    /**
     * Allows a component or plug-in to remove its panel from the dialog.
     * */
    public void removeUserInterface(final String name, final ObservationDialogPanel userInterface) {
        uiMap.remove(name);
        uiSelectionComboBox.removeItem(name);
    }

    /**
     * Returns the panel which is connected with the given name.
     * */
    public ObservationDialogPanel getUserInterface(final String name) {
        return uiMap.get(name);
    }

    /**
     * Enables the load data button.
     * */
    public void setLoadButtonEnabled(final boolean enable) {
        btnImages.setEnabled(enable);
    }

    /**
     * Shows up the dialog and initializes the UI with the panel of the given
     * name.
     * */
    public void showDialog(final String dataSourceName) {
        if (uiMap.size() <= 0) {
            Message.err("Error", "There are no data sources available!", false);
            return;
        }

        for (final ObservationDialogPanel pane : uiMap.values()) {
            pane.dialogOpened();
        }

        if (dataSourceName != null) {
            uiSelectionComboBox.setSelectedItem(dataSourceName);
        } else {
            uiSelectionComboBox.setSelectedIndex(0);
        }

        setLocationRelativeTo(ImageViewerGui.getMainFrame());

        pack();
        SwingUtilities.getRootPane(btnImages).setDefaultButton(btnImages);

        setVisible(true);
    }

    /**
     * Closes the dialog.
     * */
    private void closeDialog() {
        setVisible(false);
        dispose();
    }


    // Showable Dialog

    /**
     * {@inheritDoc}
     */
    @Override
    public void showDialog() {
        showDialog(null);
    }


    // Action Listener

    /**
     * Reacts on user input.
     * */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource().equals(uiSelectionComboBox)) {
            setUIContainerPane((String) uiSelectionComboBox.getSelectedItem());
        } else if (e.getSource().equals(btnImages)) {
            boolean result = true;

            if (selectedPane != null) {
                result = selectedPane.loadButtonPressed();
            }
            if (result) {
                closeDialog();
            }
        } else if (e.getSource().equals(btnClose)) {
            selectedPane.cancelButtonPressed();
            closeDialog();
        }
    }

    @Override
    public void init() {
    }

}
