package org.helioviewer.jhv.gui.dialogs;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class TextDialog extends StandardDialog implements ShowableDialog {

    private final String text;

    public TextDialog(String title, String _text, boolean resizable) {
        super(ImageViewerGui.getMainFrame(), title, true);
        setResizable(resizable);
        text = _text.replace("\n", "<br>");
    }

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        setDefaultAction(close);
        setDefaultCancelAction(close);

        JButton button = new JButton(close);
        button.setText("Close");
        setInitFocusedComponent(button);

        ButtonPanel panel = new ButtonPanel();
        panel.add(button, ButtonPanel.AFFIRMATIVE_BUTTON);

        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        JTextPane pane = new JTextPane();
        pane.setContentType("text/html");
        pane.setText(text);
        pane.setEditable(false);
        pane.addHyperlinkListener(JHVGlobals.hyperOpenURL);
        pane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        return new JScrollPane(pane);
    }

    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    @Override
    public void showDialog() {
        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

}
