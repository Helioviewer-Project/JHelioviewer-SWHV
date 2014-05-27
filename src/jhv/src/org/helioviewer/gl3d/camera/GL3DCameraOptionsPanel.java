package org.helioviewer.gl3d.camera;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.gl3d.scenegraph.GL3DState;

public class GL3DCameraOptionsPanel extends JPanel implements ChangeListener {
    private static final long serialVersionUID = 3942154069677445408L;
    private final JSpinner timedelaySpinner;
    private long timeDelay;

    public GL3DCameraOptionsPanel() {
        add(new JLabel("Set time delay"));
        timedelaySpinner = new JSpinner();
        timedelaySpinner.setModel(new SpinnerNumberModel(new Float(0.2f), new Float(-5000), new Float(5000), new Float(0.01f)));
        timedelaySpinner.addChangeListener(this);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(timedelaySpinner);
        timedelaySpinner.setEditor(editor);
        this.add(timedelaySpinner);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        GL3DState state = GL3DState.get();
        this.timeDelay = (long) ((SpinnerNumberModel) timedelaySpinner.getModel()).getNumber().floatValue() * 60 * 60 * 24 * 1000;
        state.getActiveCamera().setTimeDelay(this.timeDelay);
    }

}
