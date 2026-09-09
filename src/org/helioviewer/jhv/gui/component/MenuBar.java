package org.helioviewer.jhv.gui.component;

import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.helioviewer.jhv.app.AppInfo;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.Actions;
import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.gui.dialog.AboutDialog;
import org.helioviewer.jhv.gui.dialog.LogDialog;
import org.helioviewer.jhv.gui.dialog.SettingsDialog;
import org.helioviewer.jhv.gui.dialog.TextDialog;
import org.helioviewer.jhv.view.uri.FITSSettings;

// Menu bar of the main window
@SuppressWarnings("serial")
public final class MenuBar extends JMenuBar {

    public MenuBar(ToolBar toolBar, StatusPanel statusPanel) {
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(new Actions.NewLayer());
        fileMenu.add(new Actions.NewSoarLayer());
        fileMenu.add(new Actions.NewSynopticLayer());
        fileMenu.add(new Actions.NewAspiicsLayer());
        fileMenu.add(new Actions.NewPunchLayer());
        fileMenu.add(new Actions.OpenLocalFile());
        fileMenu.add(new Actions.OpenModel());
        fileMenu.addSeparator();
        fileMenu.add(new Actions.LoadState());
        fileMenu.add(new Actions.SaveState());
        fileMenu.add(new Actions.SaveStateAs());
        fileMenu.addSeparator();
        fileMenu.add(new Actions.ReloadSources());

        Actions.ExitProgram exitAction = new Actions.ExitProgram();
        if (Platform.isMacOS()) {
            DesktopIntegration.setQuitHandler((e, response) -> exitAction.actionPerformed(null));
        } else {
            fileMenu.addSeparator();
            fileMenu.add(exitAction);
        }
        add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        editMenu.add(new Actions.Paste());
        editMenu.addSeparator();
        editMenu.add(new Actions.ClearAnnotations());
        add(editMenu);

        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        viewMenu.add(new Actions.ZoomOneToOne());
        viewMenu.add(new Actions.ZoomFit());
        viewMenu.add(new Actions.ZoomIn());
        viewMenu.add(new Actions.ZoomOut());
        viewMenu.add(new Actions.ResetCameraAxis());
        viewMenu.add(new Actions.ResetCamera());
        viewMenu.addSeparator();

        JCheckBoxMenuItem separateMultiviewZoom = new JCheckBoxMenuItem(new Actions.SeparateMultiviewZoom());
        separateMultiviewZoom.setState(Display.separateViewportZoom);
        viewMenu.add(separateMultiviewZoom);

        JCheckBoxMenuItem white = new JCheckBoxMenuItem("Use White Background");
        white.addItemListener(e -> {
            Display.whiteBackground = white.getState();
            DisplayController.display();
        });
        viewMenu.add(white);

        viewMenu.addSeparator();

        JCheckBoxMenuItem showToolbar = new JCheckBoxMenuItem("Show Toolbar", toolBar.isVisible());
        showToolbar.addItemListener(e -> toolBar.setToolbarVisible(showToolbar.getState()));
        viewMenu.add(showToolbar);

        JCheckBoxMenuItem showToolbarText = new JCheckBoxMenuItem("Show Toolbar Text", toolBar.isTextVisible());
        showToolbarText.addItemListener(e -> toolBar.setTextVisible(showToolbarText.getState()));
        viewMenu.add(showToolbarText);

        JCheckBoxMenuItem showStatusBar = new JCheckBoxMenuItem("Show Status Bar", statusPanel.isVisible());
        showStatusBar.addItemListener(e -> statusPanel.setStatusBarVisible(showStatusBar.getState()));
        viewMenu.add(showStatusBar);

        viewMenu.addSeparator();
        viewMenu.add(new Actions.ShowDialog("FITS Settings...", new FITSSettings.SettingsDialog()));

        add(viewMenu);

        JMenu movieMenu = new JMenu("Movie");
        movieMenu.setMnemonic(KeyEvent.VK_M);
        movieMenu.add(Actions.PLAY_PAUSE);
        movieMenu.add(Actions.PREVIOUS_FRAME);
        movieMenu.add(Actions.NEXT_FRAME);
        movieMenu.add(Actions.RECORD);
        add(movieMenu);

        Actions.ShowDialog settingsAction = new Actions.ShowDialog("Settings...", new SettingsDialog());
        if (Platform.isMacOS()) {
            DesktopIntegration.setPreferencesHandler(e -> settingsAction.actionPerformed(null));
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
        helpMenu.add(new Actions.ShowDialog("Interaction Guide...", new TextDialog("Interaction Guide", interactionHelp(), true)));
        helpMenu.add(new Actions.ShowDialog("Timeline Interaction...", new TextDialog("Timeline Interaction", timelineHelp(), true)));

        Actions.ShowDialog aboutAction = new Actions.ShowDialog("About JHelioviewer...", new AboutDialog());
        if (Platform.isMacOS()) {
            DesktopIntegration.setAboutHandler(e -> aboutAction.actionPerformed(null));
        } else {
            helpMenu.add(aboutAction);
        }

        helpMenu.add(new Actions.OpenURLinBrowser("Open User Manual", AppInfo.documentationURL));
        helpMenu.add(new Actions.OpenURLinBrowser("Open Website", "https://www.jhelioviewer.org"));
        helpMenu.add(new Actions.OpenURLinBrowser("Open Change Log", "https://github.com/Helioviewer-Project/JHelioviewer-SWHV/blob/master/changelog.md"));
        helpMenu.add(new Actions.CheckForUpdates());
        helpMenu.addSeparator();
        helpMenu.add(new Actions.ShowDialog("Show Log...", new LogDialog()));
        helpMenu.add(new Actions.OpenURLinBrowser("Report Bug/Request Feature", AppInfo.bugURL));

        add(helpMenu);
    }

    private static String interactionHelp() {
        String shortcut = "⌘ / Ctrl";
        return "<div style='width:480px'>" + """
                <b>Image canvas</b>
                Click the canvas to give it keyboard focus.
                <b>Double-click</b> — reset the view. <b>Right-click</b> — copy the displayed time and cursor coordinates.
                <b>Shift + drag</b> — draw an annotation with the selected annotation tool.
                <b>Shift + N / P</b> — select the next / previous annotation.
                <b>Shift + Delete / Backspace</b> — delete the selected annotation.
                <b>Hold X, Y, or Z</b> — choose the axis in Rotate Axis mode. Release to restore the default (normally Y).
                
                <b>Movie slider and layers</b>
                With the movie slider focused: <b>Space</b> — play/pause; <b>Left / Right</b> — previous / next frame.
                <b>Scroll over the movie slider</b> — step through frames.
                On the slider: <b>⌥ / Alt + drag</b> — trim the nearest range boundary; <b>%1$s + drag</b> — move the playback range.
                In the layer list: <b>%1$s + C</b> — copy the selected layer's time.
                <b>Double-click an image layer's name or time</b> — change its dataset. Drag layer rows to reorder them.
                
                <b>Adjustment controls</b>
                <b>Scroll over a slider or numeric spinner</b> — adjust its value.
                <b>Double-click an adjustment slider</b> — reset it. Drag between a range slider's handles to move both together.
                
                <b>Timeline graph</b>
                See <b>Help → Timeline Interaction</b> for graph, axis, and overview controls.
                
                <b>Dataset selection</b>
                <b>%1$s + click</b> — add or remove individual selections.
                <b>Shift + click</b> — select a range. Change Dataset accepts only one dataset.
                
                <b>Type to search</b>
                Focus a dataset tree or list, the colormap drop-down, or the FITS metadata table, then type.
                Search ignores case and matches the beginning of a name. Use <b>*text</b> to find text anywhere; <b>?</b> matches one character.
                Image and timeline dataset trees also search collapsed branches.
                
                <b>While the search popup is open</b>
                <b>Up / Down</b> — previous / next match. <b>Home / End</b> — first / last match.
                <b>Backspace</b> — edit the search. <b>Esc</b> — close the search popup.
                Where multiple selection is allowed:
                <b>%1$s + Up / Down</b> — add the previous / next match to the selection.
                <b>%1$s + A</b> — select all matches for the current search.
                """.formatted(shortcut) + "</div>";
    }

    private static String timelineHelp() {
        return "<div style='width:500px'>" + """
                <b>Plot and movie time</b>
                <b>Click an empty part of the plot</b> — seek the movie to that time.
                <b>Drag the vertical movie-time marker</b> — scrub through movie frames.
                <b>Drag elsewhere</b> — pan time horizontally and value scales vertically.
                Click an interactive event or feature to activate it instead of seeking the movie.
                
                <b>Wheel or trackpad scrolling over the plot</b>
                <b>Scroll</b> — zoom time around the pointer.
                <b>Shift + scroll</b> — pan time.
                <b>⌥ / Alt + scroll</b> — zoom values only.
                <b>Ctrl + scroll</b> — zoom time and values together.
                Over the time-axis labels, scrolling zooms time even with modifiers held.
                Over a value-axis label area, scrolling zooms that value axis without a modifier.
                
                <b>Which value scales are affected?</b>
                In overlaid mode, vertical dragging or value zooming over the plot affects all value axes. Start over a value-axis label area to target that axis.
                In stacked mode, value adjustments affect only the plot under the pointer (or where the drag started). Time is shared by all plots.
                <b>Double-click</b> in an overlaid plot to fit value scales to the visible data; double-click a value-axis label area to restore its default scale.
                In stacked mode, double-click a plot or its value axis to restore that plot's default scale.
                
                <b>Overview strip below the plot</b>
                The highlighted interval is the time range shown in the plot.
                <b>Drag the highlighted interval</b> — move the range without changing its duration.
                <b>Left-click</b> — center the range on the clicked time.
                <b>Right-click left / right of the range center</b> — shift the range to later / earlier times.
                
                <b>Timeline controls and layers</b>
                The range selector chooses a preset duration, the movie interval, or the maximum interval.
                <b>Synchronize movie with time series</b> — apply timeline range changes to the image movie's time range.
                <b>Stack layers vertically</b> — give each layer its own plot and value scale; scroll the panel when plots do not fit.
                Select a layer to show its options. Its visibility icon shows or hides it; its remove icon removes it when allowed.
                For datasets with level colors, click the color swatch to switch between multicolor and single-color rendering.
                """ + "</div>";
    }

}
