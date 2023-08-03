package org.helioviewer.jhv.gui.actions;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.input.KeyShortcuts;
import org.helioviewer.jhv.io.ExtensionFileFilter;
import org.helioviewer.jhv.layers.selector.State;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public final class SaveStateAsAction extends AbstractAction {

    public SaveStateAsAction() {
        super("Save State As...");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_S, UIGlobals.menuShortcutMask | InputEvent.ALT_DOWN_MASK);
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        FileDialog fileDialog = new FileDialog(JHVFrame.getFrame(), "Save as...", FileDialog.SAVE);
        // does not work on Windows
        fileDialog.setFilenameFilter(ExtensionFileFilter.JHV);
        fileDialog.setMultipleMode(false);
        fileDialog.setDirectory(Settings.getProperty("path.state"));
        fileDialog.setFile("state__" + TimeUtils.formatFilename(System.currentTimeMillis()) + ".jhv");
        fileDialog.setVisible(true);

        String directory = fileDialog.getDirectory();
        String file = fileDialog.getFile();
        if (directory != null && file != null) {
            Settings.setProperty("path.state", directory); // remember the current directory for future
            if (!file.toLowerCase(Locale.ENGLISH).endsWith(".jhv"))
                file += ".jhv";
            State.save(directory, file);
        }
    }

}

