package org.helioviewer.jhv.plugins.swek;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;

import org.helioviewer.jhv.event.RelatedEvents;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.gui.AwtInputAdapter;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.input.PointerEvent;

class SWEKPopupController implements SWEKLayer.CanvasUi {

    private static final Cursor helpCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final int xOffset = 12;
    private static final int yOffset = 12;

    private final SWEKDialogs dialogs;
    private Cursor lastCursor;

    SWEKPopupController(SWEKDialogs _dialogs) {
        dialogs = _dialogs;
    }

    private static Component component() {
        return MainFrame.getRenderComponent();
    }

    private Point calcWindowPosition(Component component, Point p, int hekWidth, int hekHeight) {
        int compWidth = component.getWidth();
        int compHeight = component.getHeight();
        Point compLoc = component.getLocationOnScreen();
        int compLocX = compLoc.x;
        int compLocY = compLoc.y;

        boolean yCoordInMiddle = false;
        int yCoord;
        if (p.y + hekHeight + yOffset < compHeight) {
            yCoord = p.y + compLocY + yOffset;
        } else {
            yCoord = p.y + compLocY - hekHeight - yOffset;
            if (yCoord < compLocY) {
                yCoord = compLocY + compHeight - hekHeight;
                if (yCoord < compLocY) {
                    yCoord = compLocY;
                }
                yCoordInMiddle = true;
            }
        }

        int xCoord;
        if (p.x + hekWidth + xOffset < compWidth) {
            xCoord = p.x + compLocX + xOffset;
        } else {
            xCoord = p.x + compLocX - hekWidth - xOffset;
            if (xCoord < compLocX && !yCoordInMiddle) {
                xCoord = compLocX + compWidth - hekWidth;
            }
        }

        return new Point(xCoord, yCoord);
    }

    @Override
    public void open(RelatedEvents related, SolarEvent event, PointerEvent e) {
        SWEKEventInformationDialog hekPopUp = dialogs.createInformation(related, event);
        if (hekPopUp == null) return;
        Component canvas = component();
        hekPopUp.pack();
        hekPopUp.setLocation(calcWindowPosition(canvas, AwtInputAdapter.toAwtPoint(e), hekPopUp.getWidth(), hekPopUp.getHeight()));
        hekPopUp.setVisible(true);

        canvas.setCursor(helpCursor);
    }

    @Override
    public void setHand(boolean hand) {
        Component canvas = component();
        Cursor cursor = canvas.getCursor();
        if (helpCursor != cursor)
            lastCursor = cursor;
        canvas.setCursor(hand ? helpCursor : lastCursor != null ? lastCursor : Cursor.getDefaultCursor());
    }

}
