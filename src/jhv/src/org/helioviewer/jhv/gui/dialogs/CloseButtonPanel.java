package org.helioviewer.jhv.gui.dialogs;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class CloseButtonPanel extends ButtonPanel {

    public CloseButtonPanel(StandardDialog dialog) {
        AbstractAction close = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        };
        dialog.setDefaultAction(close);
        dialog.setDefaultCancelAction(close);

        JButton button = new JButton(close);
        button.setText("Close");
        dialog.setInitFocusedComponent(button);
        add(button, ButtonPanel.AFFIRMATIVE_BUTTON);
    }

}
