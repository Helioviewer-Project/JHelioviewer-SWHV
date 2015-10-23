package org.helioviewer.jhv.gui.filters;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.io.FileDownloader;
import org.helioviewer.jhv.opengl.GLImage;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2View;

/**
 * Panel to control running differences
 *
 * @author Helge Dietert
 *
 */
@SuppressWarnings("serial")
public class RunningDifferencePanel extends AbstractFilterPanel implements ChangeListener {
    /**
     * Controlled filter by this panel
     */
    private final JSpinner truncateSpinner;
    private final JLabel truncateLabel;
    private final JPanel diffPanel = new JPanel();

    private JCheckBox diffRot;
    private final static String[] combolist = { "No difference images", "Running difference", "Base difference" };
    private final JButton downloadLayerButton;
    private final JButton showMetaButton;
    private final JPanel topPanel;
    private final JPanel radPanel;
    private final JComboBox comboBox;

    private View view;

    public RunningDifferencePanel() {
        downloadLayerButton = new JButton(new AbstractAction() {
            {
                putValue(SHORT_DESCRIPTION, "Download selected layer");
                putValue(SMALL_ICON, IconBank.getIcon(JHVIcon.DOWNLOAD));
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                downloadLayer((JP2View) view);
            }
        });
        downloadLayerButton.setBorder(null);
        downloadLayerButton.setText(null);
        downloadLayerButton.setBorderPainted(false);
        downloadLayerButton.setFocusPainted(false);
        downloadLayerButton.setContentAreaFilled(false);

        showMetaButton = new JButton(new AbstractAction() {
            {
                putValue(SHORT_DESCRIPTION, "Show metadata of selected layer");
                putValue(SMALL_ICON, IconBank.getIcon(JHVIcon.INFO));
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                MetaDataDialog dialog = new MetaDataDialog(view);
                dialog.showDialog();
            }
        });
        showMetaButton.setBorder(null);
        showMetaButton.setText(null);
        showMetaButton.setBorderPainted(false);
        showMetaButton.setFocusPainted(false);
        showMetaButton.setContentAreaFilled(false);

        diffPanel.setLayout(new BoxLayout(diffPanel, BoxLayout.PAGE_AXIS));

        truncateLabel = new JLabel("Contrast boost", JLabel.RIGHT);

        truncateSpinner = new JSpinner();
        truncateSpinner.setModel(new SpinnerNumberModel(Float.valueOf(0.8f), Float.valueOf(0), Float.valueOf(0.99f), Float.valueOf(0.01f)));
        truncateSpinner.addChangeListener(this);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(truncateSpinner, "0%");
        truncateSpinner.setEditor(editor);
        editor.getTextField().setColumns(3);
        editor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        WheelSupport.installMouseWheelSupport(truncateSpinner);

        comboBox = new JComboBox(combolist);
        topPanel = new JPanel(new GridBagLayout());
        radPanel = new JPanel(new FlowLayout());
        addRadioButtons();
    }

    private void setDifferenceMode(boolean showExtraPanel) {
        if (showExtraPanel) {
            final GridBagConstraints c = new GridBagConstraints();
            c.weightx = 1;
            c.weighty = 1;
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.HORIZONTAL;

            c.gridx = 0;
            c.gridwidth = 3;
            topPanel.add(radPanel, c);
            image.setRunDiffNoRot(!diffRot.isSelected());
        } else
            topPanel.remove(radPanel);
    }

    private void setDifferenceModetoJP2View(boolean showExtraPanel, boolean differenceMode, boolean baseDifferenceMode) {
        setDifferenceMode(showExtraPanel);
        image.setDifferenceMode(differenceMode);
        image.setBaseDifferenceMode(baseDifferenceMode);
    }

    private void setDifferenceModetoChangeCombobox(boolean differenceMode, boolean baseDifferenceMode) {
        if (!differenceMode) {
            comboBox.setSelectedItem(combolist[0]);
        } else if (!baseDifferenceMode) {
            comboBox.setSelectedItem(combolist[1]);
        } else {
            comboBox.setSelectedItem(combolist[2]);
        }
    }

