package org.helioviewer.plugins.eveplugin.radio.data;

import org.helioviewer.viewmodel.changeevent.ChangedReason;
import org.helioviewer.viewmodel.view.View;

public class RadioRequestReason implements ChangedReason {
    private View view;
    private Long id;

    public RadioRequestReason(View view, Long id) {
        this.id = id;
        this.view = view;
    }

    public View getView() {
        return view;
    }

    public Long getID() {
        return this.id;
    }
}
