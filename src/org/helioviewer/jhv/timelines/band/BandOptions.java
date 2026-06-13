package org.helioviewer.jhv.timelines.band;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.gui.CompletionNotifications;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.gui.component.TerminatedFormatterFactory;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONObject;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
class BandOptions extends JPanel {

    BandOptions(Band band) {
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
            Color newColor = JColorChooser.showDialog(MainFrame.get(), "Choose Line Color", band.getDataColor());
            if (newColor != null) {
                band.setDataColor(newColor);
            }
        });
        add(pickColor, c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        JFormattedTextField propagationField = new JFormattedTextField(new TerminatedFormatterFactory("%.3f", " days", 0, 28));
        propagationField.setValue(0.);
        propagationField.setColumns(10);
        propagationField.addPropertyChangeListener("value", e -> {
            double value = (Double) propagationField.getValue();
            band.setPropagationModel(new PropagationModel.Delay(value));
        });
        add(propagationField, c);

        c.gridx = 2;
        c.anchor = GridBagConstraints.LINE_END;
        JideButton downloadButton = getDownloadButton(band);
        add(downloadButton, c);
    }

    private static JideButton getDownloadButton(Band band) {
        JideButton downloadButton = new JideButton(Buttons.download);
        downloadButton.setToolTipText("Download selected layer");
        downloadButton.addActionListener(e -> {
            Path path = Path.of(Directories.DOWNLOADS.getPath(),
                    band.getBandType().getName().replace(' ', '_') + "__" + TimeUtils.formatFilename(System.currentTimeMillis()) + ".json");
            JSONObject jo = band.toJson();

            AppThread.create(() -> {
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    jo.write(writer);
                    CompletionNotifications.fileReady(path.toString());
                } catch (Exception ex) {
                    Log.error("Failed to write JSON", ex);
                }
            }, "JHV-ExportBand").start();
        });
        return downloadButton;
    }

}
