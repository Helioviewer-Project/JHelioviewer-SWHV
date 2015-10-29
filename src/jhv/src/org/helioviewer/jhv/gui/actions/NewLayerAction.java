package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.ParseException;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.view.View;

@SuppressWarnings("serial")
public class NewLayerAction extends AbstractAction {

    public NewLayerAction(boolean small, boolean useIcon) {
        super("New Layer...", useIcon ? IconBank.getIcon(JHVIcon.ADD) : null);
        putValue(SHORT_DESCRIPTION, "Add new layer");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Check the dates if possible
        View activeView = Layers.getActiveView();
        if (activeView != null && activeView.isMultiFrame()) {
            JHVDate start = Layers.getStartDate(activeView);
            JHVDate end = Layers.getEndDate(activeView);
            try {
                Date obsStartDate = TimeUtils.apiDateFormat.parse(ObservationDialog.getInstance().getObservationImagePane().getStartTime());
                Date obsEndDate = TimeUtils.apiDateFormat.parse(ObservationDialog.getInstance().getObservationImagePane().getEndTime());
                // only updates if it's really necessary with a tolerance of an hour
                final int tolerance = 60 * 60 * 1000;
                if (Math.abs(start.getTime() - obsStartDate.getTime()) > tolerance || Math.abs(end.getTime() - obsEndDate.getTime()) > tolerance) {
                    if (ObservationDialogDateModel.getInstance().getStartDate() == null || !ObservationDialogDateModel.getInstance().isStartDateSetByUser()) {
                        ObservationDialogDateModel.getInstance().setStartDate(start.getDate(), false);
                    }
                    if (ObservationDialogDateModel.getInstance().getEndDate() == null || !ObservationDialogDateModel.getInstance().isEndDateSetByUser()) {
                        ObservationDialogDateModel.getInstance().setEndDate(end.getDate(), false);
                    }
                }
            } catch (ParseException ex) {
                // Should not happen
                Log.error("Cannot update observation dialog", ex);
            }
        }
        // Show dialog
        ObservationDialog.getInstance().showDialog();
    }

}