    private void addRadioButtons() {
        comboBox.setSelectedItem(0);
        diffRot = new JCheckBox("Rotation correction");
        diffRot.setSelected(true);
        final GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;

        c.gridx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;
        c.gridx = 0;
        topPanel.add(comboBox, c);
        c.gridx = 1;
        c.weightx = 0;
        topPanel.add(downloadLayerButton, c);
        c.gridx = 2;
        topPanel.add(showMetaButton, c);
        diffPanel.add(topPanel);
        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 3;

        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (comboBox.getSelectedItem().equals(combolist[0])) {
                    setDifferenceModetoJP2View(false, false, false);
                } else if (comboBox.getSelectedItem().equals(combolist[1])) {
                    setDifferenceModetoJP2View(true, true, false);
                } else if (comboBox.getSelectedItem().equals(combolist[2])) {
                    setDifferenceModetoJP2View(true, true, true);
                }
                Displayer.display();
                topPanel.revalidate();
                topPanel.repaint();
            }
        });
        diffRot.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (comboBox.getSelectedItem().equals(combolist[2])) {
                    image.setBaseDifferenceNoRot(!diffRot.isSelected());
                } else {
                    image.setRunDiffNoRot(!diffRot.isSelected());
                }
                Displayer.display();
            }
        });

        radPanel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 1;
        gc.gridx = 0;
        gc.gridwidth = 1;
        gc.anchor = GridBagConstraints.CENTER;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;
        gc.gridx = 0;
        radPanel.add(diffRot, gc);
        gc.gridx = 1;
        radPanel.add(truncateLabel, gc);
        gc.gridx = 2;
        radPanel.add(truncateSpinner, gc);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        float value = ((SpinnerNumberModel) truncateSpinner.getModel()).getNumber().floatValue();
        image.setTruncation(1 - value);
        Displayer.display();
    }

    @Override
    public void setGLImage(GLImage image) {
        super.setGLImage(image);
        boolean differenceMode = image.getDifferenceMode();
        if (differenceMode) {
            boolean baseDifferenceMode = image.getBaseDifferenceMode();
            setDifferenceModetoChangeCombobox(differenceMode, baseDifferenceMode);
        } else {
            setDifferenceModetoChangeCombobox(false, false);
        }

        truncateSpinner.setValue(1.f - image.getTruncation());
    }

    public void setView(View _view) {
        view = _view;
        if (view instanceof JP2View) {
            downloadLayerButton.setVisible(true); // enabled no good
        } else {
            downloadLayerButton.setVisible(false);
        }
    }

    public Component getPanel() {
        return diffPanel;
    }

    /**
     * Trigger downloading the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     */
    private static void downloadLayer(JP2View view) {
        Thread downloadThread = new Thread(new Runnable() {
            private JP2View theView;

            @Override
            public void run() {
                downloadFromJPIP(theView);
            }

            public Runnable init(JP2View theView) {
                this.theView = theView;
                return this;
            }
        }.init(view), "DownloadFromJPIPThread");
        downloadThread.start();
    }

    /**
     * Downloads the complete image from the JPIP server.
     *
     * Changes the source of the View afterwards, since a local file is always
     * faster.
     */
    private static void downloadFromJPIP(JP2View v) {
        FileDownloader fileDownloader = new FileDownloader();
        URI downloadUri = v.getDownloadURI();
        URI uri = v.getUri();

        // the http server to download the file from is unknown
        if (downloadUri.equals(uri) && !downloadUri.toString().contains("delphi.nascom.nasa.gov")) {
            String inputValue = JOptionPane.showInputDialog("To download this file, please specify a concurrent HTTP server address to the JPIP server: ", uri);
            if (inputValue != null) {
                try {
                    downloadUri = new URI(inputValue);
                } catch (URISyntaxException e) {
                }
            }
        }

        File downloadDestination = fileDownloader.getDefaultDownloadLocation(uri);
        try {
            if (!fileDownloader.get(downloadUri, downloadDestination, "Downloading " + v.getName())) {
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

}
