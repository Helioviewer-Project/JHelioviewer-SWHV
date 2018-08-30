package org.helioviewer.jhv.layers.spaceobject;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.components.base.JSeparatorComboBox;
import org.helioviewer.jhv.position.LoadPosition;
import org.helioviewer.jhv.position.StatusReceiver;

import org.json.JSONArray;

@SuppressWarnings("serial")
public class SpaceObjectComboBox extends JPanel implements ActionListener, StatusReceiver {

    private final JLabel label = new JLabel("", JLabel.RIGHT);
    private final JSeparatorComboBox comboBox;

    private final UpdateViewpoint uv;
    private final SpaceObject observer;
    private final Frame frame;

    private long startTime;
    private long endTime;

    private LoadPosition load;

    public SpaceObjectComboBox(JSONArray ja, boolean activate, UpdateViewpoint _uv, SpaceObject _observer, Frame _frame, long _startTime, long _endTime) {
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

        uv = _uv;
        observer = _observer;
        frame = _frame;
        startTime = _startTime;
        endTime = _endTime;

        comboBox = new JSeparatorComboBox(SpaceObject.getTargetsSeparated(observer).toArray());
        comboBox.addActionListener(this);

        if (activate)
            comboBox.setSelectedItem(SpaceObject.get(ja.optString(0, "Earth")));

        add(comboBox);
        add(label);
    }

    public void setTime(long _startTime, long _endTime) {
        if (startTime == _startTime && endTime == _endTime)
            return;

        startTime = _startTime;
        endTime = _endTime;
        actionPerformed(null);
    }

    public boolean isDownloading() {
        return load != null && load.isDownloading();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SpaceObject target = (SpaceObject) comboBox.getSelectedItem();
        load = LoadPosition.execute(this, observer, target, frame, startTime, endTime);
        uv.setObserver(load);
        // System.out.println(">>> " + load);
    }

    @Override
    public void setStatus(String status) {
        label.setText(status);
        label.setToolTipText(status);
        Display.getCamera().refresh();
    }

}
