package org.helioviewer.jhv.gui.dialog;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.gui.component.SwingPanel;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.io.ProxySettings;
import org.helioviewer.jhv.ui.DialogScreen;
import org.helioviewer.jhv.ui.Panel;

// Real window events through the dialogs, which are never shown.
public final class SettingsDialogTest {

    private static void run() throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        DataSources.initSources();
        SwingUtilities.invokeAndWait(() -> {
            try {
                settingsWindowClose();
                screenWindowClose();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public static void main(String[] args) {
        int status = 0;
        try {
            run();
            System.out.println("SettingsDialogTest passed");
        } catch (Throwable failure) {
            failure.printStackTrace();
            status = 1;
        }
        System.exit(status);
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    private static <T> List<T> all(Container parent, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child))
                found.add(type.cast(child));
            if (child instanceof Container container)
                found.addAll(all(container, type));
        }
        return found;
    }

    private static <T> T field(String name, Class<T> type, ScreenDialog dialog) throws Exception {
        Field field = ScreenDialog.class.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(dialog));
    }

    private static void windowClosing(ScreenDialog dialog) {
        dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
    }

    // What showDialog does before it shows the window.
    private static void open(ScreenDialog dialog) throws Exception {
        dialog.initialize();
        field("screen", DialogScreen.class, dialog).show();
        field("content", SwingPanel.class, dialog).refresh();
        dialog.pack();
    }

    private static boolean stored(String user, String password) {
        return user.equals(Settings.getProperty("proxy.username")) && password.equals(ProxySettings.getPassword());
    }

    // The title-bar X saves nothing and keeps the drafts for the next opening; Close and Esc save them.
    private static void settingsWindowClose() throws Exception {
        Settings.setProperty("proxy.username", "stored");
        ProxySettings.setPassword("secret");
        SettingsDialog dialog = SettingsDialog.getInstance();
        open(dialog);
        JTextField user = all(dialog.getContentPane(), JTextField.class).stream().filter(field -> !(field instanceof JPasswordField)).findFirst().orElseThrow();
        JPasswordField password = all(dialog.getContentPane(), JPasswordField.class).getFirst();
        check("stored".equals(user.getText()) && "secret".equals(new String(password.getPassword())), "The dialog did not show the stored credentials");

        user.setText("draft");
        password.setText("draft-secret");
        windowClosing(dialog);
        check(stored("stored", "secret"), "The title-bar X saved the proxy drafts");
        check(!dialog.isDisplayable(), "The title-bar X did not dispose the dialog");

        open(dialog);
        check("draft".equals(user.getText()) && "draft-secret".equals(new String(password.getPassword())), "Reopening lost the proxy drafts");
        check(stored("stored", "secret"), "Reopening saved the proxy drafts");

        AbstractButton close = all(dialog.getContentPane(), AbstractButton.class).stream().filter(button -> "Close".equals(button.getText())).findFirst().orElseThrow();
        close.doClick();
        check(stored("draft", "draft-secret"), "Close did not save the kept drafts");

        open(dialog);
        user.setText("escaped");
        dialog.getDefaultCancelAction().actionPerformed(new ActionEvent(dialog, ActionEvent.ACTION_PERFORMED, "cancel"));
        check(stored("escaped", "draft-secret"), "Esc did not save the proxy drafts");
        check(!dialog.isVisible(), "A window was opened");
        dialog.dispose();
    }

    // Every other screen dialog closes through its screen.
    private static void screenWindowClose() {
        int[] closed = new int[1];
        ScreenDialog dialog = new ScreenDialog(new DialogScreen() {
            @Override
            public String title() {
                return "Screen";
            }

            @Override
            public Panel build() {
                return new Panel().label("Screen");
            }

            @Override
            public void close() {
                closed[0]++;
                super.close();
            }
        }, false);
        windowClosing(dialog);
        check(closed[0] == 1, "The window close did not reach the screen");
        dialog.dispose();
    }

}
