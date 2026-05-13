package org.helioviewer.jhv.swing;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.desktop.AboutHandler;
import java.awt.desktop.PreferencesHandler;
import java.awt.desktop.QuitHandler;
import java.io.File;
import java.net.URI;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.helioviewer.jhv.Log;

public final class DesktopIntegration {

    public static final boolean canBrowse = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    public static final boolean canOpen = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN);
    public static final int menuShortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

    public static final HyperlinkListener hyperOpenURL = e -> {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getURL() != null)
            openURL(e.getURL().toString());
    };

    private DesktopIntegration() {}

    public static void openURL(String url) {
        try {
            if (url == null)
                return;

            openURI(new URI(url));
        } catch (Exception e) {
            Log.warn(e);
        }
    }

    private static void openURI(URI uri) throws Exception {
        if ("file".equalsIgnoreCase(uri.getScheme()) && canOpen)
            Desktop.getDesktop().open(new File(uri));
        else if (canBrowse)
            Desktop.getDesktop().browse(uri);
    }

    public static void setQuitHandler(QuitHandler handler) {
        Desktop.getDesktop().setQuitHandler(handler);
    }

    public static void setPreferencesHandler(PreferencesHandler handler) {
        Desktop.getDesktop().setPreferencesHandler(handler);
    }

    public static void setAboutHandler(AboutHandler handler) {
        Desktop.getDesktop().setAboutHandler(handler);
    }

}
