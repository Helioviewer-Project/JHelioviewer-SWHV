package org.helioviewer.jhv.plugins.swek.swing;

import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.WindowConstants;

import org.helioviewer.jhv.event.RelatedEvents;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.component.SwingPanel;
import org.helioviewer.jhv.plugins.swek.EventInformationScreen;

// A JDialog, so it can appear above the heavyweight render surface.
@SuppressWarnings("serial")
final class SWEKEventInformationDialog extends JDialog {

    private final EventInformationScreen screen;

    SWEKEventInformationDialog(SWEKSwingUI ui, RelatedEvents related, SolarEvent event) {
        super(MainFrame.get());
        setType(Window.Type.UTILITY); // avoids tab on macOS when Prefer tabs is always
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        screen = new EventInformationScreen(ui.session, related, event, ui::open);
        setTitle(screen.title());
        add(new SwingPanel(screen));
    }

    @Override
    public void dispose() {
        screen.close();
        super.dispose();
    }

}
