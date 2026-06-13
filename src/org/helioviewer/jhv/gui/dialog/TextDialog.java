package org.helioviewer.jhv.gui.dialog;

import java.awt.BorderLayout;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.component.HTMLPane;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings({"serial", "this-escape"})
public class TextDialog extends StandardDialog implements Interfaces.ShowableDialog {

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
        pane.setOpaque(false);
        pane.setText(text);
        pane.addHyperlinkListener(DesktopIntegration.hyperOpenURL);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(pane);
        return new JScrollPane(panel);
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
