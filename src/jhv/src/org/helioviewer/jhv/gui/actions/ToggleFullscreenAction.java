package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.input.KeyShortcuts;

import com.jogamp.newt.opengl.GLWindow;

@SuppressWarnings("serial")
public class ToggleFullscreenAction extends AbstractAction {

    private final KeyStroke exitKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    private final KeyStroke playKey = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);

    public ToggleFullscreenAction() {
        super("Toggle Full Screen");
        putValue(SHORT_DESCRIPTION, "Toggle full screen");

        KeyStroke toggleKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        putValue(ACCELERATOR_KEY, toggleKey);
        KeyShortcuts.registerKey(toggleKey, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final GLWindow window = ImageViewerGui.getGLWindow();
        final boolean full = window.isFullscreen();
        if (full) {
            KeyShortcuts.unregisterKey(exitKey);
            KeyShortcuts.unregisterKey(playKey);
        } else {
            KeyShortcuts.registerKey(exitKey, this);
            KeyShortcuts.registerKey(playKey, MoviePanel.getPlayPauseAction());
        }

        final int w = ImageViewerGui.getGLComponent().getWidth();
        final int h = ImageViewerGui.getGLComponent().getHeight();

        new Thread(() -> {
            window.setFullscreen(!full);
            if (full) // it may have ignored size requests in full screen
                window.setSize(w, h);
        }).start();
    }

}
