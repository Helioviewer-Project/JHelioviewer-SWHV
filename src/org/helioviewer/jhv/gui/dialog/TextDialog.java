package org.helioviewer.jhv.gui.dialog;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.gui.component.HTMLPane;

import com.jidesoft.dialog.ButtonPanel;

@SuppressWarnings({"serial", "this-escape"})
public class TextDialog extends BaseDialog {

    private final String text;

    public TextDialog(String title, String _text, boolean resizable) {
        super(title, true);
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
        pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return new JScrollPane(pane);
    }

}
