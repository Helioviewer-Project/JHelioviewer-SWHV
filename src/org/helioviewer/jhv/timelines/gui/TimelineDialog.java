package org.helioviewer.jhv.timelines.gui;

import java.awt.event.ActionEvent;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;

import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.JHVFrame;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public final class TimelineDialog extends StandardDialog implements Interfaces.ShowableDialog {

    private final TimelineDataPanel observationPanel = new TimelineDataPanel();

    public TimelineDialog() {
        super(JHVFrame.getFrame(), "New Layer", true);
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

        ButtonPanel panel = new ButtonPanel();
        panel.add(okBtn, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(cancelBtn, ButtonPanel.CANCEL_BUTTON);

        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        observationPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        return observationPanel;
    }

    @Nullable
    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    @Override
    public void showDialog() {
        observationPanel.updateGroupValues();
        pack();
        setLocationRelativeTo(JHVFrame.getFrame());
        setVisible(true);
    }

    public TimelineDataPanel getObservationPanel() {
        return observationPanel;
    }

}
