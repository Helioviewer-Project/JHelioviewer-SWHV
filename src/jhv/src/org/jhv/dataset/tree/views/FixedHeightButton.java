package org.jhv.dataset.tree.views;

import java.awt.Dimension;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

public class FixedHeightButton extends JButton {
    public FixedHeightButton() {
        super();
        this.setPreferredSize(new Dimension(15, -1));
    }

    public FixedHeightButton(String title) {
        super(title);
        this.setPreferredSize(new Dimension(50, 15));
    }

    public FixedHeightButton(Action arg0) {
        super(arg0);
        this.setPreferredSize(new Dimension(15, -1));
    }

    public FixedHeightButton(Icon arg0) {
        super(arg0);
        this.setPreferredSize(new Dimension(15, -1));
    }

    public FixedHeightButton(String arg0, Icon arg1) {
        super(arg0, arg1);
        this.setPreferredSize(new Dimension(15, -1));
    }
}
