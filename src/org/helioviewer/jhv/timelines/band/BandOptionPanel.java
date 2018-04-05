package org.helioviewer.jhv.timelines.band;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.TimelineSettings;
import org.helioviewer.jhv.timelines.propagation.PropagationModelLinear;
import org.json.JSONObject;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
class BandOptionPanel extends JPanel {

    BandOptionPanel(Band band) {
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;

        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        JButton pickColor = new JButton("Line color");
        pickColor.setMargin(new Insets(0, 0, 0, 0));
        pickColor.setToolTipText("Change the color of the current line");
        pickColor.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(ImageViewerGui.getMainFrame(), "Choose Line Color", band.getDataColor());
            if (newColor != null) {
                band.setDataColor(newColor);
            }
        });
        add(pickColor, c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.EAST;
        JButton availabilityButton = new JButton("Available data");
        availabilityButton.addActionListener(e -> JHVGlobals.openURL(TimelineSettings.availabilityURL + '#' + band.getBandType().getName()));
        add(availabilityButton, c);

        c.gridx = 2;
        c.anchor = GridBagConstraints.EAST;
        JideButton downloadButton = new JideButton(Buttons.download);
        downloadButton.setToolTipText("Download selected layer");
        downloadButton.addActionListener(e -> {
            String fileName = JHVDirectory.REMOTEFILES.getPath() + band.getBandType().getName() + "__" + TimeUtils.formatFilename(System.currentTimeMillis()) + ".json";
            JSONObject jo = band.toJson();

            new Thread(() -> {
                try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName), StandardCharsets.UTF_8)) {
                    jo.write(writer);
                    EventQueue.invokeLater(() -> JHVGlobals.displayNotification(fileName));
                } catch (Exception ex) {
                    Log.error("Failed to write JSON: " + ex);
                }
            }).start();
        });
        add(downloadButton, c);

        c.gridx = 3;
        c.anchor = GridBagConstraints.EAST;
        NumberFormat integerFormat = NumberFormat.getIntegerInstance(Locale.GERMAN);
        JFormattedTextField propagationField = new JFormattedTextField(integerFormat);
        propagationField.setValue(new Integer(0));
        propagationField.setColumns(10);
        propagationField.addPropertyChangeListener("value", e -> {
            int propagationSpeed = ((Number) propagationField.getValue()).intValue();
            if (propagationSpeed == 0) {
                band.removePropagationModel();
            } else {
                band.setPropagationModel(new PropagationModelLinear(propagationSpeed * 1000));
            }
        });
        add(propagationField, c);
        ComponentUtils.smallVariant(this);
    }

}
