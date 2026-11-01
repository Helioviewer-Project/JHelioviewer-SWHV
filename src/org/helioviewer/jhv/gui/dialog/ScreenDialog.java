package org.helioviewer.jhv.gui.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.JComponent;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.component.SwingPanel;
import org.helioviewer.jhv.ui.DialogScreen;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

// A fixed-size window around a shared screen.
@SuppressWarnings({"serial", "this-escape"})
public class ScreenDialog extends StandardDialog {

    private final DialogScreen screen;
    private final SwingPanel content;

    public ScreenDialog(DialogScreen _screen, boolean modal) {
        super(MainFrame.get(), _screen.title(), modal);
        screen = _screen;
        content = new SwingPanel(screen);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                screen.close();
            }
        });
        screen.onDismiss(() -> setVisible(false));
    }

    @Override
    public ButtonPanel createButtonPanel() {
        setDefaultCancelAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                screen.close();
            }
        });
        setInitFocusedComponent(content.initialFocus());
        return null;
    }

    @Override
    public JComponent createContentPanel() {
        return content;
    }

    @Nullable
    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    public void showDialog() {
        if (isVisible()) {
            toFront();
        } else {
            initialize();
            screen.show();
            content.refresh();
            pack();
            setLocationRelativeTo(MainFrame.get());
            setVisible(true);
        }
    }

}
