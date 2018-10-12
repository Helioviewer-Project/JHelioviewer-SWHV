package org.helioviewer.jhv.gui.dialogs;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.components.base.HTMLPane;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class TextDialog extends StandardDialog implements ShowableDialog {

    private final String text;

    public TextDialog(String title, String _text, boolean resizable) {
        super(JHVFrame.getFrame(), title, true);
        setResizable(resizable);
        text = _text;
    }

    @Override
    public ButtonPanel createButtonPanel() {
        return new CloseButtonPanel(this);
    }

    @Override
    public JComponent createContentPanel() {
        HTMLPane pane = new HTMLPane();
        pane.setText(text);
        pane.addHyperlinkListener(JHVGlobals.hyperOpenURL);
        pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return new JScrollPane(pane);
    }

    @Nullable
    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    @Override
    public void showDialog() {
        pack();
        setLocationRelativeTo(JHVFrame.getFrame());
        setVisible(true);
    }

}
