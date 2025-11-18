package org.helioviewer.jhv.gui.components;

import java.awt.Desktop;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Platform;
import org.helioviewer.jhv.gui.Actions;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.dialogs.AboutDialog;
import org.helioviewer.jhv.gui.dialogs.LogDialog;
import org.helioviewer.jhv.gui.dialogs.SettingsDialog;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.view.uri.FITSSettings;

// Menu bar of the main window
@SuppressWarnings("serial")
public final class MenuBar extends JMenuBar {

    public MenuBar() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(new Actions.NewLayer());
        fileMenu.add(new Actions.NewSoarLayer());
        fileMenu.add(new Actions.OpenLocalFile());
        fileMenu.addSeparator();
        fileMenu.add(new Actions.LoadState());
        fileMenu.add(new Actions.SaveState());
        fileMenu.add(new Actions.SaveStateAs());
        fileMenu.addSeparator();
        fileMenu.add(new Actions.ReloadSources());

        Actions.ExitProgram exitAction = new Actions.ExitProgram();
        if (Platform.isMacOS()) {
            Desktop.getDesktop().setQuitHandler((e, response) -> exitAction.actionPerformed(null));
        } else {
            fileMenu.addSeparator();
            fileMenu.add(exitAction);
        }
        add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        editMenu.add(new Actions.Paste());
        add(editMenu);

        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        viewMenu.add(new Actions.ZoomOneToOne());
        viewMenu.add(new Actions.ZoomFit());
        viewMenu.add(new Actions.ZoomIn());
        viewMenu.add(new Actions.ZoomOut());
        viewMenu.addSeparator();
        viewMenu.add(new Actions.ResetCameraAxis());
        viewMenu.add(new Actions.ResetCamera());
        viewMenu.addSeparator();
        viewMenu.add(new Actions.ClearAnnotations());

        JCheckBoxMenuItem white = new JCheckBoxMenuItem("Use White Background");
        white.addItemListener(e -> {
            JHVFrame.getGLListener().setWhiteBack(white.getState());
            MovieDisplay.display();
        });
        viewMenu.add(white);

        viewMenu.addSeparator();
        viewMenu.add(new Actions.ShowDialog("FITS Settings...", new FITSSettings.SettingsDialog()));

        add(viewMenu);

        JMenu movieMenu = new JMenu("Movie");
        movieMenu.setMnemonic(KeyEvent.VK_M);
        movieMenu.add(MoviePanel.getPlayPauseAction());
        movieMenu.add(MoviePanel.getPreviousFrameAction());
        movieMenu.add(MoviePanel.getNextFrameAction());
        add(movieMenu);

        Actions.ShowDialog settingsAction = new Actions.ShowDialog("Settings...", new SettingsDialog());
        if (Platform.isMacOS()) {
            Desktop.getDesktop().setPreferencesHandler(e -> settingsAction.actionPerformed(null));
            JMenu windowMenu = new JMenu("Window");
            windowMenu.setMnemonic(KeyEvent.VK_W);
            windowMenu.add(new Actions.WindowMinimize());
            windowMenu.add(new Actions.WindowZoom());
            add(windowMenu);
        } else {
            JMenu toolsMenu = new JMenu("Tools");
            toolsMenu.setMnemonic(KeyEvent.VK_T);
            toolsMenu.add(settingsAction);
            add(toolsMenu);
        }

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        Actions.ShowDialog aboutAction = new Actions.ShowDialog("About JHelioviewer...", new AboutDialog());
        if (Platform.isMacOS()) {
            Desktop.getDesktop().setAboutHandler(e -> aboutAction.actionPerformed(null));
        } else {
            helpMenu.add(aboutAction);
        }

        helpMenu.add(new Actions.OpenURLinBrowser("Open User Manual", JHVGlobals.documentationURL));
        helpMenu.add(new Actions.OpenURLinBrowser("Open Website", "https://www.jhelioviewer.org"));
        helpMenu.add(new Actions.OpenURLinBrowser("Open Change Log", "https://github.com/Helioviewer-Project/JHelioviewer-SWHV/blob/master/changelog.md"));
        helpMenu.add(new Actions.UpdateChecker());
        helpMenu.addSeparator();
        helpMenu.add(new Actions.ShowDialog("Show Log...", new LogDialog()));
        helpMenu.add(new Actions.OpenURLinBrowser("Report Bug/Request Feature", JHVGlobals.bugURL));

        add(helpMenu);
    }

}
