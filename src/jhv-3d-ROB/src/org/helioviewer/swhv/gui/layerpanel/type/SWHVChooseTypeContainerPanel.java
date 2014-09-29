package org.helioviewer.swhv.gui.layerpanel.type;

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.helioviewer.swhv.mvc.SWHVPanel;

public class SWHVChooseTypeContainerPanel extends JPanel implements SWHVChooseTypeContainerModelListener, SWHVPanel {
    private static final long serialVersionUID = 1L;

    private static SWHVChooseTypeContainerPanel singletonInstance = new SWHVChooseTypeContainerPanel();
    private SWHVChooseTypeContainerController controller;

    private SWHVChooseTypeContainerPanel() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setLayout(new FlowLayout(FlowLayout.LEADING));
                add(new JLabel("Add new"));
            }
        });
    }

    @Override
    public void typeRegistered(final SWHVChooseTypeContainerModel model) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < model.getRegisteredModels().length; i++) {
                    SWHVChooseTypeController ct = model.getRegisteredModels()[i].getController();
                    add(ct.getPanel());
                }
            }
        });
    }

    public static SWHVChooseTypeContainerPanel getSingletonInstance() {
        return singletonInstance;
    }

    @Override
    public SWHVChooseTypeContainerController getController() {
        return this.controller;
    }

}
