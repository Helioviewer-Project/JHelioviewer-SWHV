package org.helioviewer.jhv.gui;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;

import org.helioviewer.jhv.gui.IconBank.JHVIcon;

public class JHVCursors {
    public static final Cursor openHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.OPEN_HAND).getImage(), new Point(16, 8), IconBank.getIcon(JHVIcon.OPEN_HAND).toString());
    public static final Cursor closedHandCursor = Toolkit.getDefaultToolkit().createCustomCursor(IconBank.getIcon(JHVIcon.CLOSED_HAND).getImage(), new Point(16, 8), IconBank.getIcon(JHVIcon.CLOSED_HAND).toString());

}
