package org.helioviewer.jhv.gui.dialogs;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
class CloseButtonPanel extends ButtonPanel {

    CloseButtonPanel(StandardDialog dialog) {
        AbstractAction close = new CloseAction(dialog);
        dialog.setDefaultAction(close);
        dialog.setDefaultCancelAction(close);

        JButton button = new JButton(close);
        button.setText("Close");
        dialog.setInitFocusedComponent(button);
        add(button, ButtonPanel.AFFIRMATIVE_BUTTON);
    }

    private static class CloseAction extends AbstractAction {

        private final StandardDialog dialog;

        CloseAction(StandardDialog _dialog) {
            dialog = _dialog;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            dialog.setVisible(false);
        }

    }

}
