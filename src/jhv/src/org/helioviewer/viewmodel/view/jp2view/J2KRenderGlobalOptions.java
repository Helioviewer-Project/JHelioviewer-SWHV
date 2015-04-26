package org.helioviewer.viewmodel.view.jp2view;

public class J2KRenderGlobalOptions {

    private static boolean useDoubleBuffering = false;

    public static void setDoubleBufferingOption(boolean isActivated) {
        useDoubleBuffering = isActivated;
    }

    public static boolean getDoubleBufferingOption() {
        return useDoubleBuffering;
    }

}
