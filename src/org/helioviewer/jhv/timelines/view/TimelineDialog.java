package org.helioviewer.jhv.timelines.view;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.jhv.timelines.TimelineSettings;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class TimelineDialog extends StandardDialog implements ShowableDialog {

    private TimelineContentPanel observationPanel = new EmptyTimelineContentPanel();

    public TimelineDialog() {
        super(ImageViewerGui.getMainFrame(), "New Layer", true);
        setResizable(false);
    }

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        setDefaultCancelAction(close);

        JButton cancelBtn = new JButton(close);
        cancelBtn.setText("Cancel");

        AbstractAction load = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                observationPanel.loadButtonPressed();
                setVisible(false);
            }
        };
        setDefaultAction(load);

        JButton okBtn = new JButton(load);
        okBtn.setText("Add");
        setInitFocusedComponent(okBtn);

        JButton availabilityBtn = new JButton("Available data");
        availabilityBtn.addActionListener(e -> JHVGlobals.openURL(TimelineSettings.availabilityURL));

        ButtonPanel panel = new ButtonPanel();
        panel.add(okBtn, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(cancelBtn, ButtonPanel.CANCEL_BUTTON);
        panel.add(availabilityBtn, ButtonPanel.OTHER_BUTTON);

        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        JComponent panel = observationPanel.getTimelineContentPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        return panel;
    }

    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    @Override
    public void showDialog() {
        observationPanel.updateGroupValues();
        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

    public TimelineContentPanel getObservationPanel() {
        return observationPanel;
    }

    public void setObservationPanel(TimelineContentPanel timelineContentPanel) {
        observationPanel = timelineContentPanel;
    }

    private static class EmptyTimelineContentPanel implements TimelineContentPanel {

        @Override
        public void loadButtonPressed() {
        }

        @Override
        public JComponent getTimelineContentPanel() {
            return new JPanel();
        }

        @Override
        public void setupDatasets() {
        }

        @Override
        public void updateGroupValues() {
        }

    }

}
