package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.io.DownloadViewTask;
import org.helioviewer.jhv.layers.ImageLayerOptions;
import org.helioviewer.jhv.opengl.GLImage;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
public class RunningDifferencePanel implements FilterDetails {

    private final JPanel modePanel = new JPanel(new GridLayout(1, 3));
    private final JPanel buttonPanel = new JPanel();

    public RunningDifferencePanel(ImageLayerOptions parent) {
        ButtonGroup modeGroup = new ButtonGroup();
        for (GLImage.DifferenceMode mode : GLImage.DifferenceMode.values()) {
            JRadioButton item = new JRadioButton(mode.toString());
            if (mode == parent.getGLImage().getDifferenceMode())
                item.setSelected(true);
            item.addActionListener(e -> {
                parent.getGLImage().setDifferenceMode(mode);
                Displayer.display();
            });
            modeGroup.add(item);
            modePanel.add(item);
        }

        JideButton metaButton = new JideButton(Buttons.info);
        metaButton.setToolTipText("Show metadata of selected layer");
        metaButton.addActionListener(e -> {
            MetaDataDialog dialog = new MetaDataDialog(parent.getView());
            dialog.showDialog();
        });

        JideButton downloadButton = new JideButton(Buttons.download);
        downloadButton.setToolTipText("Download selected layer");
        downloadButton.addActionListener(e -> {
            DownloadViewTask downloadTask = new DownloadViewTask(parent.getView().getImageLayer(), parent.getView());
            JHVGlobals.getExecutorService().execute(downloadTask);
        });

        buttonPanel.add(metaButton);
        buttonPanel.add(downloadButton);
    }

    @Override
    public Component getTitle() {
        return new JLabel("Difference", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return modePanel;
    }

    @Override
    public Component getLabel() {
        return buttonPanel;
    }

}
