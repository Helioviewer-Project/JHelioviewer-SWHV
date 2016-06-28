package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.input.KeyShortcuts;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.view.View;

@SuppressWarnings("serial")
public class NewLayerAction extends AbstractAction {

    public NewLayerAction(boolean small, boolean useIcon) {
        super("New Layer...", useIcon ? IconBank.getIcon(JHVIcon.ADD) : null);
        putValue(SHORT_DESCRIPTION, "Add new layer");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true);
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Check the dates if possible
        View view = Layers.getActiveView();
        if (view != null && view.isMultiFrame()) {
            JHVDate start = view.getFirstTime();
            JHVDate end = view.getLastTime();

            long obsStartDate = ObservationDialog.getInstance().getObservationImagePane().getStartTime();
            long obsEndDate = ObservationDialog.getInstance().getObservationImagePane().getEndTime();
            // only updates if it's really necessary with a tolerance of an hour
            int tolerance = 60 * 60 * 1000;
            if (Math.abs(start.milli - obsStartDate) > tolerance || Math.abs(end.milli - obsEndDate) > tolerance) {
                if (!ObservationDialogDateModel.getInstance().isStartTimeSetByUser()) {
                    ObservationDialogDateModel.getInstance().setStartTime(start.milli, false);
                }
                if (!ObservationDialogDateModel.getInstance().isEndTimeSetByUser()) {
                    ObservationDialogDateModel.getInstance().setEndTime(end.milli, false);
                }
            }
        }
        ObservationDialog.getInstance().showDialog(null, "Image data");
    }

}
