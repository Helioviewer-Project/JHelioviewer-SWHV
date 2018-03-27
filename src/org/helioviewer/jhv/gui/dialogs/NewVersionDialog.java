package org.helioviewer.jhv.gui.dialogs;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.UIGlobals;

import com.jidesoft.dialog.ButtonPanel;

@SuppressWarnings("serial")
public class NewVersionDialog extends TextDialog {

    // setting for check.update.next
    private int nextCheck = 0;
    // suspended startups when clicked remindMeLater
    private static final int suspendedStarts = 5;
    private final boolean verbose;

    public NewVersionDialog(String _text, boolean _verbose) {
        super("New Version Available", _text, false);
        verbose = _verbose;
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

        JButton closeBtn = new JButton(close);
        closeBtn.setText("Close");

        AbstractAction download = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JHVGlobals.openURL(JHVGlobals.downloadURL);
                setVisible(false);
            }
        };
        setDefaultAction(download);

        JButton downBtn = new JButton(download);
        downBtn.setEnabled(UIGlobals.canBrowse);
        downBtn.setText("Download");
        setInitFocusedComponent(downBtn);

        ButtonPanel panel = new ButtonPanel();

        if (!verbose) {
            JButton laterBtn = new JButton("Remind me later");
            laterBtn.addActionListener(e -> {
                setVisible(false);
                nextCheck = suspendedStarts;
            });
            panel.add(laterBtn, ButtonPanel.OTHER_BUTTON);
        }

        panel.add(downBtn, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(closeBtn, ButtonPanel.CANCEL_BUTTON);

        return panel;
    }

    public int getNextCheck() {
        return nextCheck;
    }

}
