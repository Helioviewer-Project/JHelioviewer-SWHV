package org.helioviewer.jhv.platform;

import java.lang.reflect.Method;

import org.helioviewer.jhv.ExitHooks;
import org.helioviewer.jhv.gui.dialogs.AboutDialog;
import org.helioviewer.jhv.gui.dialogs.PreferencesDialog;

public class OSXHandler {

    public static void aboutHandler() {
        try {
            Class[] cArg = new Class[0];
            Method m = AboutDialog.class.getMethod("dialogShow", cArg);
            OSXAdapter.setAboutHandler("", m);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void preferencesHandler() {
        try {
            Class[] cArg = new Class[0];
            Method m = PreferencesDialog.class.getMethod("dialogShow", cArg);
            OSXAdapter.setPreferencesHandler("", m);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void quitHandler() {
        try {
            Class[] cArg = new Class[0];
            Method m = ExitHooks.class.getMethod("exitProgram", cArg);
            OSXAdapter.setQuitHandler("", m);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

}
