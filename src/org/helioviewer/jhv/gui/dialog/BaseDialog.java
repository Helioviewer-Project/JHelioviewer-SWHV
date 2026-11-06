package org.helioviewer.jhv.gui.dialog;

import java.awt.BorderLayout;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JComponent;

import org.helioviewer.jhv.gui.MainFrame;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import com.jidesoft.dialog.StandardDialogPane;

// The margins and gaps shared by all dialogs.
@SuppressWarnings("serial")
public abstract class BaseDialog extends StandardDialog {

    public static final int GAP = 6;

    protected BaseDialog(String title, boolean modal) {
        super(MainFrame.get(), title, modal);
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        StandardDialogPane pane = getStandardDialogPane();
        pane.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));
        ((BorderLayout) pane.getLayout()).setVgap(GAP);
        getButtonPanel().setSizeConstraint(ButtonPanel.NO_LESS_THAN);
    }

    @Nullable
    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    public void showDialog() {
        pack();
        setLocationRelativeTo(MainFrame.get());
        setVisible(true);
    }

}
