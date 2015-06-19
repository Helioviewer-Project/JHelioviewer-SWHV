package org.helioviewer.viewmodel.view.jp2view;

import org.helioviewer.base.datetime.ImmutableDateTime;

public interface JHVJP2ViewDataHandler {

    public abstract void handleData(JHVJP2View view, ImmutableDateTime dateTime);

}
