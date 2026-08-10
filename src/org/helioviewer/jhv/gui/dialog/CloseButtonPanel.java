package org.helioviewer.jhv.gui.dialog;

import javax.swing.AbstractAction;
import javax.swing.JButton;

import org.helioviewer.jhv.gui.ComponentUtils;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
class CloseButtonPanel extends ButtonPanel {

    CloseButtonPanel(StandardDialog dialog) {
        AbstractAction close = ComponentUtils.hideAction(dialog);
        dialog.setDefaultAction(close);
        dialog.setDefaultCancelAction(close);

        JButton button = new JButton(close);
        button.setText("Close");
        dialog.setInitFocusedComponent(button);
        add(button, ButtonPanel.AFFIRMATIVE_BUTTON);
    }

}
