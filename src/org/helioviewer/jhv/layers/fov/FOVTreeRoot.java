package org.helioviewer.jhv.layers.fov;

import javax.swing.tree.DefaultMutableTreeNode;

import org.helioviewer.jhv.layers.MovieDisplay;

@SuppressWarnings("serial")
class FOVTreeRoot extends DefaultMutableTreeNode {

    private final String name;

    FOVTreeRoot(String _name) {
        name = _name;
    }

    private static double control2Center(double v) { // v in arcmin
        return Math.tan(v * (Math.PI / 180. / 60.));
    }

    void setCenterX(double controlX) {
        double centerX = control2Center(controlX);
        children().asIterator().forEachRemaining(c -> ((FOVTreeElement) c).setCenterX(centerX));
        MovieDisplay.display();
    }

    void setCenterY(double controlY) {
        double centerY = control2Center(controlY);
        children().asIterator().forEachRemaining(c -> ((FOVTreeElement) c).setCenterY(centerY));
        MovieDisplay.display();
    }

    @Override
    public String toString() {
        return name;
    }

}
