package org.helioviewer.jhv.plugins.swek.swing;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.JDialog;

import org.helioviewer.jhv.event.SWEKSession;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.component.SwingPanel;
import org.helioviewer.jhv.plugins.swek.FilterScreen;

@SuppressWarnings("serial")
final class FilterDialog extends JDialog implements FocusListener, WindowFocusListener {

    private final FilterScreen screen = new FilterScreen();

    FilterDialog(SWEKSession session, SWEKSupplier supplier) {
        super(MainFrame.get());
        setUndecorated(true);
        addFocusListener(this);
        addWindowFocusListener(this);

        screen.show(session, supplier);
        setContentPane(new SwingPanel(screen));
        pack();
    }

    @Override
    public void dispose() {
        screen.clear();
        super.dispose();
    }

    @Override
    public void focusGained(FocusEvent e) {}

    @Override
    public void focusLost(FocusEvent e) {
        setVisible(false);
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {}

    @Override
    public void windowLostFocus(WindowEvent e) {
        setVisible(false);
    }

}
