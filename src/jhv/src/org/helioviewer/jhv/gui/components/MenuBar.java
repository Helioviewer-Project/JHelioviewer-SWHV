package org.helioviewer.jhv.gui.components;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.helioviewer.jhv.gui.actions.ClearAnnotationsAction;
import org.helioviewer.jhv.gui.actions.ExitProgramAction;
import org.helioviewer.jhv.gui.actions.OpenLocalFileAction;
import org.helioviewer.jhv.gui.actions.OpenURLinBrowserAction;
import org.helioviewer.jhv.gui.actions.ResetCameraAction;
import org.helioviewer.jhv.gui.actions.ShowDialogAction;
import org.helioviewer.jhv.gui.actions.ToggleFullscreenAction;
import org.helioviewer.jhv.gui.actions.ZoomFitAction;
import org.helioviewer.jhv.gui.actions.ZoomInAction;
import org.helioviewer.jhv.gui.actions.ZoomOneToOneAction;
import org.helioviewer.jhv.gui.actions.ZoomOutAction;
import org.helioviewer.jhv.gui.dialogs.AboutDialog;
import org.helioviewer.jhv.gui.dialogs.HelpDialog;
//import org.helioviewer.jhv.gui.dialogs.OpenRemoteFileDialog;
import org.helioviewer.jhv.gui.dialogs.PreferencesDialog;
import org.helioviewer.jhv.gui.dialogs.plugins.PluginsDialog;
import org.helioviewer.jhv.platform.OSXHandler;

/**
 * Menu bar of the main window.
 *
 * <p>
 * Basically, contains all actions from {@link org.helioviewer.jhv.gui.actions}.
 *
 * @author Markus Langenberg
 *
 */
@SuppressWarnings("serial")
public class MenuBar extends JMenuBar {

    public MenuBar() {
        super();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(new OpenLocalFileAction());
        // fileMenu.add(new ShowDialogAction("Open Remote Image...", OpenRemoteFileDialog.class));

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
        viewMenu.add(new ZoomInAction(true, false));
        viewMenu.add(new ZoomOutAction(true, false));
        viewMenu.add(new ZoomFitAction(true, false));
        viewMenu.add(new ZoomOneToOneAction(true, false));
        viewMenu.addSeparator();
        viewMenu.add(new ResetCameraAction(true, false));
        viewMenu.add(new ClearAnnotationsAction(true, false));
        add(viewMenu);

        JMenu movieMenu = new JMenu("Movie");
        movieMenu.setMnemonic(KeyEvent.VK_A);
        movieMenu.add(new MoviePanel.StaticPlayPauseAction());
        movieMenu.add(new MoviePanel.StaticPreviousFrameAction());
        movieMenu.add(new MoviePanel.StaticNextFrameAction());
        add(movieMenu);

        JMenu pluginsMenu = new JMenu("Plug-ins");
        pluginsMenu.setMnemonic(KeyEvent.VK_I);
        pluginsMenu.add(new ShowDialogAction("Manage Plug-ins...", PluginsDialog.class));
        add(pluginsMenu);

        if (System.getProperty("jhv.os").equals("mac")) {
            OSXHandler.preferencesHandler();
        } else {
            JMenu optionsMenu = new JMenu("Options");
            optionsMenu.setMnemonic(KeyEvent.VK_O);
            optionsMenu.add(new ShowDialogAction("Preferences...", PreferencesDialog.class));
            add(optionsMenu);
        }

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        if (System.getProperty("jhv.os").equals("mac")) {
            OSXHandler.aboutHandler();
        } else {
            helpMenu.add(new ShowDialogAction("About JHelioviewer...", AboutDialog.class));
        }

        helpMenu.add(new OpenURLinBrowserAction("Open User Manual", "http://swhv.oma.be/user_manual/"));
        helpMenu.add(new ShowDialogAction("Show Shortcuts...", HelpDialog.class));
        helpMenu.addSeparator();
        helpMenu.add(new OpenURLinBrowserAction("Report a Bug", "https://bugs.launchpad.net/jhelioviewer/+filebug"));
        helpMenu.add(new OpenURLinBrowserAction("Submit a Feature Request", "https://bugs.launchpad.net/jhelioviewer/+filebug"));
        helpMenu.addSeparator();
        helpMenu.add(new OpenURLinBrowserAction("Open Website", "http://www.jhelioviewer.org"));
        add(helpMenu);
    }

}
