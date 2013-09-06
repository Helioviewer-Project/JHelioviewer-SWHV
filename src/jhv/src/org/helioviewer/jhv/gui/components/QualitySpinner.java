package org.helioviewer.jhv.gui.components;

import java.util.LinkedList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.event.ChangeListener;

import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JP2View;
import org.helioviewer.viewmodelplugin.filter.FilterAlignmentDetails;

/**
 * A class that is used to display a slider that updates the quality layers used
 * for an image.
 */
public class QualitySpinner extends JPanel implements ChangeListener, ViewListener, FilterAlignmentDetails {

    private static final long serialVersionUID = 1L;
    private JSpinner qualitySpinner;
    private JP2View jp2View;

    /** The public constructor that sets up the panel containing the slider */
    public QualitySpinner(JP2View view) {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        jp2View = view;
        // title.setPreferredSize(new Dimension(AbstractFilterPanel.titleWidth,
        // AbstractFilterPanel.height));
        // add(title);
        qualitySpinner = new JSpinner();
        qualitySpinner.addChangeListener(this);
        updateModel();
        add(qualitySpinner);
        WheelSupport.installMouseWheelSupport(qualitySpinner);
        if (jp2View != null) {
            jp2View.addViewListener(this);
        } else {
            qualitySpinner.setEnabled(false);
        }
    }

    /** A method to restore the default setting */
    public void setToDefault() {
        if (jp2View != null) {
            qualitySpinner.setValue(jp2View.getMaximumNumQualityLayers());
        }
    }

    public void stateChanged(javax.swing.event.ChangeEvent e) {
        if (jp2View != null) {
            int quality = ((QualityLevel) ((SpinnerListModel) qualitySpinner.getModel()).getValue()).getLevel();
            jp2View.setNumQualityLayers(quality);
        }
    }

    public void viewChanged(View sender, org.helioviewer.viewmodel.changeevent.ChangeEvent aEvent) {
        int level = jp2View.getCurrentNumQualityLayers();

        updateModel();
        qualitySpinner.getModel().setValue(getLevelObject(level));
    }

    private Object getLevelObject(int level) {
        level--;

        SpinnerListModel slm = (SpinnerListModel) qualitySpinner.getModel();
        List<?> list = slm.getList();

        if (level < list.size()) {
            return list.get(level);
        }

        return null;
    }

    private SpinnerListModel generateModel(int max) {
        LinkedList<QualityLevel> entries = new LinkedList<QualityLevel>();

        if (jp2View != null) {
            for (int i = 1; i <= jp2View.getMaximumNumQualityLayers(); i++) {
                entries.add(new QualityLevel(i, jp2View.getMaximumNumQualityLayers()));
            }
        } else {
            entries.add(new QualityLevel(1, 1));
        }

        return new SpinnerListModel(entries);
    }

    public void updateModel() {

        int max = 1;
        if (jp2View != null) {
            max = jp2View.getMaximumNumQualityLayers();
        }

        if (qualitySpinner.getModel() == null || !(qualitySpinner.getModel() instanceof SpinnerListModel)) {
            qualitySpinner.setModel(generateModel(max));
        } else {
            SpinnerListModel slm = (SpinnerListModel) qualitySpinner.getModel();
            if (slm.getList().size() != max) {
                qualitySpinner.setModel(generateModel(max));
            }
        }

    }

    class QualityLevel {

        int level = 1;
        int max = 1;

        public QualityLevel(int level, int max) {
            this.level = level;
            this.max = max;
        }

        public String toString() {
            return level + "/" + max;
        }

        public int getLevel() {
            return this.level;
        }

    }

    public int getDetails() {
        return FilterAlignmentDetails.POSITION_QUALITY;
    }

    /**
     * Override the setEnabled method in order to keep the containing
     * components' enabledState synced with the enabledState of this component.
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.qualitySpinner.setEnabled(enabled);
    }
}