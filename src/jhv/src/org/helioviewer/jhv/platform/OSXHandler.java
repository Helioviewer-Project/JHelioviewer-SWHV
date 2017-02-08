package org.helioviewer.jhv.platform;

import java.lang.reflect.Method;

import org.helioviewer.jhv.ExitHooks;

public class OSXHandler {

    public static void aboutHandler(Object action) {
        try {
            Method m = action.getClass().getDeclaredMethod("show", (Class[]) null);
            OSXAdapter.setAboutHandler(action, m);
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void preferencesHandler(Object action) {
        try {
            Method m = action.getClass().getDeclaredMethod("show", (Class[]) null);
            OSXAdapter.setPreferencesHandler(action, m);
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void quitHandler() {
        try {
            Method m = ExitHooks.class.getMethod("exitProgram", (Class[]) null);
            OSXAdapter.setQuitHandler("", m);
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
    }

}
