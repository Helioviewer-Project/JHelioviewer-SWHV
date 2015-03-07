package org.helioviewer.viewmodel.changeevent;

import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

public class ReaderErrorReason implements ChangedReason {

    private JHVJP2View view;
    private Throwable exception;

    public ReaderErrorReason(JHVJP2View view, Throwable exception) {
        this.view = view;
        this.exception = exception;
    }

    public View getView() {
        return view;
    }

    public JHVJP2View getJHVJP2View() {
        return view;
    }

    public Throwable getException() {
        return exception;
    }

}
