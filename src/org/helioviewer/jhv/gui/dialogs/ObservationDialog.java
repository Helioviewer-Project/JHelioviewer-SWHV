package org.helioviewer.jhv.gui.dialogs;

import java.awt.event.ActionEvent;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.components.CadencePanel;
import org.helioviewer.jhv.gui.components.ImageSelectorPanel;
import org.helioviewer.jhv.gui.components.timeselector.TimeSelectorPanel;
import org.helioviewer.jhv.gui.interfaces.ObservationSelector;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.layers.ImageLayer;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class ObservationDialog extends StandardDialog implements ObservationSelector {

    private final AbstractAction load = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            load("xxx", -1); // selection may not be good
        }
    };
    private final JButton okBtn = new JButton(load);
    private final JButton availabilityBtn = new JButton("Available data");

    private final TimeSelectorPanel timeSelectorPanel = new TimeSelectorPanel();
    private final CadencePanel cadencePanel = new CadencePanel();
    private final ImageSelectorPanel imageSelectorPanel;
    private ImageLayer layer;

    private static ObservationDialog instance;

    public static ObservationDialog getInstance() {
        if (instance == null) {
            instance = new ObservationDialog(JHVFrame.getFrame());
        }
        return instance;
    }

    private ObservationDialog(JFrame mainFrame) {
        super(mainFrame, true);
        setResizable(false);

        imageSelectorPanel = new ImageSelectorPanel(this);
        availabilityBtn.addActionListener(e -> JHVGlobals.openURL(imageSelectorPanel.getAvailabilityURL()));
        setInitFocusedComponent(imageSelectorPanel.getFocused());
    }

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                layer = null;
                setVisible(false);
            }
        };
        setDefaultCancelAction(close);

        JButton cancelBtn = new JButton(close);
        cancelBtn.setText("Cancel");

        ButtonPanel panel = new ButtonPanel();
        panel.add(okBtn, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(cancelBtn, ButtonPanel.CANCEL_BUTTON);
        panel.add(availabilityBtn, ButtonPanel.OTHER_BUTTON);

        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.add(timeSelectorPanel);
        content.add(cadencePanel);
        content.add(imageSelectorPanel);
        content.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        return content;
    }

    @Nullable
    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    public void showDialog(boolean newLayer, ImageLayer _layer) {
        layer = _layer;

        APIRequest req;
        if (layer != null && (req = layer.getAPIRequest()) != null) {
            imageSelectorPanel.setupLayer(req);
            timeSelectorPanel.setTime(req.startTime, req.endTime);
            cadencePanel.setCadence(req.cadence);
        }

        if (newLayer) {
            setTitle("New Layer");
            okBtn.setText("Add");
        } else {
            setTitle("Change Layer");
            okBtn.setText("Change");
        }

        pack();
        setLocationRelativeTo(JHVFrame.getFrame());
        setVisible(true);
    }

    @Override
    public int getCadence() {
        return cadencePanel.getCadence();
    }

    @Override
    public void setTime(long start, long end) {
        timeSelectorPanel.setTime(start, end);
    }

    @Override
    public long getStartTime() {
        return timeSelectorPanel.getStartTime();
    }

    @Override
    public long getEndTime() {
        return timeSelectorPanel.getEndTime();
    }

    @Override
    public void load(String server, int sourceId) {
        long start = getStartTime();
        long end = getEndTime();
        if (start > end) {
            setTime(end, end);
            JOptionPane.showMessageDialog(null, "End date is before start date", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        imageSelectorPanel.load(layer, getStartTime(), getEndTime(), getCadence());
        layer = null;
        setVisible(false);
    }

    @Override
    public void setAvailabilityEnabled(boolean enabled) {
        availabilityBtn.setEnabled(enabled);
    }

}
