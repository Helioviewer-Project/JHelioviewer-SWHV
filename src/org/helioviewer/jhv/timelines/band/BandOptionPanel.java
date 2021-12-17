package org.helioviewer.jhv.timelines.band;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.TimelineSettings;
import org.helioviewer.jhv.timelines.propagation.PropagationModelRadial;
import org.json.JSONObject;

import com.jidesoft.swing.JideButton;

import java.lang.invoke.MethodHandles;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
class BandOptionPanel extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    BandOptionPanel(Band band) {
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;

        c.gridx = 0;
        c.anchor = GridBagConstraints.LINE_START;
        JButton pickColor = new JButton("Line color");
        pickColor.setToolTipText("Change the color of the current line");
        pickColor.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(JHVFrame.getFrame(), "Choose Line Color", band.getDataColor());
            if (newColor != null) {
                band.setDataColor(newColor);
            }
        });
        add(pickColor, c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_END;
        JButton availabilityButton = new JButton("Available data");
        availabilityButton.addActionListener(e -> JHVGlobals.openURL(TimelineSettings.availabilityURL + '#' + band.getBandType().getName()));
        add(availabilityButton, c);

        c.gridx = 2;
        c.anchor = GridBagConstraints.LINE_END;
        JideButton downloadButton = new JideButton(Buttons.download);
        downloadButton.setToolTipText("Download selected layer");
        downloadButton.addActionListener(e -> {
            String fileName = JHVDirectory.REMOTEFILES.getPath() + band.getBandType().getName().replace(" ", "_") + "__" +
                    TimeUtils.formatFilename(System.currentTimeMillis()) + ".json";
            JSONObject jo = band.toJson();

            new Thread(() -> {
                try (BufferedWriter writer = Files.newBufferedWriter(Path.of(fileName), StandardCharsets.UTF_8)) {
                    jo.write(writer);
                    EventQueue.invokeLater(() -> JHVGlobals.displayNotification(fileName));
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Failed to write JSON", ex);
                }
            }).start();
        });
        add(downloadButton, c);

        c.gridx = 3;
        c.anchor = GridBagConstraints.LINE_END;
        JFormattedTextField propagationField = new JFormattedTextField(new TerminatedFormatterFactory("%.3f", "km/s", 0, 299792.458));
        propagationField.setValue(0.);
        propagationField.setColumns(10);
        propagationField.addPropertyChangeListener("value", e -> {
            double speed = (Double) propagationField.getValue();
            band.setPropagationModel(new PropagationModelRadial(speed));
        });
        add(propagationField, c);
    }

}
