package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.util.ArrayList;
import java.util.List;

public enum DrawableType {
    FULL_IMAGE, LINE, ICON, TEXT, TEXT_ICON;

    public static List<DrawableType> getZOrderedList() {
        ArrayList<DrawableType> tempList = new ArrayList<DrawableType>();
        tempList.add(DrawableType.FULL_IMAGE);
        tempList.add(DrawableType.TEXT_ICON);
        tempList.add(DrawableType.LINE);
        tempList.add(DrawableType.ICON);
        tempList.add(DrawableType.TEXT);
        return tempList;
    }
}
