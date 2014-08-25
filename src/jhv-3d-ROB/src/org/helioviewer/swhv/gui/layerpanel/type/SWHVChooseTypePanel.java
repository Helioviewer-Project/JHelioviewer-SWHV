package org.helioviewer.swhv.gui.layerpanel.type;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class SWHVChooseTypePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private JButton addButton;
    private SWHVChooseTypeController controller;

    public SWHVChooseTypePanel(final SWHVChooseTypeModel model) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                addButton = new JButton(model.getName());
                addButton.setToolTipText(model.getName());
                add(addButton);
                addButton.addActionListener(model.getActionListener());
            }

        });
    }

    public SWHVChooseTypeController getController() {
        return this.controller;
    }

    public void getController(SWHVChooseTypeController controller) {
        this.controller = controller;
    }
}
