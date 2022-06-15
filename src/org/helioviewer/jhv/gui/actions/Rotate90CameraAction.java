package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;

@SuppressWarnings("serial")
public class Rotate90CameraAction extends AbstractAction {

    private final Quat rotation;

    public Rotate90CameraAction(String name, Vec3 axis) {
        super(name);
        rotation = Quat.createRotation(Math.PI / 2, axis);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Display.getCamera().rotateDragRotation(rotation);
        MovieDisplay.display();
    }

}
