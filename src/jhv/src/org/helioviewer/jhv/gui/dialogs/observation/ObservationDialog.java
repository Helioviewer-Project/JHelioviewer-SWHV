package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.gui.ImageViewerGui;

/**
 * The Observation Dialog provides the main interface to get data from the
 * Helioviewer or other servers.
 *
 * In order to select data a panel has to be added to the dialog. The Panel has
 * to be derived from ObservationDialogPanel.
 *
 * @author Stephan Pagel
 * */
@SuppressWarnings("serial")
public class ObservationDialog extends JDialog implements ActionListener {

    private final HashMap<String, ObservationDialogPanel> uiMap = new HashMap<String, ObservationDialogPanel>();

    private final JPanel contentPane = new JPanel();
    private final JPanel uiSelectionPane = new JPanel();
    private final JComboBox uiSelectionComboBox = new JComboBox();
    private final JPanel buttonPane = new JPanel();
    private final JButton btnImages = new JButton();
    private final JButton btnClose = new JButton("Cancel");
    private final JButton availabilityButton = new JButton("Available data");

    private ObservationDialogPanel selectedPane = null;

    private final ImageDataPanel imageObservationPanel;

    private static ObservationDialog instance;

    public static ObservationDialog getInstance() {
        if (instance == null) {
            instance = new ObservationDialog(ImageViewerGui.getMainFrame());
        }
        return instance;
    }

    public ImageDataPanel getObservationImagePane() {
        return imageObservationPanel;
    }

    private ObservationDialog(JFrame mainFrame) {
        super(mainFrame, false);

        // set dialog settings
        setContentPane(contentPane);
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        contentPane.setBorder(BorderFactory.createEmptyBorder(3, 9, 1, 9));
        contentPane.setFocusable(true);

        // set up components
        uiSelectionPane.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        uiSelectionPane.setBorder(BorderFactory.createEtchedBorder());
        uiSelectionPane.add(new JLabel("Data type"));
        uiSelectionPane.add(uiSelectionComboBox);
        uiSelectionPane.add(availabilityButton);

        availabilityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String str = (String) uiSelectionComboBox.getSelectedItem();
                if (str.equals("Image data")) {
                    String url = Settings.getSingletonInstance().getProperty("availability.images.url");
                    int sourceId = imageObservationPanel.getSourceId();
                    if (sourceId != -1)
                        url += "#IID" + sourceId;

                    JHVGlobals.openURL(url);
                } else if (str.equals("1D time series")) {
                    String url = Settings.getSingletonInstance().getProperty("availability.timelines.url");
                    JHVGlobals.openURL(url);
                }
            }
        });
        uiSelectionComboBox.addActionListener(this);

        buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 3, 3));
        buttonPane.add(btnClose);
        buttonPane.add(btnImages);

        btnImages.addActionListener(this);
        btnClose.addActionListener(this);

        getRootPane().registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeDialog();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        imageObservationPanel = new ImageDataPanel();
        addUserInterface("Image data", imageObservationPanel);
    }

    /**
     * Deactivates and activates the old and new displayed panel.
     * */
    private void setUIContainerPane(String name) {
        if (selectedPane != null) {
            selectedPane = null;
        }
        if (name != null) {
            selectedPane = uiMap.get(name);
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
        getRootPane().setDefaultButton(btnImages);
    }

    /**
     * Allows a component or plug-in to add its panel to the dialog in order to
     * select the corresponding data.
     * */
    public void addUserInterface(String name, ObservationDialogPanel userInterface) {
        uiMap.put(name, userInterface);
        uiSelectionComboBox.addItem(name);
    }

    /**
     * Allows a component or plug-in to remove its panel from the dialog.
     * */
    public void removeUserInterface(String name) {
        uiMap.remove(name);
        uiSelectionComboBox.removeItem(name);
    }

    /**
     * Returns the panel which is connected with the given name.
     * */
    public ObservationDialogPanel getUserInterface(String name) {
        return uiMap.get(name);
    }

    /**
     * Shows up the dialog and initializes the UI with the panel of the given
     * name.
     * */
    public void showDialog(Object layer, String name) {
        if (uiMap.isEmpty()) {
            Message.err("Error", "There are no data sources available!", false);
            return;
        }

        uiSelectionComboBox.setSelectedItem(name);
        if (layer == null) {
            setTitle("New Layer");
            btnImages.setText("Add");
        } else {
            setTitle("Change Layer");
            btnImages.setText("Change");
        }
        uiMap.get(name).setLayer(layer);

        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        getRootPane().setDefaultButton(btnImages);
        setVisible(true);
    }

    private void closeDialog() {
        setVisible(false);
        // dispose();
    }

    // Action Listener
    private void addPressed() {
        boolean result = true;
        if (selectedPane != null) {
            result = selectedPane.loadButtonPressed();
        }
        if (result) {
            closeDialog();
        }
    }

    public void setAvailabilityStatus(boolean status) {
        availabilityButton.setEnabled(status);
    }

    /**
     * Reacts on user input.
     * */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(uiSelectionComboBox)) {
            String str = (String) uiSelectionComboBox.getSelectedItem();
            setUIContainerPane(str);

            // that will do for now
            if (!str.equals("Image data"))
                setAvailabilityStatus(true);

        } else if (e.getSource().equals(btnImages)) {
            addPressed();
        } else if (e.getSource().equals(btnClose)) {
            closeDialog();
        }
    }

}
