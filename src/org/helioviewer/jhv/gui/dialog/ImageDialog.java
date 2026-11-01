package org.helioviewer.jhv.gui.dialog;

import java.awt.Window;

import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.component.SwingPanel;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.ui.ImageScreen;

@SuppressWarnings("serial")
public final class ImageDialog extends ScreenDialog {

    private final ImageScreen screen;

    public ImageDialog() {
        this(new ImageScreen(DesktopIntegration::openURL));
    }

    private ImageDialog(ImageScreen _screen) {
        super(_screen, false);
        screen = _screen;
        setType(Window.Type.UTILITY);
    }

    public void showNewLayerSelector() {
        screen.show();
        showSelector();
    }

    public void changeDataset(ImageLayer layer) {
        screen.show(layer);
        showSelector();
    }

    private void showSelector() {
        setTitle(screen.title());
        if (isVisible()) {
            toFront();
        } else {
            initialize();
            ((SwingPanel) createContentPanel()).refresh();
            pack();
            setLocation(MainFrame.getRenderComponent().getLocationOnScreen());
            setVisible(true);
        }
    }
}
