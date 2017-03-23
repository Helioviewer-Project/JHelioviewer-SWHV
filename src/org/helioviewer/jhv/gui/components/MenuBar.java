package org.helioviewer.jhv.gui.components;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.helioviewer.jhv.gui.actions.CheckUpdateAction;
import org.helioviewer.jhv.gui.actions.ClearAnnotationsAction;
import org.helioviewer.jhv.gui.actions.ExitProgramAction;
import org.helioviewer.jhv.gui.actions.NewLayerAction;
import org.helioviewer.jhv.gui.actions.OpenLocalFileAction;
import org.helioviewer.jhv.gui.actions.OpenURLinBrowserAction;
import org.helioviewer.jhv.gui.actions.ResetCameraAction;
import org.helioviewer.jhv.gui.actions.ShowDialogAction;
import org.helioviewer.jhv.gui.actions.ToggleFullscreenAction;
import org.helioviewer.jhv.gui.actions.WindowMinimizeAction;
import org.helioviewer.jhv.gui.actions.WindowZoomAction;
import org.helioviewer.jhv.gui.actions.ZoomFitAction;
import org.helioviewer.jhv.gui.actions.ZoomInAction;
import org.helioviewer.jhv.gui.actions.ZoomOneToOneAction;
import org.helioviewer.jhv.gui.actions.ZoomOutAction;
import org.helioviewer.jhv.gui.dialogs.AboutDialog;
import org.helioviewer.jhv.gui.dialogs.PreferencesDialog;
import org.helioviewer.jhv.gui.dialogs.plugins.PluginsDialog;
import org.helioviewer.jhv.platform.OSXHandler;

// Menu bar of the main window
@SuppressWarnings("serial")
public class MenuBar extends JMenuBar {

    public MenuBar() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(new NewLayerAction());
        fileMenu.add(new OpenLocalFileAction());

        if (System.getProperty("jhv.os").equals("mac")) {
            OSXHandler.quitHandler();
        } else {
            fileMenu.addSeparator();
            fileMenu.add(new ExitProgramAction());
        }
        add(fileMenu);

        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        viewMenu.add(new ToggleFullscreenAction());
        viewMenu.addSeparator();
        viewMenu.add(new ZoomOneToOneAction());
        viewMenu.add(new ZoomFitAction());
        viewMenu.add(new ZoomInAction());
        viewMenu.add(new ZoomOutAction());
        viewMenu.addSeparator();
        viewMenu.add(new ResetCameraAction());
        viewMenu.add(new ClearAnnotationsAction());
        add(viewMenu);

        JMenu movieMenu = new JMenu("Movie");
        movieMenu.setMnemonic(KeyEvent.VK_M);
        movieMenu.add(MoviePanel.getPlayPauseAction());
        movieMenu.add(MoviePanel.getPreviousFrameAction());
        movieMenu.add(MoviePanel.getNextFrameAction());
        add(movieMenu);

        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setMnemonic(KeyEvent.VK_T);
        toolsMenu.add(new ShowDialogAction("Manage Plug-ins...", new PluginsDialog()));
        add(toolsMenu);

        ShowDialogAction preferencesAction = new ShowDialogAction("Preferences...", new PreferencesDialog());
        if (System.getProperty("jhv.os").equals("mac")) {
            OSXHandler.preferencesHandler(preferencesAction);
            JMenu windowMenu = new JMenu("Window");
            windowMenu.setMnemonic(KeyEvent.VK_W);
            windowMenu.add(new WindowMinimizeAction());
            windowMenu.add(new WindowZoomAction());
            add(windowMenu);
        } else {
            toolsMenu.add(preferencesAction);
        }

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        ShowDialogAction aboutAction = new ShowDialogAction("About JHelioviewer...", new AboutDialog());
        if (System.getProperty("jhv.os").equals("mac")) {
            OSXHandler.aboutHandler(aboutAction);
        } else {
            helpMenu.add(aboutAction);
        }

        helpMenu.add(new OpenURLinBrowserAction("Open User Manual", "http://swhv.oma.be/user_manual/"));
        helpMenu.add(new OpenURLinBrowserAction("Open Website", "http://www.jhelioviewer.org"));
        helpMenu.add(new CheckUpdateAction());
        helpMenu.addSeparator();
        helpMenu.add(new OpenURLinBrowserAction("Report a Bug/Feature Request", "https://github.com/Helioviewer-Project/JHelioviewer-SWHV/issues"));
        add(helpMenu);
    }

}
